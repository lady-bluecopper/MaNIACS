package anonymous.maniac.domain;

import java.io.Serializable;
import java.util.Objects;

public class Edge implements Serializable {

    private int src;
    private int dst;
    
    public Edge() {
        
    }

    public Edge(int src, int dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public String toString() {
        return "[" + src + "-" + dst + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return (src == edge.src && dst == edge.dst) ||
                (src == edge.dst && dst == edge.src);
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }

    public int getSrc() {
        return src;
    }

    public void setSrc(int src) {
        this.src = src;
    }

    public int getDst() {
        return dst;
    }

    public void setDst(int dst) {
        this.dst = dst;
    }
}
