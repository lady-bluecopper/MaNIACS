package fi.tkk.ics.jbliss.pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fi.tkk.ics.jbliss.Graph;
import fi.tkk.ics.jbliss.Reporter;
import anonymous.maniac.domain.Edge;
import anonymous.maniac.domain.Node;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class JBlissPattern implements Serializable {
    
    private int id;
    private double freq;
    private List<Node> vertices;
    private List<Edge> edges;
    private Set<Integer> orbits;
    private Set<Integer> parents;
    private String unlabeledHashString;
    private String labeledHashString;
    private VertexPositionEquivalence automorphisms;
    private final Map<Integer, Integer> canonicalLabelling;
    private final Graph jblissGraph;
    

    public JBlissPattern(int id, Collection<Node> nodes, Map<Integer, Set<Integer>> adj) {
        this.id = id;
        this.freq = Integer.MAX_VALUE;
        this.vertices = Lists.newArrayList(nodes);
        this.edges = Lists.newArrayList();
        this.orbits = Sets.newHashSet();
        adj.entrySet().forEach(e -> e.getValue().forEach(n -> {
            if (e.getKey() < n) {
                addEdge(e.getKey(), n);
            }
        }));
        this.parents = Sets.newHashSet();
        this.unlabeledHashString = null;
        this.labeledHashString = null;
        this.automorphisms = null;
        this.canonicalLabelling = Maps.newHashMap();
        this.jblissGraph = new Graph(this);
    }
    
    public JBlissPattern(Collection<Node> nodes, Map<Integer, Set<Integer>> adj) {
        this.id = -1;
        this.freq = Integer.MAX_VALUE;
        this.orbits = Sets.newHashSet();
        this.parents = Sets.newHashSet();
        this.unlabeledHashString = null;
        this.labeledHashString = null;
        this.automorphisms = null;
        this.canonicalLabelling = Maps.newHashMap();
        initialize(nodes, adj);
        this.jblissGraph = new Graph(this);
    }
    
    // used for finding the canonical unlabeled pattern
    public JBlissPattern(Collection<Node> nodes, Collection<Edge> edges) {
        this.id = -1;
        this.freq = Integer.MAX_VALUE;
        this.orbits = Sets.newHashSet();
        this.parents = Sets.newHashSet();
        this.unlabeledHashString = null;
        this.labeledHashString = null;
        this.automorphisms = null;
        this.canonicalLabelling = Maps.newHashMap();
        this.jblissGraph = new Graph(this);
        this.vertices = Lists.newArrayList();
        this.edges = Lists.newArrayList();
        Map<Integer, Integer> tmp = Maps.newHashMap();
        nodes.stream().forEach(node -> {
            int vid = vertices.size();
            this.vertices.add(new Node(vid, 0));
            tmp.put(node.getIndex(), vid);
        });
        edges.stream().forEach(e -> addEdge(tmp.get(e.getSrc()), tmp.get(e.getDst())));
    }
    
    public JBlissPattern() {
        this.id = -1;
        this.freq = Integer.MAX_VALUE;
        this.orbits = Sets.newHashSet();
        this.parents = Sets.newHashSet();
        this.unlabeledHashString = null;
        this.labeledHashString = null;
        this.automorphisms = null;
        this.canonicalLabelling = Maps.newHashMap();
        this.jblissGraph = new Graph(this);
    }
    
    private void initialize(Collection<Node> nodes, Map<Integer, Set<Integer>> adj) {
        this.vertices = Lists.newArrayList();
        this.edges = Lists.newArrayList();
        Map<Integer, Integer> tmp = Maps.newHashMap();
        nodes.stream().forEach(node -> {
            int vid = vertices.size();
            this.vertices.add(new Node(vid, node.getLabel()));
            tmp.put(node.getIndex(), vid);
        });
        adj.entrySet().forEach(e -> e.getValue().forEach(n -> {
            if (e.getKey() < n) {
                addEdge(tmp.get(e.getKey()), tmp.get(n));
            }
        }));
    }
    
    public int initialize(Collection<Node> nodes, Map<Integer, Set<Integer>> adj, int marked) {
        this.vertices = Lists.newArrayList();
        this.edges = Lists.newArrayList();
        Map<Integer, Integer> tmp = Maps.newHashMap();
        nodes.stream().forEach(node -> {
            int vid = vertices.size();
            this.vertices.add(new Node(vid, node.getLabel()));
            tmp.put(node.getIndex(), vid);
        });
        adj.entrySet().forEach(e -> e.getValue().forEach(n -> {
            if (e.getKey() < n) {
                addEdge(tmp.get(e.getKey()), tmp.get(n));
            }
        }));
        return tmp.get(marked);
    }

    protected class AutomorphismReporter implements Reporter {
        VertexPositionEquivalence equivalences;

        public AutomorphismReporter(VertexPositionEquivalence equivalences) {
            this.equivalences = equivalences;
        }

        @Override
        public void report(Map<Integer, Integer> generator, Object user_param) {
            generator.entrySet().forEach(e -> equivalences.addEquivalence(e.getKey(), e.getValue()));
        }
    }

    protected void fillVertexPositionEquivalences(VertexPositionEquivalence vertexPositionEquivalences) {
        for (int i = 0; i < vertices.size(); ++i) {
            vertexPositionEquivalences.addEquivalence(i, i);
        }
        AutomorphismReporter reporter = new AutomorphismReporter(vertexPositionEquivalences);
        jblissGraph.findAutomorphisms(reporter, null);
        vertexPositionEquivalences.propagateEquivalences();
    }
    
    public void findAutomorphisms() {
        if (automorphisms == null) {
            automorphisms = new VertexPositionEquivalence();
            fillVertexPositionEquivalences(automorphisms);
        }
    }
    
    public VertexPositionEquivalence getAutomorphisms() {
        return automorphisms;
    }
    
    public int getNumAutos() {
        return automorphisms.getDistinctEquivalences().size();
    }
    
    public Set<Integer> getAutomorphismsOf(int id) {
        return automorphisms.getEquivalences(id);
    }
    
    public void addEdge(int src, int dst) {
        Edge edge = new Edge(src, dst);
        edges.add(edge);
    }
    
    public List<Edge> getEdges() {
        return edges;
    }
    
    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }
    
    public int getNumberOfEdges() {
        return edges.size();
    }

    public void addVertex(Node node) {
        vertices.add(node);
    }

    public List<Node> getVertices() {
        return vertices;
    }
    
    public void setVertices(List<Node> vertices) {
        this.vertices = vertices;
    }
    
    public int getNumberOfVertices() {
        return vertices.size();
    }
    
    public void setFrequency(double freq) {
        this.freq = Math.min(this.freq, freq);
    }
    
    public double frequency() {
        return freq;
    }
    
    public void addOrbit(int orbit) {
        orbits.add(orbit);
    }
    
    public Set<Integer> getOrbits() {
        return orbits;
    }
    
    public void setOrbits(Set<Integer> orbits) {
        this.orbits = orbits;
    }
    
    public void addParent(int parent) {
        this.parents.add(parent);
    }
    
    public void addParents(Set<Integer> parents) {
        this.parents.addAll(parents);
    }
    
    public Set<Integer> getParents() {
        return this.parents;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }

    public void computeCanonicalLabeling() {
        if (canonicalLabelling.isEmpty()) {
            fillCanonicalLabelling(canonicalLabelling);
        }
    }
    
    protected void fillCanonicalLabelling(Map<Integer, Integer> canonicalLabelling) {
        jblissGraph.fillCanonicalLabeling(canonicalLabelling);
    }

    public int turnCanonical(int marked) {
        findAutomorphisms();
        computeCanonicalLabeling();

        boolean allEqual = true;
        for (Entry<Integer, Integer> e : canonicalLabelling.entrySet()) {
            if (e.getKey() != e.getValue()) {
                allEqual = false;
            }
        }
        if (allEqual) {
            return marked;
        }

        List<Node> oldVertices = Lists.newArrayList(vertices);
        for (int i = 0; i < vertices.size(); ++i) {
            int newPos = canonicalLabelling.get(i);
            // If position didn't change, do nothing
            if (newPos == i) {
                continue;
            }
            vertices.set(newPos, new Node(newPos, oldVertices.get(i).getLabel()));
        }

        for (int i = 0; i < edges.size(); ++i) {
            Edge edge = edges.get(i);

            int srcPos = edge.getSrc();
            int dstPos = edge.getDst();

            int convertedSrcPos = canonicalLabelling.get(srcPos);
            int convertedDstPos = canonicalLabelling.get(dstPos);

            if (convertedSrcPos < convertedDstPos) {
                edge.setSrc(convertedSrcPos);
                edge.setDst(convertedDstPos);
            } else {
                // If we changed the position of source and destination due to
                // relabel, we also have to change the labels to match this
                // change.
                edge.setSrc(convertedDstPos);
                edge.setDst(convertedSrcPos);
            }
        }
        edges.sort((Edge o1, Edge o2) -> {
            if (o1.getSrc() != o2.getSrc()) {
                return Integer.compare(o1.getSrc(), o2.getSrc());
            }
            return Integer.compare(o1.getDst(), o2.getDst());
        });
        VertexPositionEquivalence canonicalAutomorphisms = new VertexPositionEquivalence();
        Map<Integer, Set<Integer>> oldAutomorphisms = automorphisms.getEquivalences();
        oldAutomorphisms.entrySet().forEach(e -> {
            Set<Integer> canonicalEquivalences = e.getValue()
                    .stream()
                    .map(eq -> canonicalLabelling.get(eq))
                    .collect(Collectors.toSet());
            canonicalAutomorphisms.addEquivalences(canonicalLabelling.get(e.getKey()), canonicalEquivalences);
        });
        automorphisms = canonicalAutomorphisms;
        return canonicalLabelling.get(marked);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JBlissPattern that = (JBlissPattern) o;
        if (that.getNumberOfVertices() != this.getNumberOfVertices() || that.getNumberOfEdges() != this.getNumberOfEdges()) {
            return false;
        }
        for (int i = 0; i < this.vertices.size(); i++) {
            if (!this.vertices.get(i).equals(that.vertices.get(i))) {
                return false;
            }
        }
        for (int i = 0; i < this.edges.size(); i++) {
            if (!this.edges.get(i).equals(that.edges.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return edges.hashCode();
    }
    
    // only for single-edge patterns
    public String singleEdgeHashCode() {
        return vertices.get(0).getLabel() + "-" + vertices.get(1).getLabel();
    }
    
    public String getUnlabeledHashCode() {
        if (unlabeledHashString == null) {
            JBlissPattern tmp = new JBlissPattern(vertices, edges);
            tmp.turnCanonical(0);
            unlabeledHashString = tmp.edges.toString();
        }
        return unlabeledHashString;
    }
    
    public String getLabeledhashCode() {
        if (labeledHashString == null) {
            String h = vertices.stream().map(n -> n.getLabel()).collect(Collectors.toList()).toString();
            labeledHashString = h + ";" + edges.toString();
        }
        return labeledHashString;
    }
    
    public void printPattern() {
        System.out.println("Pattern: " + this.id + 
                " |N|=" + this.vertices.size() + 
                " |E|=" + this.edges.size() + 
                " FREQ=" + this.freq + " >> " +
                this.vertices.toString() + " " +
                this.edges.toString() + " " +
                "P=" + this.parents.toString() + " " +
                "O=" + this.orbits.toString() + " "
                );
    }
    
    @Override
    public String toString() {
        String text = String.valueOf(this.freq);
        text += "\t";
        text += this.vertices.toString();
        text += "\t";
        text += this.edges.toString();
        return text;
    }
    
    public void serialize(ObjectOutputStream out) {
        try {
            out.writeInt(id);
            out.writeObject(vertices);
            out.writeObject(edges);
            out.writeObject(orbits);
            out.writeObject(parents);
        } catch (IOException ex) {
            System.out.println("Error in writing pattern to disk.");
        }
    }
    
    public void readPattern(ObjectInputStream in) {
        try {
            id = in.readInt();
            vertices = (List<Node>) in.readObject();
            edges = (List<Edge>) in.readObject();
            orbits = (Set<Integer>) in.readObject();
            parents = (Set<Integer>) in.readObject();
        } catch (IOException ex) {
            System.out.println("Error in reading pattern from disk.");
        } catch (ClassNotFoundException ex) {
            System.out.println("Error in casting field of pattern.");
        }
    }
}