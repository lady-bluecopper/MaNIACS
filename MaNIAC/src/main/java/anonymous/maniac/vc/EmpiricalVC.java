package anonymous.maniac.vc;

import anonymous.maniac.domain.Orbit;
import anonymous.maniac.domain.Pair;
import anonymous.maniac.utils.Settings;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.math3.util.CombinatoricsUtils;

public class EmpiricalVC {

    public static double computeVCLayer(List<Orbit> layer, HashMap<Integer, BitSet> T, boolean isUB) {
        Map<Integer, List<Integer>> supportUBs = new HashMap();
        Map<Integer, Set<BitSet>> images = new HashMap<>();
        // for each subtree of the lattice
        for (Orbit o : layer) {
            if (!o.isDead()) {
                int markedLabel = o.getMarkedNodeLabel();
                Set<BitSet> I = images.getOrDefault(markedLabel, new HashSet<>());
                if (isUB || !I.contains(T.get(o.getId()))) {
                    I.add(T.get(o.getId()));
                    images.put(markedLabel, I);
                    List<Integer> V = supportUBs.getOrDefault(markedLabel, new ArrayList<>());
                    V.add(T.get(o.getId()).cardinality());
                    supportUBs.put(markedLabel, V);
                }
            }
        }
        List<Pair<Integer, List<Integer>>> C = supportUBs.values().stream()
                .filter(s -> !s.isEmpty())
                .map(V -> {
                    Collections.sort(V, (Integer o1, Integer o2) -> -Integer.compare(o1, o2));
                    return new Pair<Integer, List<Integer>>(Math.min(V.get(0), floorLog2(V.size() + 1)), V);
                })
                .collect(Collectors.toList());
        Collections.sort(C, (Pair<Integer, List<Integer>> t1, Pair<Integer, List<Integer>> t2) 
                -> -Integer.compare(t1.getA(), t2.getA()));
        double d = 0;
        for (int i = 0; i < C.size(); i++) {
            Pair<Integer, List<Integer>> t = C.get(i);
            List<Integer> V = t.getB();
            // if <= d don't bother examining it
            if (t.getA() > d) {
                List<Integer> w = initializeVector(t.getA());
                int j = 0;
                while (j < V.size() && j < w.size()) {
                    if (V.get(j) < w.get(j)) {
                        C.add(i, new Pair<>(t.getA() - 1, t.getB()));
                        w = initializeVector(C.get(i).getA() - 1);
                    } else {
                        j += 1;
                    }
                }
                // Empirical VC dimension is the max among all the subtrees
                d = Math.max(d, C.get(i).getA());
            }
        }
        System.out.print("VC: " + d + " ");
        return d;
    }

    private static List<Integer> initializeVector(int size) {
        List<Integer> w = new ArrayList();
        for (int i = 0; i < size; i++) {
            long fact = CombinatoricsUtils.binomialCoefficient(size, size - i);
            for (int j = 0; j < fact; j++) {
                w.add(size - i);
            }
        }
        return w;
    }

    public static double getEmpiricalFreq(double d) {
        double epsilon = getEpsilon(d);
        System.out.println(" EPSILON: " + epsilon);
        return Settings.frequency - epsilon / 2;
    }
    
    public static double getEpsilonLayer(List<Orbit> layer, HashMap<Integer, BitSet> T, boolean isUB) {
        double d = computeVCLayer(layer, T, isUB);
        double epsilon = getEpsilon(d);
        return epsilon;
    }

    private static double getEpsilon(double d) {
        return Math.sqrt(Settings.c * (d + Math.log(Settings.patternSize / Settings.failure)) / Settings.sampleSize);
    }
    
    private static int floorLog2(double x) {
        return (int)Math.floor(Math.log(x)/Math.log(2.0) + 1e-10);
    }

}
