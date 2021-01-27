package fi.tkk.ics.jbliss.pattern;

import anonymous.maniac.domain.Edge;
import anonymous.maniac.domain.Node;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.List;
import java.util.Set;

/**
 *
 * @author giulia
 */
public class PatternSerializer extends Serializer<JBlissPattern> {

    @Override
    public void write(Kryo kryo, Output output, JBlissPattern t) {
        output.writeInt(t.getId());
        kryo.writeObject(output, t.getVertices());
        kryo.writeObject(output, t.getEdges());
        kryo.writeObject(output, t.getOrbits());
        kryo.writeObject(output, t.getParents());
    }

    @Override
    public JBlissPattern read(Kryo kryo, Input input, Class<? extends JBlissPattern> type) {
        JBlissPattern p = new JBlissPattern();
        p.setId(input.readInt());
        p.setVertices((List<Node>) kryo.readObject(input, java.util.ArrayList.class));
        p.setEdges((List<Edge>) kryo.readObject(input, java.util.ArrayList.class));
        p.setOrbits((Set<Integer>) kryo.readObject(input, java.util.HashSet.class));
        p.addParents((Set<Integer>) kryo.readObject(input, java.util.HashSet.class));
        return p;
    }
    
}