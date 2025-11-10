package npc.trait;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;;

public interface ITraitManager {
    void begin(Player player, String field, Entity e);
    public abstract String getName();
}
