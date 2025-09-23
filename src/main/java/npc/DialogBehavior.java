package npc;

import Dialogue.Manage.DialogManager;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.entity.Player;
import java.io.IOException;

import Dialogue.Dialogue;
import Dialogue.Manage.DeserializeDialogue;

public class DialogBehavior extends Trait implements NPCBehavior {
    private String name;
    private DialogManager dialogManager;
    private Dialogue dialogue;

    public DialogBehavior(DialogManager dialogManager, String pathDialogue, String name) {
        super("ivanbehavior");
        this.name = name;
        this.dialogManager = dialogManager;
        Dialogue d = null;
        try {
            d = DeserializeDialogue.deserialize(pathDialogue);
        } catch (IOException e) {
            // логируем и оставляем dialogue = null
            e.printStackTrace();
        }
        this.dialogue = d;
    }

    @Override
    public void onRightClick(Player player) {
        if (dialogue == null) {
            player.sendMessage("§cОшибка: диалог не загружен.");
            return;
        }
        dialogManager.beginConversation(player, name, dialogue);
    }
}