package npc.behavior;

import npc.models.TraitConfig;
import npc.trait.ITraitManager;
import npc.trait.Trait;

import org.bukkit.entity.Player;

import npc.trait.ManagerAware;

public class DialogBehavior extends Trait implements NPCBehavior, ManagerAware {
    private final String name = "DialogBehavior";
    private final String reqManager = "ConversationManager";
    private ITraitManager convManager;
    private final String npcName;

    public DialogBehavior(TraitConfig config, String npcName) {
        super(config);
        this.npcName = npcName;
    }

    @Override
    public void setManager(ITraitManager manager) {
        this.convManager = manager;
    }

    @Override
    public void onInteract(Player player) {
        if (convManager == null || npcName == "") return;
        convManager.begin(player, npcName);
    }

    @Override
    public void onRightClick(Player player) {
        onInteract(player);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getReqManager() {
        return reqManager;
    }
}

