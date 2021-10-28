package it.isi.approxmni.lattice;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fi.tkk.ics.jbliss.pattern.JBlissPattern;
import it.isi.approxmni.domain.Edge;
import it.isi.approxmni.domain.Node;
import it.isi.approxmni.domain.Orbit;
import it.isi.approxmni.domain.Pair;
import it.isi.approxmni.domain.Triplet;
import it.isi.approxmni.utils.Settings;
import it.isi.approxmni.vc.EmpiricalVC;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class LatticeTree {

    private List<List<Orbit>> lattice; // lattice tree
    private HashMap<Integer, JBlissPattern> patterns; // patterns discovered so far
    private HashMap<Integer, Pair<Integer, Integer>> nodes; // (i,j) position of orbit node in the lattice
    private HashSet<String> frequentEdges;
    private int nextId; // ids for orbit nodes
    private int patternNextId; // ids for patterns

    public LatticeTree() {
        this.lattice = new ArrayList<>();
        this.patterns = new HashMap<>();
        this.nodes = new HashMap<>();
        this.frequentEdges = new HashSet<>();
        this.nextId = 1;
        this.patternNextId = 0;
    }

    // prune orbits that are not frequent 
    public boolean pruneLayer(double tau, int layer) {
        boolean isChanged = false;
        for (Orbit o : lattice.get(layer)) {
            JBlissPattern p = patterns.get(o.getPattern());
            if (p.frequency() < tau) {
                if (killPatternAndOrbits(p.getId())) {
                    isChanged = true;
                }
                if (p.getNumberOfEdges() == 1) {
                    frequentEdges.remove(p.singleEdgeHashCode());
                }
            }
        }
        return isChanged;
    }

    // creates first layer of the lattice tree
    public void initializeLattice(Map<Integer, Double> labelFreqs) {
        List<Orbit> VNE = new ArrayList();
        for (int label : labelFreqs.keySet()) {
            Orbit o = new Orbit(nextId, new ArrayList(), new HashMap(), new LinkedList());
            o.addParent(0);
            o.addNode(0, label);
            JBlissPattern p = o.computeCanonicalForm(patternNextId);
            p.addOrbit(nextId);
            p.setFrequency(labelFreqs.get(label));
            patterns.put(p.getId(), p);
            o.setPattern(p.getId());
            VNE.add(o);
            nodes.put(o.getId(), new Pair(0, VNE.size() - 1));
            nextId += 1;
            patternNextId += 1;
        }
        lattice.add(VNE);
    }

    public Orbit createChild(int id, Orbit o, boolean sameLevel) {
        Orbit child = new Orbit(id, o.getNodes(), o.getAdj(), o.getGray());
        if (!o.getGray().isEmpty() || !o.isConnected() || sameLevel) {
            child.addParents(o.getParents());
        } else {
            child.addParent(o.getId());
        }
        return child;
    }
    
    // ONLINE LATTICE GENERATION

    public double newLayer(Map<Integer, Double> freqLabels, HashMap<Integer, BitSet> orbitBitSetMap, 
            int layer, double tau, int sampleSize) {
        Queue<Orbit> temporaryOrbits = new LinkedList<>();
        Map<Integer, Orbit> VNE = new HashMap<>();
        lattice.get(lattice.size() - 1).stream().forEach(o -> VNE.put(o.getId(), o));
        HashMap<String, List<Orbit>> nextLayer = new HashMap<>();
        HashMap<String, List<JBlissPattern>> nextPatterns = new HashMap<>();
        Map<String, List<Orbit>> lastLayer = generateParentHashCodes(VNE.values());
        // iterate over the orbits of the last layer
        for (Orbit a : VNE.values()) {
            if (!a.isDead()) {
                // creates a new orbit for each frequent label
                for (int label : freqLabels.keySet()) {
                    Orbit b = createChild(nextId, a, false);
                    int newVId = b.getNodes().size();
                    b.addNode(newVId, label);
                    nextId++;
                    // add a gray edge from the new node to all the others
                    for (int i = 0; i < a.getNodes().size(); i++) {
                        // add a gray edge only if it is frequent
                        if ((frequentEdges.contains(a.getNodeLabel(i) + "-" + label)
                                || frequentEdges.contains(label + "-" + a.getNodeLabel(i))) || b.getNodes().size() == 2) {
                            b.getGray().add(new Edge(a.getNodeID(i), newVId));
                        }
                    }
                    temporaryOrbits.add(b);
                }
            }
        }
        while (!temporaryOrbits.isEmpty()) {
            Orbit b = temporaryOrbits.poll();
            // remove one gray edge at a time
            while (!b.getGray().isEmpty()) {
                Edge e = b.getGray().poll();
                // this new orbit contains the edge e
                Orbit c = createChild(nextId, b, true);
                nextId += 1;
                c.addEdge(e.getSrc(), e.getDst());
                if (!b.getGray().isEmpty()) {
                    temporaryOrbits.add(b);
                } else if (b.isConnected() && !isRedundant(b, nextLayer, VNE, nextPatterns) 
                        && findParents(b, lastLayer) && canBeFrequent(b, orbitBitSetMap, tau, sampleSize)) {
                    String hashB = b.getMarkedNodeLabel() + "-" + b.getPattern();
                    List<Orbit> tmp = nextLayer.getOrDefault(hashB, new ArrayList<>());
                    tmp.add(b);
                    nextLayer.put(hashB, tmp);
                }
                if (!c.getGray().isEmpty()) {
                    temporaryOrbits.add(c);
                } else if (!isRedundant(c, nextLayer, VNE, nextPatterns) && findParents(c, lastLayer) 
                        && canBeFrequent(c, orbitBitSetMap, tau, sampleSize)) {
                    String hashC = c.getMarkedNodeLabel() + "-" + c.getPattern();
                    List<Orbit> tmp = nextLayer.getOrDefault(hashC, new ArrayList<>());
                    tmp.add(c);
                    nextLayer.put(hashC, tmp);
                }
            }
        }
        final double epsilon;
        List<Orbit> ubNextLayer = nextLayer.values().stream().flatMap(l -> l.stream()).collect(Collectors.toList());
        if (Settings.isExact) {
            epsilon = 0;
        } else {
            epsilon = EmpiricalVC.getEpsilonLayer(ubNextLayer, orbitBitSetMap, true, sampleSize);
        }
        List<Orbit> newLayer = ubNextLayer.stream()
                .filter(o -> {
                    JBlissPattern thisPattern = getPattern(o.getPattern());
                    return thisPattern.frequency() >= Settings.frequency - epsilon 
                        && thisPattern.getNumAutos() == thisPattern.getOrbits().size();
                }).collect(Collectors.toList());
        Collections.sort(newLayer,
                (Orbit o1, Orbit o2) -> Integer.compare(orbitBitSetMap.get(o1.getId()).cardinality(), orbitBitSetMap.get(o2.getId()).cardinality()));
        for (int i = 0; i < newLayer.size(); i++) {
            nodes.put(newLayer.get(i).getId(), new Pair(lattice.size(), i));
        }
        // add layer to the tree
        addLayer(newLayer);
        return epsilon;
    }    
    
    public boolean findParents(Orbit b, Map<String, List<Orbit>> lastLayer) {
        JBlissPattern p = getPattern(b.getPattern());
        Set<Set<Integer>> autos = p.getAutomorphisms().getDistinctEquivalences();
        int parentsFound = 1;
        int validAutos = 0;
        // search for all the parents
        for (Set<Integer> lo : autos) {
            if (!lo.contains(b.getCanId()) || lo.size() > 1) {
                Pair<List<Node>, HashMap<Integer, Set<Integer>>> pair = removeNodeFromSubOrbit(p, lo, b.getCanId());
                if (pair.getB().isEmpty() || pair.getA().size() != pair.getB().keySet().size() || !isConnected(pair)) {
                    continue;
                }
                validAutos += 1;
                Triplet<Integer, String, String> fp = computeFingerprint(pair);
                String hashFP = fp.getA() + "-" + b.getMarkedNodeLabel() + "-" + fp.getB();
                for (Orbit obp : lastLayer.getOrDefault(hashFP, new ArrayList<>())) {
                    if (satisfiesConstraints(b, obp, fp) && isEqualToOrbit(b.getCanId(), obp, pair)) {
                        parentsFound += 1;
                        b.addParent(obp.getId());
                        p.addParent(obp.getPattern());
                        // if a parent is dead, the child cannot be frequent
                        if (obp.isDead()) {
                            killPattern(b.getPattern());
                            return false;
                        }
                        break;
                    }
                }
            } 
        }
        return parentsFound >= validAutos;
    }
    
    // UTILITY
    
    public Map<String, List<Orbit>> generateParentHashCodes(Collection<Orbit> orbits) {
        Map<String, List<Orbit>> hashMapParents = new HashMap<>();
        for (Orbit o : orbits) {
            String fp = o.getNumEdges() + "-" + o.getMarkedNodeLabel() + "-" + o.getDegreeSequence();
            List<Orbit> tmp = hashMapParents.getOrDefault(fp, new ArrayList<>());
            tmp.add(o);
            hashMapParents.put(fp, tmp);
        }
        return hashMapParents;
    }

    public Triplet<Integer, String, String> computeFingerprint(Pair<List<Node>, HashMap<Integer, Set<Integer>>> pair) {
        int otherNumEdges = 0;
        for (Entry<Integer, Set<Integer>> e : pair.getB().entrySet()) {
            for (int n : e.getValue()) {
                if (e.getKey() < n) {
                    otherNumEdges += 1;
                }
            }
        }
        //  create degree sequence and label frequency sequence
        List<Integer> degrees = pair.getB().values().stream().map(s -> s.size()).collect(Collectors.toList());
        Collections.sort(degrees);
        String otherDegreeSequence = degrees.toString();
        HashMap<Integer, Integer> labelFreqs = new HashMap<>();
        pair.getA().forEach(n -> labelFreqs.put(n.getLabel(), labelFreqs.getOrDefault(n.getLabel(), 0) + 1));
        List<Map.Entry<Integer, Integer>> temp = new ArrayList<>(labelFreqs.entrySet());
        Collections.sort(temp, (Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) -> Integer.compare(o1.getKey(), o2.getKey()));
        String otherLabelFrequency = temp.stream().map(e -> e.getKey() + "=" + e.getValue()).reduce("", (String t, String u) -> t + "," + u);
        return new Triplet<>(otherNumEdges, otherDegreeSequence, otherLabelFrequency);
    }

    public Pair<List<Node>, HashMap<Integer, Set<Integer>>> removeNodeFromSubOrbit(JBlissPattern p, Set<Integer> suborbit, int candId) {
        List<Node> N = Lists.newArrayList(p.getVertices());
        Iterator<Integer> it = suborbit.iterator();
        int node = it.next();
        while (node == candId) {
            node = it.next();
        }
        for (int i = 0; i < p.getVertices().size(); i++) {
            if (p.getVertices().get(i).getIndex() == node) {
                N.remove(i);
            }
        }
        HashMap<Integer, Set<Integer>> E = Maps.newHashMap();
        p.getEdges().stream().forEach(entry -> {
            Set<Integer> tmp = E.getOrDefault(entry.getSrc(), Sets.newHashSet());
            tmp.add(entry.getDst());
            E.put(entry.getSrc(), tmp);
            Set<Integer> tmp2 = E.getOrDefault(entry.getDst(), Sets.newHashSet());
            tmp2.add(entry.getSrc());
            E.put(entry.getDst(), tmp2);
        });
        for (Integer n : E.get(node)) {
            Set<Integer> tmp3 = E.get(n);
            tmp3.remove(node);
            if (!tmp3.isEmpty()) {
                E.put(n, tmp3);
            } else {
                E.remove(n);
            }
        }
        E.remove(node);
        return new Pair<>(N, E);
    }
    
    // check if the patterns already exists before inserting it
    public JBlissPattern insertPattern(JBlissPattern p, Map<String, List<JBlissPattern>> pList, boolean insert) {
        String hashP = p.getLabeledhashCode();
        for (JBlissPattern p2 : pList.getOrDefault(hashP, new ArrayList<>())) {
            if (p2.equals(p)) {
                return p2;
            }
        }
        List<JBlissPattern> tmp = pList.getOrDefault(hashP, new ArrayList<>());
        tmp.add(p);
        pList.put(hashP, tmp);
        if (insert) {
            patterns.put(p.getId(), p);
        }
        return p;
    }
    
    // BOOLEAN METHODS

    // check if the orbit already exists
    public boolean isRedundant(Orbit o1, Map<String, List<Orbit>> vne, Map<Integer, Orbit> lastLayer, Map<String, List<JBlissPattern>> pList) {
        JBlissPattern tmp = o1.computeCanonicalForm(patternNextId);
        JBlissPattern p = insertPattern(tmp, pList, true);
        o1.setPattern(p.getId());
        if (p.getId() == patternNextId) {
            patternNextId += 1;
        }
        boolean redundant = false;
        String hashB = o1.getMarkedNodeLabel() + "-" + o1.getPattern();
        for (Orbit o2 : vne.getOrDefault(hashB, new ArrayList<>())) {
            // marked label in the same equivalence class
            if (p.getAutomorphisms().getEquivalences(o1.getCanId()).contains(o2.getCanId())) {
                o2.addParents(o1.getParents());
                redundant = true;
                break;
            }
        }
        o1.getParents().forEach(parent -> p.addParent(lastLayer.get(parent).getPattern()));
        return redundant;
    }
    
    public boolean satisfiesConstraints(Orbit b, Orbit parent, Triplet<Integer, String, String> fp) {
        return !b.getParents().contains(parent.getId()) && parent.getLabelFrequency().equals(fp.getC());
    }
    
    public boolean isEqualToOrbit(int canId, Orbit o, Pair<List<Node>, HashMap<Integer, Set<Integer>>> pair) {
        JBlissPattern other = new JBlissPattern();
        int m = other.initialize(pair.getA(), pair.getB(), canId);
        int f = other.turnCanonical(m);
        return patterns.get(o.getPattern()).equals(other) && other.getAutomorphismsOf(o.getCanId()).contains(f);
    }
    
    public boolean canBeFrequent(Orbit b, HashMap<Integer, BitSet> T, double tau, int sampleSize) {
        BitSet tmp2 = new BitSet(Settings.sampleSize);
        tmp2.set(0, Settings.sampleSize, true);
        boolean frequent = true;
        // candidate vertices for this orbit are those appearing in all the parents
        for (int parent : b.getParents()) {
            if (getNode(parent).isDead() || T.get(parent).cardinality() < tau * sampleSize) {
                frequent = false;
                killPattern(b.getPattern());
                break;
            }
            tmp2.and(T.get(parent));
        }
        JBlissPattern p = getPattern(b.getPattern());
        if (frequent && p.frequency() >= tau) {
            for (int patternParent : p.getParents()) {
                if (getPattern(patternParent).frequency() < tau) {
                    killPattern(p.getId());
                    return false;
                }
            }
            // this orbit can be frequent and so we add it to the layer
            for (int parent : b.getParents()) {
                Orbit porb = getNode(parent);
                porb.addChild(b.getId());
            }
            addOrbit(b.getPattern(), b.getId());
            T.put(b.getId(), tmp2);
            return true;
        }
        killPattern(b.getPattern());
        return false;
    }
    
    public boolean isLastLayerNotEmpty(int layer) {
        return layer < 0 || 
                (!lattice.get(layer).isEmpty() && lattice.get(layer).stream().filter(o -> !o.isDead()).count() > 0);
    }
    
    public boolean isConnected(Pair<List<Node>, HashMap<Integer, Set<Integer>>> cands) {
        int v = cands.getA().get(0).getIndex();
        Set<Integer> visited = new HashSet<>();
        visit(v, visited, cands.getB());
        return visited.size() == cands.getA().size();
    }
    
    private void visit(int n, Set<Integer> visited, HashMap<Integer, Set<Integer>> adj) {
        visited.add(n);
        for (int u : adj.get(n)) {
            if (!visited.contains(u)) {
                visit(u, visited, adj);
            }
        }
    }

    // GET/SET/ADD METHODS
    
    public void addOrbit(int patternId, int orbitId) {
        JBlissPattern p = patterns.get(patternId);
        p.addOrbit(orbitId);
    }

    public void addNode(int id, Pair<Integer, Integer> pos) {
        nodes.put(id, pos);
    }
    
    public void addNodeToLayer(Orbit b, Map<String, List<Orbit>> nextLayer) {
        String hashB = b.getMarkedNodeLabel() + "-" + b.getPattern();
        List<Orbit> tmp = nextLayer.getOrDefault(hashB, new ArrayList<>());
        tmp.add(b);
        nextLayer.put(hashB, tmp);
    }

    public void addFrequentEdge(String e) {
        frequentEdges.add(e);
    }

    public void addLayer(List<Orbit> layer) {
        lattice.add(layer);
    }
    
    public Orbit getNode(int id) {
        Pair<Integer, Integer> p = nodes.get(id);
        return lattice.get(p.getA()).get(p.getB());
    }
    
    public List<Orbit> getNodes() {
        return lattice.stream().flatMap(l -> l.stream()).collect(Collectors.toList());
    }
    
    public Set<Integer> getNodeIDs() {
        return nodes.keySet();
    }

    public List<Orbit> getLayer(int l) {
        return lattice.get(l);
    }

    public int getNumLayers() {
        return lattice.size();
    }

    public int getNumNodes() {
        return nodes.size();
    }

    public JBlissPattern getPattern(int id) {
        return patterns.get(id);
    }

    public Collection<JBlissPattern> getPatterns() {
        return patterns.values();
    }

    public int getNumPatterns() {
        return patterns.size();
    }
    
    public List<JBlissPattern> getFrequentPatterns(double tau, int size) {
        return patterns.values().parallelStream()
                .filter(p -> p.frequency() >= tau && p.frequency() != Integer.MAX_VALUE && p.getNumberOfVertices() == size)
                .collect(Collectors.toList());
    }
    
    public void setFrequency(int pattern, double freq) {
        JBlissPattern p = patterns.get(pattern);
        p.setFrequency(freq);
    }
    
    public void killPattern(int pattern) {
        JBlissPattern p = patterns.get(pattern);
        p.setFrequency(0);
    }
    
    public boolean killPatternAndOrbits(int pattern) {
        JBlissPattern p = patterns.get(pattern);
        p.setFrequency(0);
        boolean itKilled = false;
        for (int orbitID : p.getOrbits()) {
            if (!getNode(orbitID).isDead()) {
                itKilled = true;
                getNode(orbitID).setDead(true);
            }
        }
        return itKilled;
    }

}