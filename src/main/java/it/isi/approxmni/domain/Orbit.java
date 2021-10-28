package it.isi.approxmni.domain;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fi.tkk.ics.jbliss.pattern.JBlissPattern;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class Orbit implements Serializable {

    private int id;
    private int canonicalId; // id of marked node in the canonical pattern
    private int pattern;
    private int numEdges;
    private String degreeSequence;
    private String labelFrequency;
    private List<Node> nodes;
    private HashMap<Integer, Set<Integer>> adj;
    private Queue<Edge> gray;
    private Set<Integer> parents;
    private Set<Integer> children;
    private boolean dead;

    public Orbit(int id, List<Node> nodes, HashMap<Integer, Set<Integer>> edges, Queue<Edge> gray) {
        this.id = id;
        this.canonicalId = 0;
        this.pattern = -1;
        this.nodes = Lists.newArrayList(nodes);
        this.adj = Maps.newHashMap();
        this.numEdges = 0;
        edges.entrySet().stream().forEach(e -> {
            this.adj.put(e.getKey(), Sets.newHashSet(e.getValue()));
            for (int d : e.getValue()) {
                if (e.getKey() < d) {
                    this.numEdges += 1;
                }
            }
        });
        this.degreeSequence = null;
        this.labelFrequency = null;
        this.gray = new LinkedList(gray);
        this.parents = Sets.newHashSet();
        this.children = Sets.newHashSet();
        this.dead = false;
    }
    
    public Orbit() {
        this.nodes = Lists.newArrayList();
        this.adj = Maps.newHashMap();
        this.gray = new LinkedList();
        this.numEdges = 0;
        this.degreeSequence = null;
        this.labelFrequency = null;
        this.parents = Sets.newHashSet();
        this.children = Sets.newHashSet();
        this.dead = false;
    }
    
    public int getMarkedNodeLabel() {
        return nodes.get(0).getLabel();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public int getCanId() {
        return canonicalId;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }
    
    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }
    
    public void addNode(int index, int label) {
        Node n =  new Node(index, label);
        this.nodes.add(n);
    }
    
    public int getNodeID(int pos) {
        return this.nodes.get(pos).getIndex();
    }
    
    public int getNodeLabel(int pos) {
        return this.nodes.get(pos).getLabel();
    }
    
    public int getNumEdges() {
        return numEdges;
    }
    
    public int getLevel() {
        return this.nodes.size();
    }

    public HashMap<Integer, Set<Integer>> getAdj() {
        return adj;
    }

    public void setAdj(HashMap<Integer, Set<Integer>> adj) {
        this.adj = adj;
    }

    public Queue<Edge> getGray() {
        return gray;
    }

    public void setGray(Queue<Edge> gray) {
        this.gray = gray;
    }

    public int getPattern() {
        return pattern;
    }

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }

    public Set<Integer> getNeighbours(int node) {
        return adj.getOrDefault(node, Sets.newHashSet());
    }
    
    public List<Integer> getSmallerNeighbours(int node) {
        if (adj.keySet().contains(node)) {
            return adj.get(node).stream().filter(n -> n < node).collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    public JBlissPattern computeCanonicalForm(int id) {
        JBlissPattern p = new JBlissPattern(id, nodes, adj);
        int canId = p.turnCanonical(0);
        this.canonicalId = canId;
        return p;
    }

    public boolean isConnected() {
        if (nodes.size() < 2) {
            return true;
        }
        if (!adj.containsKey(nodes.get(nodes.size() - 1).getIndex())) {
            return false;
        }
        return !adj.get(nodes.get(nodes.size() - 1).getIndex()).isEmpty();
    }
    
    public void addEdge(int src, int dst) {
        Set<Integer> tmp = adj.getOrDefault(src, Sets.newHashSet());
        tmp.add(dst);
        adj.put(src, tmp);
        Set<Integer> tmp2 = adj.getOrDefault(dst, Sets.newHashSet());
        tmp2.add(src);
        adj.put(dst, tmp2);
        numEdges += 1;
    }
    
    public void increaseNumEdges() {
        numEdges += 1;
    }

    public void addChild(int id) {
        children.add(id);
    }

    public void removeChild(int id) {
        children.remove(id);
    }

    public void addParent(int id) {
        parents.add(id);
    }

    public void addParents(Set<Integer> ps) {
        parents.addAll(ps);
    }

    public Set<Integer> getParents() {
        return parents;
    }

    public void setParents(Set<Integer> parents) {
        this.parents = parents;
    }

    public Set<Integer> getChildren() {
        return children;
    }
    
    public void setChildren(Set<Integer> children) {
        this.children = children;
    }
    
    public String getDegreeSequence() {
        if (degreeSequence == null) {
            List<Integer> degrees = adj.values().stream().map(s -> s.size()).collect(Collectors.toList());
            Collections.sort(degrees);
            degreeSequence = degrees.toString();
        }
        return degreeSequence;
    }
    
    public String getLabelFrequency() {
        if (labelFrequency == null) {
            HashMap<Integer, Integer> labelFreqs = new HashMap<>();
            nodes.forEach(n -> labelFreqs.put(n.getLabel(), labelFreqs.getOrDefault(n.getLabel(), 0) + 1));
            List<Entry<Integer, Integer>> temp = new ArrayList<>(labelFreqs.entrySet());
            Collections.sort(temp, (Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) -> Integer.compare(o1.getKey(), o2.getKey()));
            labelFrequency = temp.stream().map(e -> e.getKey() + "=" + e.getValue()).reduce("", (String t, String u) -> t + "," + u);
        }
        return labelFrequency;
    }
    
    public Orbit cloneOrbit(int id, int label) {
        Orbit cl = new Orbit();
        cl.setId(this.id + id);
        cl.numEdges = this.numEdges;
        cl.degreeSequence = this.degreeSequence;
        int firstLabel = this.nodes.get(0).getLabel();
        for (Node n : this.nodes) {
            if (n.getLabel() == firstLabel) {
                cl.addNode(n.getIndex(), label);
            } else if (n.getLabel() == label) {
                cl.addNode(n.getIndex(), firstLabel);
            } else {
                cl.addNode(n.getIndex(), n.getLabel());
            }
        }
        for (Entry<Integer, Set<Integer>> e : this.adj.entrySet()) {
            Set<Integer> s = new HashSet(e.getValue());
            cl.adj.put(e.getKey(), s);
        }
        for (int p : this.parents) {
            if (p == 0) {
                cl.addParent(0);
            } else {
                cl.addParent(p + id);
            }
        }
        for (int c : this.children) {
            cl.addChild(c + id);
        }
        return cl;
    }

    public void printOrbit() {
        System.out.print("ID: " + this.id + " DEAD: " + this.dead +
                " PARENTS: " + this.parents.toString() + " CHILDREN: " + this.children.toString() +
                " NUM GRAY: " + this.gray.size() + " LEVEL: " + this.nodes.size() +
                " NUM EDGES: " + numEdges + 
                this.nodes.toString() + " ");
        printEdges();
    }

    public void printEdges() {
        for (Entry<Integer, Set<Integer>> e : adj.entrySet()) {
            for (int n : e.getValue()) {
                if (e.getKey() < n) {
                    System.out.print("[" + e.getKey() + "-" + n + "]");
                }
            }
        }
        System.out.println();
    }
    
    public void serialize(ObjectOutputStream out) {
        try {
            out.writeInt(id);
            out.writeInt(pattern);
            out.writeObject(nodes);
            out.writeObject(parents);
            out.writeObject(children);
            out.writeInt(adj.size());
            for (Entry<Integer, Set<Integer>> e : adj.entrySet()) {
                out.writeInt(e.getKey());
                out.writeObject(e.getValue());
            }
        } catch (IOException ex) {
            System.out.println("Error in writing orbit to disk.");
        }
    }
    
    public void readOrbit(ObjectInputStream in) {
        try {
            id = in.readInt();
            pattern = in.readInt();
            nodes = (List<Node>) in.readObject();
            parents = (Set<Integer>) in.readObject();
            children = (Set<Integer>) in.readObject();
            adj = new HashMap<>();
            int len = in.readInt();
            for (int i=0; i < len; i++) {
                int key = in.readInt();
                Set<Integer> neig = (Set<Integer>) in.readObject();
                adj.put(key, neig);
                for (int n : neig) {
                    if (key < n) {
                        numEdges += 1;
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Error in reading orbit from file.");
        } catch (ClassNotFoundException ex) {
            System.out.println("Error in reading field of orbit.");
        }
    }

}
