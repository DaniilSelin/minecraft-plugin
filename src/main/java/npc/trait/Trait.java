package npc.trait;

import npc.models.TraitConfig;
import org.bukkit.entity.Player;

public abstract class Trait {
    protected final TraitConfig config;

    public Trait(TraitConfig config) {
        this.config = config;
    }

    public void onInteract(Player player) {}

    public abstract String getName();
}
