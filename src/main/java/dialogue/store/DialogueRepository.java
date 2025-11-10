package dialogue.store;

import dialogue.models.Dialogue;
import dialogue.models.DialogueLine;
import dialogue.models.DialogueStage;
import dialogue.models.PlayerOption;
import dialogue.store.session.ChoiceSession;
import dialogue.store.session.ConversationSession;

import org.bukkit.entity.Player;

import load.ILoad;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class DialogueRepository extends AbstractDialogueRepository {
    private final String basePath;
    private final ILoad<dialogue.models.Dialogue> loader;

    // TODO: ПОСЛЕ РЕАЛИЗАЦИИ НПС НОРМВАЛЬНО СДЕЛАТЬ БЯЛТЬ
    // protected Map<String, Dialogue> dialogues = new ConcurrentHashMap<>();
    private Dialogue dialogues;    

    // активные выборы (временный, можно хранить UUID вместо Player)
    private final Map<UUID, ChoiceSession> activeChoices = new ConcurrentHashMap<>();

    public DialogueRepository(String basePath, ILoad loader) {
        this.basePath = basePath;
        this.loader = loader;
    }

    @Override
    public void loadDialogue() {
        try {
            // TODO: ПОСЛЕ РЕАЛИЗАЦИИ НПС НОРМВАЛЬНО СДЕЛАТЬ БЯЛТЬ
            dialogues = loader.load(basePath);
        } catch (IOException e) {
            System.err.println("[DialogueRepository] failed to load dialogue :" + e.getMessage());
        }
    }

    @Override
    protected Dialogue findDialogueIdByNpcName(String npcName) {
        // TODO:
        // return dialogues.get(npcName);
        return this.dialogues;
    }

    @Override
    protected DialogueStage getStage(String sessionKey) {
        ConversationSession s = getSession(sessionKey);
        return s == null ? null : s.getCurrentStage();
    }

    @Override
    public DialogueLine getCurrentLine(String sessionKey) {
        ConversationSession s = getSession(sessionKey);
        return s == null ? null : s.getCurrentLine();
    }

    public void startChoiceForPlayer(Player player, List<PlayerOption> options, java.util.function.Consumer<Integer> onSelect) {
        if (player == null) return;
        ChoiceSession cs = new ChoiceSession(options, onSelect);
        activeChoices.put(player.getUniqueId(), cs);
    }

    public ChoiceSession getChoiceForPlayer(Player player) {
        if (player == null) return null;
        return activeChoices.get(player.getUniqueId());
    }

    public void endChoiceForPlayer(Player player) {
        if (player == null) return;
        activeChoices.remove(player.getUniqueId());
    }

    public ConversationSession createSessionForNpc(String sessionKey, String npcName) {
        Dialogue dialogue = findDialogueIdByNpcName(npcName);
        if (dialogue == null) return null;
        return createSession(sessionKey, dialogue, npcName);
    }
}
