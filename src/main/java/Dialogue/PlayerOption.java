package Dialogue;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerOption {
    @JsonProperty("id")
    public int id;                   // id опции

    @JsonProperty("text")
    public String text;                 // текст варианта, что видит игрок

    @JsonProperty("next_stage_id")
    public int nextStageId;          // куда ведёт выбор

    @JsonProperty("next_dialogue_id")
    public int nextDialogueId;       // переход в другой диалог (неюзабельно)

    @JsonProperty("conditions")
    public Map<String, Object> conditions; // условия видимости (ключи: "level", "flag", "item" и т.п.)

    @JsonProperty("effects")
    public Map<String, Object> effects;    // эффекты при выборе (ставит флаги, даёт предметы и т.п.)
}