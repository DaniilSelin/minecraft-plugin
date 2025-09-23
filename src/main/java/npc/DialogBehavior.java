package npc;

import Dialogue.Manage.DialogManager;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.entity.Player;
import main.CorePlugin;
import java.io.IOException;

import java.util.Arrays;

import Dialogue.Dialogue;
import Dialogue.Manage.DeserializeDialogue;
import Dialogue.Manage.DialogManager;

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
            // логируем и оставляем dialogue = null (или выбрасываем Runtime если хочешь фейлать на старте)
            e.printStackTrace();
            // можно: plugin.getLogger().severe("Can't load dialogue: " + pathDialogue);
        }
        this.dialogue = d;
    }

    @Override
    public void onRightClick(Player player) {
        if (dialogue == null) {
            player.sendMessage("§cОшибка: диалог не загружен.");
            return;
        }
        // Запускаем диалог при клике на NPC
        dialogManager.beginConversation(player, name, dialogue);
    }
}