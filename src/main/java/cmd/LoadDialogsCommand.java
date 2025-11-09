package cmd;

import dialogue.store.AbstractDialogueRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LoadDialogsCommand implements CommandExecutor {
    private final AbstractDialogueRepository repo;

    public LoadDialogsCommand(AbstractDialogueRepository repo) {
        this.repo = repo;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            repo.loadDialogue();
        } catch (Exception e) {
            sender.sendMessage("§cОшибка при загрузке: " + e.getMessage());
        }
        
        sender.sendMessage("§aДиалоги перезагружены:");
        return true;
    }
}
