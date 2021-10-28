package it.isi.approxmni.domain;

import org.junit.Assert;
import org.junit.Test;

public class PairTest {

    @Test
    public void basic_test() {
        Pair<Integer, Integer> pair = new Pair<>(1, 2);
        Assert.assertEquals(1, (int)pair.getA());
        Assert.assertEquals(2, (int)pair.getB());
        Pair<Integer, Integer> pair2 = new Pair<>(1, 2);
        Assert.assertEquals(pair, pair2);
    }
}
