package cmd;

import npc.NpcManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LoadNPCConfigCommand implements CommandExecutor {
    private final NpcManager manage;

    public LoadNPCConfigCommand(NpcManager manage) {
        this.manage = manage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            manage.loadConfigs();
            manage.applyAll(true);
        } catch (Exception e) {
            sender.sendMessage("§cОшибка при загрузке: " + e.getMessage());
        }
        
        sender.sendMessage("§aДиалоги перезагружены:");
        return true;
    }
}
