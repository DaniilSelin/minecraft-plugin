package main;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;

import java.util.ArrayList;
import java.util.List;
import java.io.File;

import listeners.api.ISteerVehicleHandler;
import listeners.protocol.SteerVehicleManager;

import npc.DialogBehavior;
import Dialogue.Manage.DialogManager;
import npc.NPCBehavior;

public class CorePlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getLogger().info("Плагин включен!");
        getServer().getPluginManager().registerEvents(this, this);

        DialogManager dialogManager = new DialogManager(this);
        getServer().getPluginManager().registerEvents(dialogManager, this); // для player.Quoit ивента

        // регистрируем слушателей пакетов
        List<ISteerVehicleHandler> handlers = new ArrayList<>();
        handlers.add(dialogManager);
        // handlers.add(otherHandler);

        SteerVehicleManager steerVehicleManager = new SteerVehicleManager(
                msg -> getLogger().info(msg),  // info логгер
                msg -> getLogger().severe(msg) // error логгер
        );

        steerVehicleManager.registerSteerVehicleListener(this, handlers);

        File data = getDataFolder();
        if (!data.exists()) data.mkdirs();

        File dialogsDir = new File(data, "dialogs");
        if (!dialogsDir.exists()) dialogsDir.mkdirs();

        File dialogFile = new File(dialogsDir, "dialogue.json");

        // Создаём нового NPC
        String name = "Ivan";
        NPC ivan = CitizensAPI.getNPCRegistry().createNPC(EntityType.VILLAGER, name);
        DialogBehavior dialogueTrait = new DialogBehavior(dialogManager, dialogFile.getPath(), name);

        // Добавляем trait к NPC
        ivan.addTrait(dialogueTrait);

        Location spawnLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
        ivan.spawn(spawnLocation);
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

        getLogger().info("Приветствие выведено " + player.getName());
    }

    @EventHandler
    public void onNPCRightClick(NPCRightClickEvent event) {
        NPC clicked = event.getNPC();
        if (clicked == null) return;

        getLogger().info("игрок взаимодействует с НПС");

        for (Trait trait : clicked.getTraits()) {
            if (trait instanceof NPCBehavior behavior) {
                behavior.onRightClick(event.getClicker());
            }
        }
    }
}