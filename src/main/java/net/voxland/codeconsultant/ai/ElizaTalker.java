package net.voxland.codeconsultant.ai;

import net.voxland.codeconsultant.ai.eliza.ElizaMain;

import java.io.InputStream;
import java.io.Serializable;

public class ElizaTalker implements Talker, Serializable{
    private ElizaMain eliza;

    public ElizaTalker() {
        InputStream script = getClass().getClassLoader().getResourceAsStream("net/voxland/codeconsultant/ai/eliza/script.txt");

        eliza = new ElizaMain();
        int res = eliza.readScript(script);
        if (res != 0) {
            throw new RuntimeException("Cannot connect to Eliza");
        }

    }

    public String processInput(String input) {
        return eliza.processInput(input);
    }
}
