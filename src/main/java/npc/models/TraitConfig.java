package npc.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraitConfig {
    @JsonProperty("type")
    public String type;

    @Override
    public String toString() {
        return "TraitConfig{" +
                "type='" + type + '}';
    }
}
