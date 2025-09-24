package Dialogue.Manage.Session;

import Dialogue.Dialogue;
import Dialogue.DialogueStage;
import Dialogue.DialogueLine;

import java.util.List;
import org.bukkit.entity.ArmorStand;

public class ConversationSession {
    private final Dialogue dialogue;
    private final String npcName;
    private List<ArmorStand> hologramLines;

    public List<ArmorStand> getHologramLines() { return hologramLines; }
    public void setHologramLines(List<ArmorStand> lines) { this.hologramLines = lines; }

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