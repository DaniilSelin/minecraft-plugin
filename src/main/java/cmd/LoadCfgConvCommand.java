package cmd;

import dialogue.manage.ConversationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LoadCfgConvCommand implements CommandExecutor {
    private final ConversationManager conv;
    private String path;

    public LoadCfgConvCommand(ConversationManager conv, String path) {
        this.conv = conv;
        this.path = path;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            // не требуется, но задел на будущее
            conv.loadCfg(path);
        } catch (Exception e) {
            sender.sendMessage("§cОшибка при загрузке: " + e.getMessage());
        }
        
        sender.sendMessage("§aДиалоги перезагружены:");
        return true;
    }
}
