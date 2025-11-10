package npc.trait;

import org.bukkit.entity.Player;

public interface ITraitManager {
    void begin(Player player, String field);
    public abstract String getName();
}
