package dialogue.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversationConfig {
    @JsonProperty("hologram")
    public HologramConfig hologram = new HologramConfig();

    @JsonProperty("options")
    public OptionUIConfig options = new OptionUIConfig();

    @JsonProperty("behavior")
    public BehaviorConfig behavior = new BehaviorConfig();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HologramConfig {
        @JsonProperty("forward")
        public double forward = 3.0;

        @JsonProperty("height")
        public double height = 1.5;

        @JsonProperty("lineSpacing")
        public double lineSpacing = 0.25;

        @JsonProperty("maxWidth")
        public int maxWidth = 40;

        @JsonProperty("fontSize")
        public int fontSize = 22;

        @JsonProperty("forwardPush")
        public double forwardPush = 0.45;

        @JsonProperty("setBillboard")
        public boolean setBillboard = true;

        @JsonProperty("color")
        public String color = "#FFFFFF";

        @JsonProperty("shadow")
        public boolean shadow = true;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OptionUIConfig {
        @JsonProperty("maxOptionsPerPage")
        public int maxOptionsPerPage = 6;

        @JsonProperty("optionPrefix")
        public String optionPrefix = "> ";

        @JsonProperty("optionSuffix")
        public String optionSuffix = "";

        @JsonProperty("selectedColor")
        public String selectedColor = "#FFFF00";

        @JsonProperty("optionSpacing")
        public int optionSpacing = 1;

        @JsonProperty("inputDelayMs")
        public int inputDelayMs = 0;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BehaviorConfig {
        @JsonProperty("autoCloseOnEnd")
        public boolean autoCloseOnEnd = true;

        @JsonProperty("hologramLifetimeTicks")
        public int hologramLifetimeTicks = 20 * 60;
    }

    @Override
    public String toString() {
        return "ConversationConfig{" +
                "hologram=" + hologram +
                ", options=" + options +
                ", behavior=" + behavior +
                '}';
    }
}
