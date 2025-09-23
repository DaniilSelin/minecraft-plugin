package Dialogue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DialogueStage {
    @JsonProperty("id")
    public String id;                   // id этапа

    @JsonProperty("lines")
    public List<DialogueLine> lines;    // реплики NPC в этом этапе (последовательность)

    @JsonProperty("meta")
    public Map<String, Object> meta;    // произвольные метаданные (music, emotion и т.п.)
}