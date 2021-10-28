package it.isi.approxmni.graph;

import it.isi.approxmni.domain.Edge;
import it.isi.approxmni.domain.Node;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LabeledGraphTest {

    @Test
    public void basic_test() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(0, 2));
        nodes.add(new Node(1, 3));
        nodes.add(new Node(2, 2));
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(0, 1));
        edges.add(new Edge(1, 2));
        LabeledGraph labeledGraph = new LabeledGraph(nodes, edges);
        assertEquals(2, labeledGraph.getNodeLabel(0));
    }
}
