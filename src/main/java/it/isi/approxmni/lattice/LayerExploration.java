package it.isi.approxmni.lattice;

import com.google.common.collect.Sets;
import it.isi.approxmni.domain.Edge;
import it.isi.approxmni.graph.LabeledGraph;
import it.isi.approxmni.domain.Orbit;
import it.isi.approxmni.domain.Pair;
import it.isi.approxmni.utils.Settings;
import it.isi.approxmni.utils.StopWatch;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.SerializationUtils;

public class LayerExploration {

    public static void exploreLayer(LabeledGraph graph, LatticeTree tree, int layer,
            Map<Integer, BitSet> T, Map<Integer, Integer> inverseMap, double epsilonPlus, int sampleSize) {
        List<Orbit> orbitsToExamine = tree.getLayer(layer);
        StopWatch watch = new StopWatch();
        for (Orbit o : orbitsToExamine) {
            // another orbit of the pattern is not frequent
            if (o.isDead()) {
                continue;
            }
            watch.start();
            // the candidate vertices are those in the support sets of all the parents
            BitSet tmp = (BitSet) (T.get(o.getId())).clone();
            // search for a valid assignment for each candidate vertex
            int numCands = tmp.cardinality();
            int support = 0;
            for (int v = tmp.nextSetBit(0); v >= 0; v = tmp.nextSetBit(v + 1)) {
                HashMap<Integer, Integer> match = new HashMap();
                match.put(0, inverseMap.get(v));
                if (existsOR(o, match, Sets.newHashSet(), graph)) {
                    support += 1;
                    tmp.set(v, false);
                }
                numCands -= 1;
                // this orbit cannot be frequent
                if (numCands + support < (Settings.frequency - epsilonPlus) * sampleSize) {
                    tree.killPatternAndOrbits(o.getPattern());
                    break;
                }
            }
            if (!o.isDead()) {
                tmp.xor(T.get(o.getId()));
                T.put(o.getId(), tmp);
                tree.setFrequency(o.getPattern(), 1.0 * tmp.cardinality() / sampleSize);
            } else {
                T.remove(o.getId());
            }
        }
//        System.out.println("C" + "\t" 
//                + layer + "\t" 
//                + orbitsToExamine.size() + "\t" 
//                + orbitsToExamine.stream().filter(o -> !o.isDead()).count());
        if (layer == 1) {
            tree.getPatterns().stream()
                    .filter(p -> (p.frequency() >= Settings.frequency - epsilonPlus && p.getNumberOfEdges() == 1))
                    .forEach(p -> tree.addFrequentEdge(p.singleEdgeHashCode()));
        }
    }

    public static boolean existsOR(Orbit o, HashMap<Integer, Integer> match, HashSet<Edge> used, LabeledGraph graph) {
        if (match.size() == o.getNodes().size()) {
            return true;
        }
        int next = match.size();
        List<Integer> previousConnectedNodes = o.getSmallerNeighbours(next);
        // find candidate vertices for matching the orbit node *next*
        Set<Integer> candidates = Sets.newHashSet(graph.getNeighbours(match.get(previousConnectedNodes.get(0))));
        for (int i = 1; i < previousConnectedNodes.size(); i++) {
            candidates.retainAll(graph.getNeighbours(match.get(previousConnectedNodes.get(i))));
        }
        return candidates.parallelStream().anyMatch(u -> {
            // graph node and orbit node must have the same label
            if (graph.getNodeLabel(u) == o.getNodes().get(next).getLabel()) {
                Pair<Boolean, Set<Edge>> isCandMatch = isCandidateMatch(match, used, u, graph.getNeighbours(u), o.getNeighbours(next));
                // recursive step
                if (isCandMatch.getA()) {
                    HashMap<Integer, Integer> copy = SerializationUtils.clone(match);
                    copy.put(next, u);
                    HashSet<Edge> used_copy = SerializationUtils.clone(used);
                    used_copy.addAll(isCandMatch.getB());
                    if (existsOR(o, copy, used_copy, graph)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private static Pair<Boolean, Set<Edge>> isCandidateMatch(HashMap<Integer, Integer> match, Set<Edge> used, int u, Set<Integer> nbs, Set<Integer> o_nbs) {
        Set<Edge> filledEdges = Sets.newHashSet();
        for (Entry<Integer, Integer> entry : match.entrySet()) {
            if (o_nbs.contains(entry.getKey())) {
                Edge e = u < entry.getValue() ? new Edge(u, entry.getValue()) : new Edge(entry.getValue(), u);
                if (used.contains(e)) {
                    return new Pair<>(false, filledEdges);
                } 
                filledEdges.add(e);
                // not a vertex-induced appearance of the pattern    
            } else if (nbs.contains(entry.getValue())) {
                return new Pair<>(false, filledEdges);
            }
        }
        return new Pair<>(true, filledEdges);
    }

}
