package dialogue.store.session;

import java.util.List;
import org.bukkit.entity.TextDisplay;

import dialogue.models.Dialogue;
import dialogue.models.DialogueLine;
import dialogue.models.DialogueStage;

public class ConversationSession {
    private final Dialogue dialogue;
    private final String npcName;
    private List<TextDisplay> hologramLines;

    public List<TextDisplay> getHologramLines() { return hologramLines; }
    public void setHologramLines(List<TextDisplay> lines) { this.hologramLines = lines; }

    private String lastDisplayedText = null;

    public String getLastDisplayedText() {
        return lastDisplayedText;
    }

    public void setLastDisplayedText(String text) {
        this.lastDisplayedText = text;
    }

    private int currentStageId;
    private int currentLineId;

    public ConversationSession(Dialogue dialogue, String npcName) {
        this.dialogue = dialogue;
        this.npcName = npcName;

        this.currentLineId = 0;
    }

    public String getNpcName() {
        return npcName;
    }

    public DialogueStage getCurrentStage() {
        if (dialogue.stages == null) return null;
        for (DialogueStage st : dialogue.stages) {
            if (st.id == currentStageId) return st;
        }
        return null;
    }

    public DialogueLine getCurrentLine() {
        DialogueStage st = getCurrentStage();
        if (st == null || st.lines == null) return null;
        for (DialogueLine ln : st.lines) {
            if (ln.id == currentLineId) return ln;
        }
        return null;
    }

    public void setStage(int nextStageId) {
        this.currentStageId = nextStageId;
        this.currentLineId = 0;
    }
}