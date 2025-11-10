package load.impl;

import load.ILoad;
import dialogue.models.ConversationConfig;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public class LoadCfgConv implements ILoad {
    @Override
    public ConversationConfig load(String path) throws IOException {
        try (Reader r = Files.newBufferedReader(Path.of(path))) {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(r);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(loaded, ConversationConfig.class);
        }
    }
}
