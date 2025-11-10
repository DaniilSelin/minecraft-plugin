package npc.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NpcsRoot {
    @JsonProperty("npcs")
    public List<NpcConfig> npcs;
    public Defaults defaults;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Defaults {
        @JsonProperty("spawn-on-enable")
        public boolean spawnOnEnable = false;
    }
}