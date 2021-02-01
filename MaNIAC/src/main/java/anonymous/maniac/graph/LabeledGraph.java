package anonymous.maniac.graph;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import anonymous.maniac.domain.Edge;
import anonymous.maniac.domain.Node;
import anonymous.maniac.utils.Settings;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LabeledGraph {
    
    private final List<Node> nodes;
    private final HashMap<Integer, Set<Integer>> adj;
    
    public LabeledGraph(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.adj = Maps.newHashMap();
        initialize(edges);
    }
    
    public int numberOfNodes() {
        return nodes.size();
    }
    
    public Map<Integer, Double> getNodeLabelFrequencies(Collection<Integer> sample, double epsilon) {
        HashMap<Integer, Double> freq = new HashMap();
        sample.forEach(node -> freq.put(getNodeLabel(node), freq.getOrDefault(getNodeLabel(node), 0.) + 1./sample.size()));
        return freq.entrySet().stream().filter(e -> e.getValue() >= Settings.frequency - epsilon).collect(Collectors.toMap(i->i.getKey(), i->i.getValue()));
    }
    
    public void initialize(List<Edge> edges) {
        for (Edge e : edges) {
            Set<Integer> tmp = adj.getOrDefault(e.getSrc(), Sets.newHashSet());
            tmp.add(e.getDst());
            adj.put(e.getSrc(), tmp);
            Set<Integer> tmp2 = adj.getOrDefault(e.getDst(), Sets.newHashSet());
            tmp2.add(e.getSrc());
            adj.put(e.getDst(), tmp2);
        }
    }
    
    public int getNodeLabel(int node) {
        return nodes.get(node).getLabel();
    }
    
    public Set<Integer> getNeighbours(int node) {
        return adj.getOrDefault(node, Sets.newHashSet());
    } 
    
    public Set<Integer> getNodeIDs() {
        return nodes.stream().map(n -> n.getIndex()).collect(Collectors.toSet());
    }
    
}
