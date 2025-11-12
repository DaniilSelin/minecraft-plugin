package cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import npc.NpcManager;

public class RespawnNpcCommand implements CommandExecutor {
    private final NpcManager npcManager;

    public RespawnNpcCommand(NpcManager npcManager) {
        this.npcManager = npcManager;
    }

    // /respawnnpc <id> <x> <y> <z> [force]
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("Usage: /respawnnpc <id> <x> <y> <z> [force]");
            return true;
        }
        try {
            int id = Integer.parseInt(args[0]);
            double x = Double.parseDouble(args[1]);
            double y = Double.parseDouble(args[2]);
            double z = Double.parseDouble(args[3]);
            boolean force = args.length > 4 && ("true".equalsIgnoreCase(args[4]) || "force".equalsIgnoreCase(args[4]));

            Location loc = new Location(p.getWorld(), x, y, z);
            Entity e = npcManager.respawnNpcAt(id, loc, force);
            if (e == null) {
                p.sendMessage("§cFailed to respawn npc id=" + id);
            } else {
                p.sendMessage("§aNpc respawned: id=" + id + " uuid=" + e.getUniqueId());
            }
        } catch (NumberFormatException nfe) {
            sender.sendMessage("Bad number format: " + nfe.getMessage());
        }
        return true;
    }
}
