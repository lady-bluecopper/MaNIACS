package anonymous.maniac.lattice;

import anonymous.maniac.domain.Orbit;
import anonymous.maniac.domain.OrbitSerializer;
import anonymous.maniac.domain.Pair;
import anonymous.maniac.utils.CLParser;
import anonymous.maniac.utils.Settings;
import anonymous.maniac.utils.StopWatch;
import fi.tkk.ics.jbliss.pattern.JBlissPattern;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import fi.tkk.ics.jbliss.pattern.PatternSerializer;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author giulia
 */
public class LatticeGeneration {
    
    public static void main(String[] args) throws IOException, ParseException {
        CLParser.parse(args);
        generateLattice(Settings.dataFolder + "lattices/lattice_K" + Settings.patternSize + "L" + Settings.numLabels,
                    Settings.dataFolder + "lattices/patterns_K" + Settings.patternSize + "L" + Settings.numLabels);
    }
    
    public static void generateLattice(String orbitFile, String patternFile) throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();
        LatticeTree lattice = new LatticeTree();
        Map<String, List<JBlissPattern>> pList = new HashMap<>();
        Output out = new Output(new FileOutputStream(orbitFile));
        Kryo kryo = kryoLatticeWriter();

        List<List<Orbit>> subtree = lattice.generateSubTree(0, Settings.patternSize, Settings.numLabels, pList);
        setChildren(subtree);
        
        List<Integer> subtreeSizes = subtree.stream().map(l -> l.size()).collect(Collectors.toList());
        int size = subtreeSizes.stream().mapToInt(i -> i).sum();
        System.out.println("First Subtree Generated with Nodes " + size);
        writeSubTree(kryo, out, subtree, size);
        for (int i = 1; i < Settings.numLabels; i++) {
            List<List<Orbit>> cloned = lattice.cloneSubTree(subtree, i, pList);
            setChildren(cloned);
            System.out.println("Subtree for Label " + i + " Generated.");
            writeSubTree(kryo, out, cloned, size);
            out.flush();
            cloned = null;
        }
        out.close();
        System.out.println("Lattice generated in ms " + watch.getElapsedTime());
        watch.start();
        Kryo kryoP = kryoPatternWriter();
        writePatterns(kryoP, patternFile, pList.values().stream().flatMap(l -> l.stream()).collect(Collectors.toList()));
        System.out.println("Patterns written in ms " + watch.getElapsedTime());
    }
    
    private static void setChildren(List<List<Orbit>> subtree) {
        Map<Integer, Pair<Integer, Integer>> nodeMap = new HashMap<>();
        for (int i = 0; i < subtree.size(); i++) {
            for (int j = 0; j < subtree.get(i).size(); j++) {
                nodeMap.put(subtree.get(i).get(j).getId(), new Pair<>(i, j));
            }
        }
        for (int i = 0; i < subtree.size(); i++) {
            for (int j = 0; j < subtree.get(i).size(); j++) {
                Orbit b = subtree.get(i).get(j);
                for (int parent : b.getParents()) {
                    if (parent != 0) {
                        Pair<Integer, Integer> pos = nodeMap.get(parent);
                        Orbit porb = subtree.get(pos.getA()).get(pos.getB());
                        porb.addChild(b.getId());
                    }
                }
            }
        }
    }
    
    private static Kryo kryoLatticeWriter() {
        Kryo kryo = new Kryo();
        kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.HashSet.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(anonymous.maniac.domain.Node.class);
        OrbitSerializer ser = new OrbitSerializer();
        kryo.register(Orbit.class, ser);
        return kryo;
    }
    
    private static Kryo kryoPatternWriter() {
        Kryo kryo = new Kryo();
        kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.HashSet.class);
        kryo.register(anonymous.maniac.domain.Node.class);
        kryo.register(anonymous.maniac.domain.Edge.class);
        PatternSerializer ser = new PatternSerializer();
        kryo.register(JBlissPattern.class, ser);
        return kryo;
    }
    
    private static void writeSubTree(Kryo kryo, Output latticeFile, List<List<Orbit>> subtree, int size) throws IOException {
        latticeFile.writeInt(size);
        for (int i =0; i < subtree.size(); i ++) {
            for (int j = 0; j < subtree.get(i).size(); j++) {
                kryo.writeObject(latticeFile, subtree.get(i).get(j));
            }
        }
    }
    
    private static void writePatterns(Kryo kryo, String patternFile, Collection<JBlissPattern> patterns) throws FileNotFoundException, IOException {
        Output out = new Output(new FileOutputStream(patternFile));
        out.writeInt(patterns.size());
        patterns.stream().forEach(p -> kryo.writeObject(out, p));
        out.close();
    }
    
}
