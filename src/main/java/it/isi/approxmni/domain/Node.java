package it.isi.approxmni.domain;


import java.io.Serializable;
import java.util.Objects;

public class Node implements Serializable {

    private int index;
    private int label;

    public Node(int index, int label) {
        this.index = index;
        this.label = label;
    }
    
    public Node() {
        
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return index == node.index && label == node.label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, label);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLabel() {
        return label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "(" + index + "," + label + ")";
    }

}
