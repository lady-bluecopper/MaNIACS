package it.isi.approxmni.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TripletTest {

    @Test
    public void basic_test() {
        Triplet<Integer, Integer, Integer> triplet = new Triplet<>(1, 2, 3);
        assertEquals(1, (int) triplet.getA());
        assertEquals(2, (int) triplet.getB());
        assertEquals(3, (int) triplet.getC());
        Triplet<Integer, Integer, Integer> triplet1 = new Triplet<>(1, 2, 3);
        assertEquals(triplet, triplet1);

    }
}
