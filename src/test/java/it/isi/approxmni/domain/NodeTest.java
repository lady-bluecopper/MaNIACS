package it.isi.approxmni.domain;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NodeTest {

    @Test
    public void basic_test() {
        Node node = new Node(1, 2);
        assertEquals(1, node.getIndex());
        assertEquals(2, node.getLabel());
        Node node2 = new Node(1,2);
        Assert.assertTrue(node.equals(node2));

    }
}
