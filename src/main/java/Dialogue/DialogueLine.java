package Dialogue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DialogueLine {
    @JsonProperty("id")
    public int id;                   // id реплики внутри этапа (опционально)

    @JsonProperty("speaker")
    public String speaker;              // кто говорит (имя).

    @JsonProperty("text")
    public String text;                 // текст реплики

    @JsonProperty("options")
    public List<PlayerOption> options;  // варианты игрока (могут быть пустыми — просто монолог)
}
