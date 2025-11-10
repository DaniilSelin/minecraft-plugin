package load.impl;

import load.ILoad;
import dialogue.models.Dialogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class LoadDialogue implements ILoad {
    @Override
    public Dialogue load(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(path), Dialogue.class);
    }
}
