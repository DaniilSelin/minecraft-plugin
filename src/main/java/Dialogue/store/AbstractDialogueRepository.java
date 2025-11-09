package dialogue.store;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.TextDisplay;

import dialogue.models.Dialogue;
import dialogue.models.DialogueLine;
import dialogue.models.DialogueStage;
import dialogue.models.PlayerOption;
import dialogue.store.session.ConversationSession;

public abstract class AbstractDialogueRepository {
    protected final Map<String, Dialogue> dialogues = new ConcurrentHashMap<>();
    protected final Map<String, ConversationSession> activeSessions = new ConcurrentHashMap<>();

    public abstract void loadDialogue();
    // public abstract void saveDialogue(Dialogue dialogue);
    protected abstract DialogueStage getStage(String sessionKey);
    public abstract DialogueLine getCurrentLine(String sessionKey);
    public String getCurrentLineText(String sessionKey) {
        DialogueLine line = getCurrentLine(sessionKey);
        return (line == null || line.text == null) ? "" : line.text;
    }
    protected abstract Dialogue findDialogueIdByNpcName(String npcName);

    public boolean checkSession(String sessionKey) { return activeSessions.containsKey(sessionKey); }
    protected ConversationSession getSession(String sessionKey) { return activeSessions.get(sessionKey); }
    public void endSession(String sessionKey) { activeSessions.remove(sessionKey); }

    public ConversationSession createSession(String sessionKey, Dialogue d, String npcName) {
        if (d == null) return null;
        ConversationSession s = new ConversationSession(d, npcName);
        activeSessions.put(sessionKey, s);
        return s;
    }

    // --- хранилище UI-объектов: менеджер создаёт TextDisplay, репозиторий хранит ссылку ---
    public List<TextDisplay> getHologramLines(String sessionKey) {
        ConversationSession s = getSession(sessionKey);
        return s == null ? null : s.getHologramLines();
    }

    public void setHologramLines(String sessionKey, List<TextDisplay> lines) {
        ConversationSession s = getSession(sessionKey);
        if (s != null) s.setHologramLines(lines);
    }

    public String getLastDisplayedText(String sessionKey) {
        ConversationSession s = getSession(sessionKey);
        return s == null ? null : s.getLastDisplayedText();
    }

    public void setLastDisplayedText(String sessionKey, String text) {
        ConversationSession s = getSession(sessionKey);
        if (s != null) s.setLastDisplayedText(text);
    }

    public ConversationSession ensureSessionForNpc(String sessionKey, String npcName) {
        if (checkSession(sessionKey)) return getSession(sessionKey);
        Dialogue dialogue = findDialogueIdByNpcName(npcName);
        if (dialogue == null) return null;
        return createSession(sessionKey, dialogue, npcName);
    }

    public List<PlayerOption> getOptionsWithContinue(String sessionKey) {
        DialogueLine line = getCurrentLine(sessionKey);
        List<PlayerOption> out = new ArrayList<>();
        if (line != null && line.options != null) out.addAll(line.options);

        PlayerOption cont = new PlayerOption();
        cont.id = -1;
        cont.text = "...";
        DialogueStage st = getStage(sessionKey);
        cont.nextStageId = (st == null) ? -1 : st.id;
        out.add(cont);
        return out;
    }

    public boolean acceptOption(String sessionKey, int optionIndex) {
        List<PlayerOption> opts = getOptionsWithContinue(sessionKey);
        if (opts == null || optionIndex < 0 || optionIndex >= opts.size()) {
            return true; // сигнализируем менеджеру, что разговор завершён / некорректный выбор
        }
        PlayerOption chosen = opts.get(optionIndex);
        if (chosen.id == -1) {
            // "..." -> завершаем разговор (менеджер сам вызовет endSession и удалит UI)
            return true;
        } else {
            ConversationSession s = getSession(sessionKey);
            if (s == null) return true;
            s.setStage(chosen.nextStageId);
            return false;
        }
    }

}