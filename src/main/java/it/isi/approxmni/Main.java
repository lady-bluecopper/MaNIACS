package it.isi.approxmni;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.isi.approxmni.lattice.LayerExploration;
import fi.tkk.ics.jbliss.pattern.JBlissPattern;
import it.isi.approxmni.domain.Edge;
import it.isi.approxmni.graph.LabeledGraph;
import it.isi.approxmni.lattice.LatticeTree;
import it.isi.approxmni.domain.Node;
import it.isi.approxmni.domain.Orbit;
import it.isi.approxmni.domain.Pair;
import it.isi.approxmni.utils.CLParser;
import it.isi.approxmni.utils.Settings;
import it.isi.approxmni.utils.StopWatch;
import it.isi.approxmni.vc.EmpiricalVC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    private static StopWatch watch;

    public static void main(String[] args) throws IOException, ParseException {
        CLParser.parse(args);
        LabeledGraph graph = loadGraph(Settings.dataFolder + Settings.inputFile);
        List<Pair<Double, List<JBlissPattern>>> results = executeAlgorithm(graph);
        writeResults(results);
    }

    public static LabeledGraph loadGraph(String path) throws IOException, ParseException {
        final BufferedReader rows = new BufferedReader(new FileReader(path));
        List<Node> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        String line;
        int counter = 0;

        while ((line = rows.readLine()) != null) {
            if (line.startsWith("v")) {
                final String[] parts = line.split("\\s+");
                final int index = Integer.parseInt(parts[1]);
                final int label = Integer.parseInt(parts[2]);
                if (index != counter) {
                    throw new ParseException("The node list is not sorted", counter);
                }
                nodes.add(new Node(index, label));
                counter++;
            } else if (line.startsWith("e")) {
                final String[] parts = line.split("\\s+");
                final int index1 = Integer.parseInt(parts[1]);
                final int index2 = Integer.parseInt(parts[2]);
                edges.add(new Edge(index1, index2));
            }
        }
        rows.close();
        LabeledGraph g = new LabeledGraph(nodes, edges);
        return g;
    }

    public static List<Pair<Double, List<JBlissPattern>>> executeAlgorithm(LabeledGraph graph) throws IOException {
        watch = new StopWatch();
        watch.start();
        Map<Integer, Integer> sample = getSample(graph);
        Map<Integer, Integer> inverseMap = inverseMapping(sample);
        double epsilon = EmpiricalVC.getEpsilon(1, sample.size());
        Map<Integer, Double> nodeLabelFrequencies = graph.getNodeLabelFrequencies(sample.keySet(), epsilon);
        if (nodeLabelFrequencies.isEmpty()) {
            return new ArrayList<>();
        }
        LatticeTree lattice = new LatticeTree();
        lattice.initializeLattice(nodeLabelFrequencies);
        HashMap<Integer, BitSet> orbitBitSetMap = initializeBitSetMap(graph, lattice, sample, nodeLabelFrequencies);
//        System.out.println("Created Orbits: " + orbitBitSetMap.size() + "; TIME ELAPSED: " + watch.getElapsedTime());
        List<Pair<Double, List<JBlissPattern>>> fps = runMiner(graph, lattice, orbitBitSetMap, nodeLabelFrequencies, inverseMap);
        watch.stop();
        System.out.println(watch.getElapsedTimeSecs());
        return fps;
    }

    private static HashMap<Integer, BitSet> initializeBitSetMap(LabeledGraph graph, LatticeTree lattice, Map<Integer, Integer> sample, Map<Integer, Double> labelFreqs) {
        HashMap<Integer, BitSet> orbitBitSetMap = new HashMap<>();
        for (Orbit o : lattice.getLayer(0)) {
            if (labelFreqs.containsKey(o.getMarkedNodeLabel())) {
                BitSet bitSet = new BitSet(Settings.sampleSize);
                for (Entry<Integer, Integer> e : sample.entrySet()) {
                    if (graph.getNodeLabel(e.getKey()) == o.getMarkedNodeLabel()) {
                        bitSet.set(e.getValue());
                    }
                }
                orbitBitSetMap.put(o.getId(), bitSet);
                lattice.setFrequency(o.getPattern(), labelFreqs.get(o.getMarkedNodeLabel()));
            } else {
                lattice.killPatternAndOrbits(o.getPattern());
            }
        }
        return orbitBitSetMap;
    }

    // -----------------    
    // VC DIMENSION
    // -----------------
     
    private static List<Pair<Double, List<JBlissPattern>>> runMiner(LabeledGraph graph, LatticeTree lattice, HashMap<Integer, BitSet> T, Map<Integer, Double> labelFreqs, Map<Integer, Integer> inverseMap) {
        List<Pair<Double, List<JBlissPattern>>> output = new ArrayList<>();
        double epsilon = 0.;
        BitSet mask = new BitSet(Settings.sampleSize);
        List<Integer> sample = IntStream.range(0, Settings.sampleSize).boxed().collect(Collectors.toList());

        if (!Settings.isExact) {
            epsilon = EmpiricalVC.getEpsilonLayer(lattice.getLayer(0), T, false, sample.size());
            Pair<Double, List<JBlissPattern>> p = updateLattice(lattice, 0, T, sample.size());
            output.add(p);
        } else {
            output.add(new Pair<>(0.,lattice.getFrequentPatterns(Settings.frequency, 1)));
        }
        int layer = 1;
        while (layer < Settings.patternSize && lattice.isLastLayerNotEmpty(layer - 1)) {
            if (!Settings.isExact) {
                int newSampleSize = (int) (sample.size() * Settings.alpha); 
//                System.out.println("S\t" + newSampleSize);
                Pair<List<Integer>,BitSet> resampled = resample(sample, Settings.sampleSize, newSampleSize);
                sample = resampled.getA();
                mask = resampled.getB();
                for (BitSet b : T.values()) {
                    b.and(mask);
                }
            }
            Pair<Double, List<JBlissPattern>> p = runLayer(graph, lattice, T, labelFreqs, inverseMap, layer, epsilon, sample.size());
            epsilon = p.getA();
            output.add(p);
//            System.out.println("E\t" + p.getA() + "\t" + layer);
            layer += 1;
        }
        return output;
    }
        
    private static Pair<Double, List<JBlissPattern>> runLayer(LabeledGraph graph, LatticeTree lattice,
            HashMap<Integer, BitSet> T,
            Map<Integer, Double> labelFreqs,
            Map<Integer, Integer> inverseMap,
            int layer,
            double epsilon,
            int sampleSize) {

        double epsilonPlus = lattice.newLayer(labelFreqs, T, layer, Settings.frequency - epsilon, sampleSize);
//            System.out.println("Generated Layer: " + layer + "; TIME ELAPSED: " + watch.getElapsedTime());
        LayerExploration.exploreLayer(graph, lattice, layer, T, inverseMap, epsilonPlus, sampleSize);
//        System.out.println("Explored Layer: " + layer + "; TIME ELAPSED: " + watch.getElapsedTime());
        if (Settings.isExact) {
            return new Pair<>(0., lattice.getFrequentPatterns(Settings.frequency, layer + 1));
        }
        Pair<Double, List<JBlissPattern>> p = updateLattice(lattice, layer, T, sampleSize);
//        System.out.println("Epsilon:= " + p.getA() + " TIME ELAPSED: " + watch.getElapsedTime());
        if (Settings.frequency - p.getA() < 0) {
            System.out.println("Negative Frequency Threshold!!!");
            System.exit(0);
        }
        return p;
    }
    
    private static Pair<Double, List<JBlissPattern>> updateLattice(LatticeTree lattice, int layer, 
            HashMap<Integer, BitSet> T, int sampleSize) {
        double epsilon = EmpiricalVC.getEpsilonLayer(lattice.getLayer(layer), T, false, sampleSize);
        System.out.println("E\t" + epsilon + "\t" + layer);
        boolean changed = lattice.pruneLayer(Settings.frequency - epsilon, layer);
        while (changed) {
            epsilon = EmpiricalVC.getEpsilonLayer(lattice.getLayer(layer), T, false, sampleSize);
            changed = lattice.pruneLayer(Settings.frequency - epsilon, layer);
        }
        return new Pair<>(epsilon, lattice.getFrequentPatterns(Settings.frequency - epsilon, layer + 1));
    }
    
    private static Map<Integer, Integer> getSample(LabeledGraph graph) {
        Set<Integer> sample = new HashSet<>();
        if (Settings.isExact) {
            sample = graph.getNodeIDs();
            Settings.sampleSize = sample.size();
        } else {
            Random rand = new Random(Settings.seed);
            if (Settings.percent) {
                Settings.sampleSize = graph.numberOfNodes() * Settings.sampleSize / 100;
            }
            while (sample.size() < Settings.sampleSize) {
                sample.add(rand.nextInt(graph.numberOfNodes()));
            }
        }
        Map<Integer, Integer> bitMap = new HashMap<>();
        for (int s : sample) {
            bitMap.put(s, bitMap.size());
        }
        return bitMap;
    }
    
    private static Pair<List<Integer>, BitSet> resample(List<Integer> sample, int sampleSize, int newSampleSize) {
        BitSet bitset = new BitSet(sampleSize);
        Set<Integer> newSample = Sets.newHashSet();
        Random rand = new Random(Settings.seed);
        
        while (newSample.size() < newSampleSize) {
            newSample.add(sample.get(rand.nextInt(sample.size())));
        }
        for (int i : newSample) {
            bitset.set(i);
        }
        return new Pair<>(Lists.newArrayList(newSample), bitset);
    }

    private static Map<Integer, Integer> inverseMapping(Map<Integer, Integer> m) {
        Map<Integer, Integer> inverse = new HashMap<>();
        m.entrySet().stream().forEach(e -> inverse.put(e.getValue(), e.getKey()));
        return inverse;
    }

    public static void writeResults(List<Pair<Double, List<JBlissPattern>>> results) throws IOException {
        int numFreqPatterns = results.stream().mapToInt(p -> p.getB().size()).sum();
        try {
            FileWriter fw = new FileWriter(Settings.outputFolder + "statistics.csv", true);
            fw.write(String.format("%s\t%s\t%f\t%d\t%f\t%d\t%s\t%s\n",
                    Settings.inputFile,
                    new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()),
                    watch.getElapsedTime() / 1000.0D,
                    numFreqPatterns,
                    Settings.frequency,
                    Settings.patternSize,
                    (!Settings.isExact) ? Settings.sampleSize : "EX",
                    (!Settings.isExact) ? Settings.seed : "0"));
            fw.close();

            String fName = "MANIAC_" + Settings.inputFile
                    + "_F" + Settings.frequency
                    + "P" + Settings.patternSize
                    + ((!Settings.isExact) ? "AX" + Settings.sampleSize : "EX")
                    + ((!Settings.isExact) ? "S" + Settings.seed : "0");
            FileWriter fwP = new FileWriter(Settings.outputFolder + fName);
            for (Pair<Double, List<JBlissPattern>> p : results) {
                if (!p.getB().isEmpty()) {
                    fwP.write(p.getA().toString() + "\n");
                    for (JBlissPattern pat : p.getB()) {
                        fwP.write(pat.toString() + "\n");
                    }
                }
            }
            fwP.close();
        } catch (IOException ex) {
        }
    }
}
