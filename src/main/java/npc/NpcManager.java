package npc;

import npc.models.NpcConfig;
import npc.models.NpcsRoot;
import npc.trait.ITraitManager;
import npc.trait.ManagerAware;
import npc.trait.ManagerRegistry;
import npc.trait.Trait;
import npc.trait.TraitRegistry;
import load.ILoad;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class NpcManager implements Listener {
    private final JavaPlugin plugin;
    private final String cfgPath;
    private final ILoad<NpcsRoot> loader;

    // runtime: entity UUID -> attached traits
    private final Map<UUID, List<Trait>> attached = new java.util.concurrent.ConcurrentHashMap<>();

    // configs index: id -> config
    private final Map<Integer, NpcConfig> configs = new LinkedHashMap<>();

    public NpcManager(JavaPlugin plugin, String cfgPath, ILoad<NpcsRoot> loader) {
        this.plugin = plugin;
        this.cfgPath = cfgPath;
        this.loader = loader;
    }

    public void loadConfigs() {
        try {
            NpcsRoot root = loader.load(cfgPath);
            if (root == null || root.npcs == null) {
                plugin.getLogger().info("NpcManager: no npc configs in " + cfgPath);
                return;
            }
            configs.clear();
            for (NpcConfig c : root.npcs) {
                if (c == null) continue;
                configs.put(c.id, c);
            }
            plugin.getLogger().info("NpcManager: loaded " + configs.size() + " npc configs");
        } catch (Throwable t) {
            plugin.getLogger().severe("NpcManager: failed to load configs: " + t.getMessage());
        }
    }

    public void applyAll(boolean spawnIfMissing) {
        for (NpcConfig cfg : configs.values()) apply(cfg, spawnIfMissing);
    }

    /** Применить конкретный конфиг (спавн/поиск + прикрепление трейтов) */
    public void apply(NpcConfig cfg, boolean spawnIfMissing) {
        if (cfg == null) return;

        // Найти или создать сущность
        Entity entity = findEntityByName(cfg.name);
        if (entity == null && spawnIfMissing) {
            Location loc = cfg.location == null ? null : cfg.location.toBukkitLocation(plugin);
            if (loc == null) {
                plugin.getLogger().warning("NpcManager: cannot spawn '" + cfg.name + "' (world missing)");
                return;
            }
            entity = loc.getWorld().spawnEntity(loc, cfg.type == null ? EntityType.VILLAGER : cfg.type);
            if (entity instanceof org.bukkit.entity.LivingEntity le) {
                try { le.setCustomName(cfg.name); } catch (Throwable ignored) {}
                try { le.setCustomNameVisible(true); } catch (Throwable ignored) {}
                try { le.setInvulnerable(true); } catch (Throwable ignored) {}
            }
            plugin.getLogger().info("NpcManager: spawned entity for npc '" + cfg.name + "'");
        }

        if (entity == null) {
            plugin.getLogger().info("NpcManager: entity not found for '" + cfg.name + "', skipped");
            return;
        }

        // Создаём и регистрируем все трейты, основываясь на конфиге
        List<Trait> traitsList = new ArrayList<>();
        if (cfg.traits != null) {
            for (npc.models.TraitConfig tc : cfg.traits) {
                Trait t = TraitRegistry.create(tc.type, entity, tc);
                if (t == null) {
                    plugin.getLogger().warning("NpcManager: unknown trait '" + tc.type + "' for npc " + cfg.name);
                    continue;
                }

                // Если трейт требует менеджера, подставляем из ManagerRegistry
                if (t instanceof ManagerAware ma) {
                    ITraitManager mgr = ManagerRegistry.get(ma.getReqManager());
                    if (mgr != null) ma.setManager(mgr);
                    else plugin.getLogger().warning("NpcManager: required manager '" + ma.getReqManager() + "' not found for trait '" + tc.type + "'");
                }

                traitsList.add(t);
            }
        }

        // Сохраняем список трейтов для этой сущности
        attached.put(entity.getUniqueId(), traitsList);
    }

    /** Detach and call onDetach for all traits attached to entity */
    public void detachAll(Entity e) {
        List<Trait> list = attached.remove(e.getUniqueId());   // <-- Trait вместо NpcTrait
        if (list == null) return;
    }

    /** Найти сущность по точному customName во всех мирах */
    private Entity findEntityByName(String name) {
        if (name == null) return null;
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (name.equals(e.getCustomName())) return e;
            }
        }
        return null;
    }

    /** Делегирует событие клика трейтам этой сущности */
    public void handleInteract(Entity entity, org.bukkit.entity.Player player) {
        List<Trait> list = attached.get(entity.getUniqueId());    // <-- Trait
        if (list == null || list.isEmpty()) return;
        for (Trait t : list) {
            try { t.onInteract(player); } catch (Throwable ex) { plugin.getLogger().warning("Trait error: " + ex.getMessage()); }
        }
    }

    // Event handler: регистрировать NpcManager как Listener в плагине
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent ev) {
        Entity clicked = ev.getRightClicked();
        if (clicked == null) return;
        if (!attached.containsKey(clicked.getUniqueId())) return;
        handleInteract(clicked, ev.getPlayer());
        ev.setCancelled(true);
    }

    // респавн непися
    public Entity respawnNpcAt(int npcId, Location loc, boolean forceSpawn) {
        NpcConfig cfg = configs.get(npcId);
        if (cfg == null) {
            plugin.getLogger().warning("respawnNpcAt: no npc config for id=" + npcId);
            return null;
        }
        if (loc == null) {
            loc = (cfg.location == null) ? null : cfg.location.toBukkitLocation(plugin);
            if (loc == null) {
                plugin.getLogger().warning("respawnNpcAt: no location provided and config has no location for id=" + npcId);
                return null;
            }
        }

        Entity existing = findEntityByName(cfg.name);
        if (existing != null) {
            if (!forceSpawn) {
                plugin.getLogger().info("respawnNpcAt: entity already exists for id=" + npcId + ", skipping spawn");
                return existing;
            }
            detachAll(existing);
            try {
                if (!existing.isDead()) existing.remove();
            } catch (Throwable t) {
                plugin.getLogger().warning("respawnNpcAt: failed to remove existing entity: " + t.getMessage());
            }
        }

        Entity spawned;
        try {
            spawned = loc.getWorld().spawnEntity(loc, cfg.type == null ? EntityType.VILLAGER : cfg.type);
        } catch (Throwable t) {
            plugin.getLogger().severe("respawnNpcAt: spawn failed for id=" + npcId + ": " + t.getMessage());
            return null;
        }

        if (spawned instanceof org.bukkit.entity.LivingEntity le) {
            try { le.setCustomName(cfg.name); } catch (Throwable ignored) {}
            try { le.setCustomNameVisible(true); } catch (Throwable ignored) {}
            try { le.setInvulnerable(true); } catch (Throwable ignored) {}
        }

        List<Trait> traitsList = new ArrayList<>();
        if (cfg.traits != null) {
            for (npc.models.TraitConfig tc : cfg.traits) {
                Trait t = TraitRegistry.create(tc.type, spawned, tc);
                if (t == null) {
                    plugin.getLogger().warning("NpcManager: unknown trait '" + tc.type + "' for npc " + cfg.name);
                    continue;
                }
                if (t instanceof ManagerAware ma) {
                    ITraitManager mgr = ManagerRegistry.get(ma.getReqManager());
                    if (mgr != null) ma.setManager(mgr);
                    else plugin.getLogger().warning("NpcManager: required manager '" + ma.getReqManager() + "' not found for trait '" + tc.type + "'");
                }
                traitsList.add(t);
            }
        }

        attached.put(spawned.getUniqueId(), traitsList);
        plugin.getLogger().info("respawnNpcAt: spawned npc id=" + npcId + " name=" + cfg.name + " uuid=" + spawned.getUniqueId());
        return spawned;
    }
}
