package npc;

import dialogue.manage.ConversationManager;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.entity.Player;

public class DialogBehavior extends Trait implements NPCBehavior {
    private String name;
    private ConversationManager convManager;

    public DialogBehavior(ConversationManager convManager, String pathDialogue, String name) {
        super("ivanbehavior");
        this.name = name;
        this.convManager = convManager;
    }

    @Override
    public void onRightClick(Player player) {
        convManager.beginConversation(player, name);
    }
}