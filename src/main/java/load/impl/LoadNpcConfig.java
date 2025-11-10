package load.impl;

import load.ILoad;
import npc.models.NpcsRoot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class LoadNpcConfig implements ILoad {
    @Override
    public NpcsRoot load(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(path), NpcsRoot.class);
    }
}