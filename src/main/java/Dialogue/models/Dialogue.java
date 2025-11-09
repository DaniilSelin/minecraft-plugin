package dialogue.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dialogue {
    @JsonProperty("id")
    public int id;                   // id диалога

    @JsonProperty("title")
    public String title;                // название

    @JsonProperty("stages")
    public List<DialogueStage> stages;  // все этапы (стадии)
}