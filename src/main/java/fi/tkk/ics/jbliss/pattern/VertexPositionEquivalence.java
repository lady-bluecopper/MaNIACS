package fi.tkk.ics.jbliss.pattern;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class VertexPositionEquivalence {
    private Map<Integer, Set<Integer>> equivalences;

    public VertexPositionEquivalence() {
        this.equivalences = Maps.newHashMap();
    }

    public VertexPositionEquivalence(Map<Integer, Set<Integer>> map) {
        map.entrySet().forEach(entry -> equivalences.put(entry.getKey(), Sets.newHashSet(entry.getValue())));
    }

    public void addAll(VertexPositionEquivalence vertexPositionEquivalences) {
        vertexPositionEquivalences.getEquivalences().entrySet().forEach(e -> {
            Set<Integer> tmp = equivalences.get(e.getKey());
            tmp.addAll(e.getValue());
            equivalences.put(e.getKey(), tmp);
        });
    }

    public void addEquivalence(int pos1, int pos2) {
        Set<Integer> tmp = equivalences.getOrDefault(pos1, Sets.newHashSet());
        tmp.add(pos2);
        equivalences.put(pos1, tmp);
    }
    
    public void addEquivalences(int pos1, Set<Integer> s) {
        Set<Integer> tmp = equivalences.getOrDefault(pos1, Sets.newHashSet());
        tmp.addAll(s);
        equivalences.put(pos1, tmp);
    }

    public Set<Integer> getEquivalences(int pos) {
        return equivalences.getOrDefault(pos, Sets.newHashSet());
    }
    
    public Map<Integer,Set<Integer>> getEquivalences() {
        return equivalences;
    }
    
    public Set<Set<Integer>> getDistinctEquivalences() {
        return equivalences.values().stream().collect(Collectors.toSet());
    }

    public void propagateEquivalences() {
        for (int i : equivalences.keySet()) {
            equivalences.get(i)
                    .parallelStream()
                    .filter(equivalentPosition -> !(equivalentPosition == i))
                    .forEach(equivalentPosition -> {
                        Set<Integer> tmp = equivalences.get(equivalentPosition);
                        tmp.addAll(equivalences.get(i));
                        equivalences.put(equivalentPosition, tmp);
            });
        }
    }
    
    public void printEquivalences() {
        for (int i : equivalences.keySet()) {
            System.out.println(i + "-->" + equivalences.get(i).toString());
        }
    }

}