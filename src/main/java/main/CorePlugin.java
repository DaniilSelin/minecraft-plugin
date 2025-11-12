package main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import load.impl.LoadDialogues;
import load.impl.LoadNpcConfig;
import cmd.LoadCfgConvCommand;
import cmd.LoadDialogsCommand;
import cmd.RespawnNpcCommand;
import cmd.LoadNPCConfigCommand;
import dialogue.manage.ConversationManager;
import dialogue.store.DialogueRepository;
import listeners.api.ISteerVehicleHandler;
import listeners.protocol.SteerVehicleManager;
import load.impl.LoadCfgConv;
import npc.NpcManager;
import npc.behavior.DialogBehavior;
import npc.trait.ManagerRegistry;
import npc.trait.TraitRegistry;

public class CorePlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getLogger().info("Плагин включен!");
        getServer().getPluginManager().registerEvents(this, this);

        File data = getDataFolder();
        if (!data.exists()) data.mkdirs();

        File dialogsDir = new File(data, "dialogs");
        if (!dialogsDir.exists()) dialogsDir.mkdirs();

        LoadDialogues loadDialogs = new LoadDialogues();

        DialogueRepository repo = new DialogueRepository(dialogsDir.getPath(), loadDialogs);
        repo.loadDialogues();

        LoadCfgConv loadCfgUI = new LoadCfgConv();
        ConversationManager dialogManager = new ConversationManager(this, repo, loadCfgUI);

        File uiCfgFile = new File(data, "cfgUI.yaml");
        dialogManager.loadCfg(uiCfgFile.getPath());

        // conversationManager реализует ITraitManager
        ManagerRegistry.register(dialogManager);

        getServer().getPluginManager().registerEvents(dialogManager, this);

        // регистрируем слушателей пакетов
        List<ISteerVehicleHandler> handlers = new ArrayList<>();
        handlers.add(dialogManager);
        // handlers.add(otherHandler);

        SteerVehicleManager steerVehicleManager = new SteerVehicleManager(
                msg -> getLogger().info(msg),  // info логгер
                msg -> getLogger().severe(msg) // error логгер
        );

        steerVehicleManager.registerSteerVehicleListener(this, handlers);

        TraitRegistry.register("DialogBehavior", (entity, cfg) -> new DialogBehavior(cfg, entity, entity.getCustomName()));
        TraitRegistry.register("dialogbehavior", (entity, cfg) -> new DialogBehavior(cfg, entity, entity.getCustomName())); // дубль — на всякий

        LoadNpcConfig loadNPC = new LoadNpcConfig();
        File npcFile = new File(data, "config.yaml");
        NpcManager npcManager = new NpcManager(this, npcFile.getPath(), loadNPC); 
        getServer().getPluginManager().registerEvents(npcManager, this);

        npcManager.loadConfigs();
        npcManager.applyAll(true);
        
        LoadDialogsCommand loadCommand = new LoadDialogsCommand(repo);
        getCommand("loaddialogs").setExecutor(loadCommand);

        LoadCfgConvCommand loadCfgUICommand = new LoadCfgConvCommand(dialogManager, uiCfgFile.getPath());
        getCommand("loadUI").setExecutor(loadCfgUICommand);

        LoadNPCConfigCommand loadCfgNPC = new LoadNPCConfigCommand(npcManager);
        getCommand("loadCfgNPC").setExecutor(loadCfgNPC);

        RespawnNpcCommand respawnCmd = new RespawnNpcCommand(npcManager);
        getCommand("respnnpc").setExecutor(respawnCmd);
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