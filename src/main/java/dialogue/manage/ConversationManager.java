package dialogue.manage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import load.ILoad;
import dialogue.models.PlayerOption;
import dialogue.store.AbstractDialogueRepository;
import dialogue.store.session.ChoiceSession;
import dialogue.store.session.ConversationSession;
import dialogue.models.ConversationConfig;
import npc.trait.ITraitManager;
import listeners.api.ISteerVehicleHandler;

public class ConversationManager implements Listener, ISteerVehicleHandler, ITraitManager {
    private final JavaPlugin plugin;
    private final AbstractDialogueRepository repo;
    private final String name = "ConversationManager";
    private final ILoad loaderCfg;

    private ConversationConfig cfg;

    private final Map<String, Entity> activeEntities = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> lookTasks = new ConcurrentHashMap<>();

    private final Map<Player, ChoiceSession> activeChoices = new ConcurrentHashMap<>();

    public ConversationManager(JavaPlugin plugin, AbstractDialogueRepository repo, ILoad loaderCfg) {
        this.plugin = plugin;
        this.repo = repo;
        this.loaderCfg = loaderCfg;
    }

    public void loadCfg(String path) {
        try {
            this.cfg = (ConversationConfig) loaderCfg.load(path);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void begin(Player player, String npcName, Entity entity) {
        cleanupPlayerSessions(player);
        String sessionKey = npcName + "_" + player.getUniqueId().toString();
        activeEntities.put(sessionKey, entity);

        ConversationSession session = repo.ensureSessionForNpc(sessionKey, npcName);
        if (session == null) {
            plugin.getLogger().info("No dialogue for NPC: " + npcName + " (sessionKey=" + sessionKey + ")");
            player.sendMessage("§cЭтот NPC пока не разговаривает.");
            activeEntities.remove(sessionKey);
            return;
        }

        startLookTask(player, entity, sessionKey);

        showNPCReplice(sessionKey, player);
        showPlayerOption(sessionKey, player);
    }

    private void startLookTask(Player player, Entity entity, String sessionKey) {
        // пусто, не трогаем сущность, а то ерунда какая то получилась
        // BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
        //     if (entity == null || entity.isDead()) return;
        //     Location npcLoc = entity.getLocation();
        //     Location playerLoc = player.getLocation().clone().add(0, 1.5, 0); // чуть выше глаз
        //     npcLoc.setDirection(playerLoc.toVector().subtract(npcLoc.toVector()).normalize());
        //     entity.teleport(npcLoc);
        // }, 0L, 2L); // обновляем каждые 2 тика
        // lookTasks.put(sessionKey, task);
    }
    
    private void stopLookTask(String sessionKey) {
        BukkitTask task = lookTasks.remove(sessionKey);
        if (task != null) task.cancel();
        activeEntities.remove(sessionKey);
    }

    private void endConversation(Player player, String sessionKey) {
        activeChoices.remove(player);
        stopLookTask(sessionKey);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.2f);

        Bukkit.getScheduler().runTask(plugin, () -> {
            List<TextDisplay> old = repo.getHologramLines(sessionKey);
            if (old != null && !old.isEmpty()) {
                plugin.getLogger().info("Removing " + old.size() + " hologram(s) for " + sessionKey);
                for (TextDisplay td : old) {
                    try {
                        if (td != null && !td.isDead()) td.remove();
                    } catch (Throwable t) {
                        plugin.getLogger().warning("Failed to remove TextDisplay: " + t.getMessage());
                    }
                }
                repo.setHologramLines(sessionKey, null);
            }
            repo.endSession(sessionKey);

            player.sendActionBar("");
        });
    }

    private void showPlayerOption(String sessionKey, Player player) {
        List<PlayerOption> display = repo.getOptionsWithContinue(sessionKey);
        if (display == null) {
            endConversation(player, sessionKey);
            return;
        }

        startChoice(player, display, selectedIndex -> {
            boolean ended = repo.acceptOption(sessionKey, selectedIndex);
            if (ended) {
                Bukkit.getScheduler().runTask(plugin, () -> endConversation(player, sessionKey));
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    showNPCReplice(sessionKey, player);
                    showPlayerOption(sessionKey, player);
                });
            }
        });
    }

    public void showNPCReplice(String sessionKey, Player player) {
        removeHologramsSeenByPlayer(player);
        if (!repo.checkSession(sessionKey)) return;
        String text = repo.getCurrentLineText(sessionKey);
        if (text == null) return;

        Entity npc = activeEntities.get(sessionKey);
        Location baseLoc;
        if (npc != null && !npc.isDead()) {
            baseLoc = npc.getLocation().clone().add(0, (this.cfg == null ? new ConversationConfig().hologram.height : this.cfg.hologram.height), 0);
        } else {
            org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().normalize();
            double forward = (this.cfg == null ? new ConversationConfig().hologram.forward : this.cfg.hologram.forward);
            double height = (this.cfg == null ? new ConversationConfig().hologram.height : this.cfg.hologram.height);
            baseLoc = player.getLocation().clone().add(dir.multiply(forward)).add(0, height, 0);
        }

        List<TextDisplay> old = repo.getHologramLines(sessionKey);
        List<String> splitLines = SplitNPCReplice(text.isEmpty() ? "TEST DISPLAY" : text);
        List<String> allLines = FromateNPCReplice(splitLines);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (old != null && !old.isEmpty()) {
                    for (TextDisplay td : old) {
                        if (td == null || td.isDead()) continue;
                        try {
                            try { player.hideEntity(plugin, td); } catch (Throwable ignored) {}
                            td.remove();
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Failed to remove old TextDisplay: " + t.getMessage());
                        }
                    }
                }
                repo.setHologramLines(sessionKey, null);
                List<TextDisplay> newLines = CreateHologramLines(allLines, player, baseLoc);
                repo.setHologramLines(sessionKey, newLines);
                repo.setLastDisplayedText(sessionKey, String.join("\n", allLines));
                plugin.getLogger().info("Created holograms: " + (newLines == null ? 0 : newLines.size()) + " for " + sessionKey);

                if (newLines != null) {
                    for (TextDisplay td : newLines) {
                        try {
                            if (td == null || td.isDead()) continue;
                            player.showEntity(plugin, td);
                            plugin.getLogger().info("Shown textdisplay id=" + td.getUniqueId() + " to player=" + player.getName());
                        } catch (Throwable ex) {
                            plugin.getLogger().warning("player.showEntity failed id=" + (td == null ? "null" : td.getUniqueId()) + ": " + ex.getMessage());
                        }
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().severe("showNPCReplice failed: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    public List<TextDisplay> CreateHologramLines(List<String> alllines, Player player, Location npcLocation) {
        List<TextDisplay> newLines = new ArrayList<>();
        double yOffset = 0.0;

        ConversationConfig.HologramConfig hc = (this.cfg == null || this.cfg.hologram == null) ? new ConversationConfig.HologramConfig() : this.cfg.hologram;
        double spacing = hc.lineSpacing;
        int lineWidth = hc.maxWidth;
        boolean shadow = hc.shadow;

        for (String lineText : alllines) {
            org.bukkit.util.Vector dirPlayerToNpc = npcLocation.toVector().subtract(player.getEyeLocation().toVector());
            if (dirPlayerToNpc.lengthSquared() == 0) dirPlayerToNpc = player.getLocation().getDirection();
            dirPlayerToNpc = dirPlayerToNpc.normalize();

            double offsetTowardsPlayer = 0.35;
            double distance = Math.max(0.0, npcLocation.distance(player.getEyeLocation()));
            if (distance > 10.0) offsetTowardsPlayer = Math.min(1.0, distance * 0.06);

            Location loc = npcLocation.clone()
                    .add(dirPlayerToNpc.multiply(-offsetTowardsPlayer))
                    .add(0.0, hc.height - yOffset, 0.0);

            TextDisplay td = null;
            try {
                td = npcLocation.getWorld().spawn(loc, TextDisplay.class, display -> {
                    try { display.setText(lineText); } catch (Throwable ignored) {}
                    try { display.setBillboard(Display.Billboard.CENTER); } catch (Throwable ignored) {}
                    try { display.setLineWidth(lineWidth); } catch (Throwable ignored) {}
                    try { display.setShadowed(shadow); } catch (Throwable ignored) {}
                    try { display.setTextOpacity((byte)255); } catch (Throwable ignored) {}
                    try { display.setSeeThrough(true); } catch (Throwable ignored) {}
                    try { display.setGlowing(true); } catch (Throwable ignored) {}
                    try { display.setPersistent(true); } catch (Throwable ignored) {}
                    try { display.setGravity(false); } catch (Throwable ignored) {}
                    try { display.setVisibleByDefault(false); } catch (Throwable ignored) {} // скрываем по умолчанию
                });

                if (td != null) {
                    newLines.add(td);
                    plugin.getLogger().fine("Spawned TextDisplay id=" + td.getUniqueId() + " at " + loc);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Spawn TextDisplay failed for loc " + loc + ": " + t.getMessage());
            }

            yOffset += spacing;
        }

        return newLines;
    }

    private void startChoice(Player player, List<PlayerOption> options, Consumer<Integer> onSelect) {
        ChoiceSession choiceSession = new ChoiceSession(options, onSelect);
        activeChoices.put(player, choiceSession);
        sendChoiceMessage(player);
        // Блокируем движение игрока во время выбора
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
    }

    private void sendChoiceMessage(Player player) {
        ChoiceSession cs = activeChoices.get(player);
        if (cs == null) return;
        ConversationConfig conf = this.cfg == null ? new ConversationConfig() : this.cfg;

        String selColor = conf.options.selectedColor == null ? "§c" : toMinecraftColor(conf.options.selectedColor, "§c");
        String normalColor = "§7";
        String sep = " " + normalColor + "| ";

        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < cs.SizeOption(); i++) {
            if (i == cs.getCurrentIndex()) {
                msg.append(selColor);
            } else {
                msg.append(normalColor);
            }
            msg.append(conf.options.optionPrefix != null ? conf.options.optionPrefix : "");
            msg.append(cs.GetTextOp(i));
            msg.append(conf.options.optionSuffix != null ? conf.options.optionSuffix : "");
            if (i < cs.SizeOption() - 1) msg.append(sep);
        }
        player.sendActionBar(msg.toString());
    }

    private String toMinecraftColor(String hexOr, String fallback) {
        if (hexOr == null) return fallback;
        String s = hexOr.trim();
        if (s.startsWith("#") && s.length() == 7) {
            try {
                StringBuilder b = new StringBuilder("§x");
                for (int i = 1; i < 7; i++) b.append('§').append(s.charAt(i));
                return b.toString();
            } catch (Throwable ignored) {}
        }
        return fallback;
    }

    public List<String> SplitNPCReplice(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null) return lines;
        text = text.strip();
        if (text.isEmpty()) return lines;

        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (cur.length() == 0) {
                cur.append(w);
            } else {
                if (cur.length() + 1 + w.length() <= cfg.hologram.maxWidth) {
                    cur.append(' ').append(w);
                } else {
                    lines.add(cur.toString());
                    cur = new StringBuilder(w);
                }
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    public List<String> FromateNPCReplice(List<String> splitLines) {
        List<String> allLines = new ArrayList<>();
        for (String s : splitLines) {
            allLines.add("§a" + s);
        }
        return allLines;
    }

    private void cleanupPlayerSessions(Player player) {
        String suffix = "_" + player.getUniqueId().toString();
        List<String> toCleanup = new ArrayList<>();
        for (String key : new ArrayList<>(activeEntities.keySet())) {
            if (key.endsWith(suffix)) toCleanup.add(key);
        }

        for (String key : toCleanup) {
            BukkitTask t = lookTasks.remove(key);
            if (t != null) t.cancel();

            List<TextDisplay> old = repo.getHologramLines(key);
            if (old != null && !old.isEmpty()) {
                removeHologramLinesForPlayer(this.plugin, old, player);
                repo.setHologramLines(key, null);
                plugin.getLogger().info("Cleanup: removed " + old.size() + " holograms for session " + key);
            }

            repo.endSession(key);
            activeEntities.remove(key);
        }
        
        activeChoices.remove(player);
    }

    private void removeHologramLinesForPlayer(JavaPlugin plugin, List<TextDisplay> lines, Player player) {
        if (lines == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (TextDisplay td : lines) {
                if (td == null || td.isDead()) continue;
                try {
                    try { player.hideEntity(plugin, td); } catch (Throwable ignored) {}
                    td.remove();
                } catch (Throwable t) {
                    plugin.getLogger().warning("Failed to remove TextDisplay: " + t.getMessage());
                }
            }
        });
    }

    @Override
    public void handleSteerVehicle(Player player, boolean forward, boolean backward, boolean jump) {
        ChoiceSession cs = activeChoices.get(player);
        if (cs == null) return;
        if (forward) {
            cs.previous();
            sendChoiceMessage(player);
        } else if (backward) {
            cs.next();
            sendChoiceMessage(player);
        } else if (jump) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int sel = cs.getCurrentIndex();
                activeChoices.remove(player); // снимаем UI
                cs.Accept(sel);
            });
        }
    }

    @EventHandler
    public void onPlayerJump(PlayerJumpEvent e) {
        Player p = e.getPlayer();
        if (!activeChoices.containsKey(p)) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        activeChoices.remove(player);
    }

    // НЕ ТРОГАТЬ, НЕЙРО-ПОМОИ, которые я не желаю разьбирать, но которые работаю
    public void removeHologramsSeenByPlayer(Player target) {
        if (target == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String sessionKey : new ArrayList<>(activeEntities.keySet())) {
                List<TextDisplay> stored = repo.getHologramLines(sessionKey);
                if (stored == null || stored.isEmpty()) continue;

                List<TextDisplay> keep = new ArrayList<>();
                boolean repoChanged = false;

                for (TextDisplay td : stored) {
                    if (td == null || td.isDead()) continue;

                    if (!target.canSee(td)) {
                        keep.add(td);
                        continue;
                    }

                    boolean seenByOthers = false;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p == null || p.equals(target)) continue;
                        try {
                            if (p.canSee(td)) { seenByOthers = true; break; }
                        } catch (Throwable ignored) {}
                    }

                    if (seenByOthers) {
                        // нельзя удалить глобально — скрываем только для target
                        try { target.hideEntity(plugin, td); } catch (Throwable ignored) {}
                        keep.add(td);
                    } else {
                        // безопасно удалить глобально — сначала скрываем для target, потом remove()
                        try { target.hideEntity(plugin, td); } catch (Throwable ignored) {}
                        try {
                            td.remove();
                            repo.getClass();
                            plugin.getLogger().info("Removed TextDisplay id=" + td.getUniqueId() + " (seen only by " + target.getName() + ")");
                            repoChanged = true;
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Failed to remove TextDisplay id=" + (td == null ? "null" : td.getUniqueId()) + ": " + t.getMessage());
                            keep.add(td);
                        }
                    }
                }

                if (repoChanged) {
                    repo.setHologramLines(sessionKey, keep.isEmpty() ? null : keep);
                }
            }

            Location ploc = target.getLocation();
            try {
                for (Entity e : ploc.getWorld().getNearbyEntities(ploc, 8, 8, 8)) {
                    if (!(e instanceof TextDisplay)) continue;
                    TextDisplay td = (TextDisplay) e;
                    if (td.isDead()) continue;
                    if (!target.canSee(td)) continue;

                    boolean otherSee = false;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.equals(target)) continue;
                        if (p.canSee(td)) { otherSee = true; break; }
                    }

                    if (otherSee) {
                        try { target.hideEntity(plugin, td); } catch (Throwable ignored) {}
                    } else {
                        try { target.hideEntity(plugin, td); } catch (Throwable ignored) {}
                        try {
                            td.remove();
                            plugin.getLogger().info("Removed stray TextDisplay id=" + td.getUniqueId() + " near player " + target.getName());
                        } catch (Throwable t) {
                            plugin.getLogger().warning("Failed to remove stray TextDisplay: " + t.getMessage());
                        }
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Nearby-clean failed: " + t.getMessage());
            }
        });
    }
}

