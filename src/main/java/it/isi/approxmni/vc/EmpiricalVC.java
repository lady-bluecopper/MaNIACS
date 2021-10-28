package it.isi.approxmni.vc;

import it.isi.approxmni.domain.Orbit;
import it.isi.approxmni.domain.Pair;
import it.isi.approxmni.domain.Triplet;
import it.isi.approxmni.utils.Settings;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    public static double getEpsilonLayer_(List<Orbit> layer, HashMap<Integer, BitSet> T, boolean isUB, int sampleSize) {
        double d = computeVCLayer(layer, T, isUB);
        double epsilon = getEpsilon(d, sampleSize);
        return epsilon;
    }

    public static double getEpsilonLayer(List<Orbit> layer, HashMap<Integer, BitSet> T, boolean isUB, int sampleSize) {
        double d = computeEVC(layer, T, isUB);
        double epsilon = getEpsilon(d, sampleSize);
        return epsilon;
    }

    public static double getEpsilon(double d, int sampleSize) {
        return Math.sqrt(Settings.c * (d + Math.log(Settings.patternSize / Settings.failure)) / sampleSize);
    }

    private static int floorLog2(double x) {
        return (int) Math.floor(Math.log(x) / Math.log(2.0) + 1e-10);
    }

    public static double computeEVC(List<Orbit> layer, HashMap<Integer, BitSet> T, boolean isUB) {
        Map<Integer, Map<Integer, Integer>> supports = new HashMap<>();
        Map<Integer, Set<BitSet>> images = new HashMap<>();
        Map<Integer, List<Integer>> supportUBs = new HashMap();
        Map<Integer, Integer> labelEVC = new HashMap<>();
        Map<Integer, Integer> numOrbitsPerLayer = new HashMap<>();

        for (Orbit o : layer) {
            if (!o.isDead()) {
                int markedLabel = o.getMarkedNodeLabel();
                numOrbitsPerLayer.put(markedLabel, numOrbitsPerLayer.getOrDefault(markedLabel, 0) + 1);
                Map<Integer, Integer> thisLabelMap = supports.getOrDefault(markedLabel, new HashMap<>());
                for (int v = T.get(o.getId()).nextSetBit(0); v >= 0; v = T.get(o.getId()).nextSetBit(v + 1)) {
                    thisLabelMap.put(v, thisLabelMap.getOrDefault(v, 0) + 1);
                }
                supports.put(markedLabel, thisLabelMap);
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
        for (int label : supports.keySet()) {
            int this_d = getEVCForLabel(supports.get(label).entrySet(), images.get(label));
//            List<Entry<Integer, Integer>> counts = new ArrayList<>(supports.get(label).entrySet());
//            Collections.sort(counts, (Entry<Integer, Integer> i1, Entry<Integer, Integer> i2)
//                    -> -Integer.compare(i1.getValue(), i2.getValue()));
//            int this_d = floorLog2(counts.get(0).getValue()) + 1;
//            while (this_d > 1) {
//                if (counts.size() < this_d || counts.get(this_d - 1).getValue() < Math.pow(2, this_d - 1)) {
//                    this_d -= 1;
//                } else {
//                    break;
//                }
//            }
            labelEVC.put(label, this_d);
        }
//        System.out.println(labelEVC.toString());
        List<Triplet<Integer, Integer, List<Integer>>> C = supportUBs.entrySet().stream()
                .filter(s -> !s.getValue().isEmpty())
                .map(e -> {
                    List<Integer> V = e.getValue();
                    Collections.sort(V, (Integer o1, Integer o2) -> -Integer.compare(o1, o2));
                    return new Triplet<Integer, Integer, List<Integer>>(e.getKey(), Math.min(V.get(0), floorLog2(V.size() + 1)), V);
                })
                .collect(Collectors.toList());
        Collections.sort(C, (Triplet<Integer, Integer, List<Integer>> t1, Triplet<Integer, Integer, List<Integer>> t2)
                -> -Integer.compare(t1.getB(), t2.getB()));
        for (int i = 0; i < C.size(); i++) {
            Triplet<Integer, Integer, List<Integer>> tr = C.get(i);
            int ubd = tr.getB();
            List<Integer> V = tr.getC();
            List<Integer> w = initializeVector(ubd);
            int j = 0;
            while (j < V.size() && j < w.size()) {
                if (V.get(j) < w.get(j)) {
                    ubd -= 1;
                    w = initializeVector(ubd);
                } else {
                    j += 1;
                }
            }
//            System.out.println(tr.getA() + ":=" + ubd);
            labelEVC.put(tr.getA(), Math.min(labelEVC.get(tr.getA()), ubd));
        }
        double d = labelEVC.isEmpty() ? 0 : Collections.max(labelEVC.values());
        System.out.print("VC\t" + (int) d + "\t");
        return d;
    }

    private static int getEVCForLabel(Collection<Entry<Integer, Integer>> supports, Set<BitSet> images) {
        if (supports.isEmpty()) {
            return 0;
        }
        List<Entry<Integer, Integer>> counts = new ArrayList<>(supports);
        Collections.sort(counts, (Entry<Integer, Integer> i1, Entry<Integer, Integer> i2)
                -> -Integer.compare(i1.getValue(), i2.getValue()));
        int this_d = floorLog2(counts.get(0).getValue()) + 1;
        while (this_d > 1) {
            if (counts.size() < this_d || counts.get(this_d - 1).getValue() < Math.pow(2, this_d - 1)) {
                this_d -= 1;
            } else {
                for (BitSet b : images) {
                    int sup = 0;
                    for (Entry<Integer, Integer> e : counts) {
                        if (b.get(e.getKey()) && e.getValue() >= Math.pow(2, this_d - 1)) {
                            sup += 1;
                            if (sup >= this_d) {
                                return this_d;
                            }
                        }
                    }
                }
                this_d -= 1;
            }
        }
        return this_d;
    }
}
