package net.voxland.codeconsultant.ai;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ElizaTalkerTest {

    @Test
    public void serialize() throws Exception {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(outBytes);
        ElizaTalker inTalker = new ElizaTalker();
        String hello = inTalker.processInput("hello");
        outputStream.writeObject(inTalker);
        outputStream.flush();
        outputStream.close();

        ByteArrayInputStream inBytes = new ByteArrayInputStream(outBytes.toByteArray());
        ElizaTalker outTalker = (ElizaTalker) new ObjectInputStream(inBytes).readObject();

        System.out.println(hello);
        System.out.println(outTalker.processInput("hello"));
    }

}
