package npc.models;

import java.util.List;

import org.bukkit.entity.EntityType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NpcConfig {
    @JsonProperty("id")
    public int id;
    @JsonProperty("name")
    public String name;  
    @JsonProperty("type")
    public EntityType type;
    @JsonProperty("location")
    public LocationConfig location;
    @JsonProperty("traits")
    public List<TraitConfig> traits;

    @Override
    public String toString() {
        return "NpcConfig{" + "id=" + id + ", name='" + name + '\'' + ", type=" + type + ", location=" + location + '}';
    }
}

