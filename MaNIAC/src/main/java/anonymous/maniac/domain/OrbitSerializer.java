package anonymous.maniac.domain;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class OrbitSerializer extends Serializer<Orbit> {

    @Override
    public void write(Kryo kryo, Output output, Orbit t) {
        output.writeInt(t.getId());
        output.writeInt(t.getPattern());
        kryo.writeObject(output, t.getNodes());
        kryo.writeObject(output, t.getParents());
        kryo.writeObject(output, t.getChildren());
        kryo.writeObject(output, t.getAdj());
    }

    @Override
    public Orbit read(Kryo kryo, Input input, Class<? extends Orbit> type) {
        Orbit o = new Orbit();
        o.setId(input.readInt());
        o.setPattern(input.readInt());
        o.setNodes((List<Node>) kryo.readObject(input, java.util.ArrayList.class));
        o.setParents((Set<Integer>) kryo.readObject(input, java.util.HashSet.class));
        o.setChildren((Set<Integer>) kryo.readObject(input, java.util.HashSet.class));
        o.setAdj((HashMap<Integer, Set<Integer>>) kryo.readObject(input, java.util.HashMap.class));
        for (int k : o.getAdj().keySet()) {
            for (int n : o.getAdj().get(k)) {
                if (k < n) {
                    o.increaseNumEdges();
                }
            }
        }
        return o;
    }
}
