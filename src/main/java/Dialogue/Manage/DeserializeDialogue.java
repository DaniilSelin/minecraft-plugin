package Dialogue.Manage;

import Dialogue.Dialogue;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class DeserializeDialogue {

    public static Dialogue deserialize(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(path), Dialogue.class);
    }
}