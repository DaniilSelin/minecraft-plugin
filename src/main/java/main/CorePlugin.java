package main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import load.impl.LoadDialogue;
import load.impl.LoadNpcConfig;
import cmd.LoadDialogsCommand;
import dialogue.manage.ConversationManager;
import dialogue.store.DialogueRepository;
import listeners.api.ISteerVehicleHandler;
import listeners.protocol.SteerVehicleManager;
import npc.NpcManager;
import npc.trait.ManagerRegistry;

public class CorePlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getLogger().info("Плагин включен!");
        getServer().getPluginManager().registerEvents(this, this);

        File data = getDataFolder();
        if (!data.exists()) data.mkdirs();

        File dialogsDir = new File(data, "dialogs");
        if (!dialogsDir.exists()) dialogsDir.mkdirs();

        File dialogFile = new File(dialogsDir, "dialogue.json");

        LoadDialogue load = new LoadDialogue();

        DialogueRepository repo = new DialogueRepository(dialogFile.getPath(), load);
        repo.loadDialogue();
        ConversationManager dialogManager = new ConversationManager(this, repo);

        // conversationManager реализует ITraitManager
        ManagerRegistry.register(dialogManager);

        getServer().getPluginManager().registerEvents(dialogManager, this);

        LoadDialogsCommand loadCommand = new LoadDialogsCommand(repo);

        getCommand("loaddialogs").setExecutor(loadCommand);

        // регистрируем слушателей пакетов
        List<ISteerVehicleHandler> handlers = new ArrayList<>();
        handlers.add(dialogManager);
        // handlers.add(otherHandler);

        SteerVehicleManager steerVehicleManager = new SteerVehicleManager(
                msg -> getLogger().info(msg),  // info логгер
                msg -> getLogger().severe(msg) // error логгер
        );

        steerVehicleManager.registerSteerVehicleListener(this, handlers);

        LoadNpcConfig loadNPC = new LoadNpcConfig();
        File npcFile = new File(data, "config.yaml");
        NpcManager npcManager = new NpcManager(this, npcFile.getPath(), loadNPC); 
        getServer().getPluginManager().registerEvents(npcManager, this);

        npcManager.loadConfigs();
        npcManager.applyAll(true);
    }

    @Override
    public void onDisable() {
        getLogger().info("Плагин выключен!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.sendTitle(
                "§4Ты залетел §cна сервер!",
                "§eтеперь ты... §kлох",
                10, 70, 20
        );
    }
}