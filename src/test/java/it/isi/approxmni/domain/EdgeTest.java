package it.isi.approxmni.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class EdgeTest {

    @Test
    public void basic_test() {
        Edge edge = new Edge(1 ,2);
        assertEquals(1, edge.getSrc());
        assertEquals(2, edge.getDst());
    }

    @Test
    public void basic_invertedEdgeTest() {
        Edge edge1 = new Edge(1 ,2);
        Edge edge2 = new Edge(1 ,2);
        assertTrue(edge1.equals(edge2));
    }
}
