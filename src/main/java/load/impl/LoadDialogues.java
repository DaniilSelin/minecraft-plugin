package load.impl;

import load.ILoad;
import dialogue.models.Dialogue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoadDialogues implements ILoad {
    @Override
    public Map<String, Dialogue> load(String path) throws IOException {
        Map<String, Dialogue> out = new LinkedHashMap<>();
        if (path == null) return out;

        File root = new File(path);
        if (!root.exists()) return out;

        System.out.println("[LoadDialogues] Loading dialogues from: " + root.getAbsolutePath());
        loadFromDirectory(root, out);
        System.out.println("[LoadDialogues] Finished loading. Total dialogues: " + out.size());
        return out;
    }

    private void loadFromDirectory(File dir, Map<String, Dialogue> out) {
        if (dir == null || !dir.exists()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        ObjectMapper mapper = new ObjectMapper();

        for (File f : files) {
            if (f.isDirectory()) {
                loadFromDirectory(f, out);
            } else if (f.isFile()) {
                try {
                    Dialogue d = mapper.readValue(f, Dialogue.class);
                    if (d != null && d.npcName != null && !d.npcName.isEmpty()) {
                        out.put(d.npcName, d);
                        System.out.println("[LoadDialogues] Loaded dialogue: " + d.npcName + " from file: " + f.getAbsolutePath());
                    }
                } catch (Throwable t) {
                    System.err.println("[LoadDialogues] Failed to parse " + f.getAbsolutePath() + ": " + t.getMessage());
                }
            }
        }
    }
}
