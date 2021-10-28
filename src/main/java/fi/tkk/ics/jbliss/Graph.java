/* 
 * @(#)Graph.java
 *
 * Copyright 2007-2010 by Tommi Junttila.
 * Released under the GNU General Public License version 3.
 */
package fi.tkk.ics.jbliss;

import com.google.common.collect.Maps;
import cz.adamh.utils.NativeUtils;
import fi.tkk.ics.jbliss.pattern.JBlissPattern;
import it.isi.approxmni.domain.Edge;
import it.isi.approxmni.domain.Node;
import java.io.IOException;
import java.io.Serializable;
import org.apache.commons.lang3.SystemUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * An undirected graph.Vertices can be colored (with integers) and self-loops
 are allowed but multiple edges between vertices are ignored.
 *
 * @author Tommi Junttila
 */
public class Graph implements Serializable {

    protected JBlissPattern pattern;
    protected Reporter _reporter;
    protected Object _reporter_param;

    protected void _report(int[] aut) {
        if (_reporter == null) {
            return;
        }

        int numVertices = pattern.getNumberOfVertices();

        Map<Integer, Integer> real_aut = Maps.newHashMap();

        for (int i = 0; i < numVertices; ++i) {
            real_aut.put(i, aut[i]);
        }

        _reporter.report(real_aut, _reporter_param);
    }

    /* The internal JNI interface to true bliss */
    public static native long create();

    public static native void destroy(long true_bliss);

    public static native int _add_vertex(long true_bliss, int color);

    public static native void _add_edge(long true_bliss, int v1, int v2);

    protected native void _find_automorphisms(long true_bliss, Reporter r);

    public static native int[] _canonical_labeling(long true_bliss, Reporter r);

    /**
     * Create a new undirected graph with no vertices or edges.
     * @param pattern
     */
    public Graph(JBlissPattern pattern) {
        this.pattern = pattern;
    }

    private long createBliss() {
        List<Node> vertices = pattern.getVertices();
        List<Edge> edges = pattern.getEdges();

        long bliss = create();
        assert bliss != 0;

        vertices.forEach(node -> _add_vertex(bliss, node.getLabel()));
        edges.forEach(edge -> _add_edge(bliss, edge.getSrc(), edge.getDst()));
        return bliss;
    }

    public void findAutomorphisms(Reporter reporter, Object reporter_param) {
        long bliss = createBliss();

        _reporter = reporter;
        _reporter_param = reporter_param;
        _find_automorphisms(bliss, _reporter);
        destroy(bliss);
        _reporter = null;
        _reporter_param = null;
    }

    public void fillCanonicalLabeling(Map<Integer, Integer> canonicalLabelling) {
        fillCanonicalLabeling(null, null, canonicalLabelling);
    }

    public void fillCanonicalLabeling(Reporter reporter, Object reporter_param, Map<Integer, Integer> canonicalLabellling) {
        int numVertices = pattern.getNumberOfVertices();
        long bliss = createBliss();

        _reporter = reporter;
        _reporter_param = reporter_param;
        int[] cf = _canonical_labeling(bliss, _reporter);
        destroy(bliss);

        canonicalLabellling.clear();

        for (int i = 0; i < numVertices; ++i) {
            canonicalLabellling.put(i, cf[i]);
        }

        _reporter = null;
        _reporter_param = null;
    }

    static {
        int systemBits;

        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            // Get system bits
            systemBits = unsafe.addressSize() * 8;

            if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX) {
                if (systemBits == 64) {
                    NativeUtils.loadLibraryFromJar("/libjbliss-mac.so");
                } else {
                    throw new UnsupportedOperationException("Library not compiled for MAC " + systemBits + " bits");
                }
            } else if (SystemUtils.IS_OS_LINUX) {
                if (systemBits == 64) {
                    NativeUtils.loadLibraryFromJar("/libjbliss-linux.so");
                } else {
                    throw new UnsupportedOperationException("Library not compiled for Linux " + systemBits + " bits");
                }
            } else if (SystemUtils.IS_OS_WINDOWS) {
                if (systemBits == 64) {
                    NativeUtils.loadLibraryFromJar("/libjbliss-win.dll");
                } else {
                    throw new UnsupportedOperationException("Library not compiled for Windows " + systemBits + " bits");
                }
            } else {
                throw new UnsupportedOperationException("Library not compiled for " + SystemUtils.OS_NAME);
            }
        } catch (IllegalAccessException | NoSuchFieldException | IOException e) {
            throw new RuntimeException("Unable to load correct jbliss library for system: " + SystemUtils.OS_NAME + " " + SystemUtils.OS_ARCH + " " + SystemUtils.OS_NAME, e);
        }
    }
}
