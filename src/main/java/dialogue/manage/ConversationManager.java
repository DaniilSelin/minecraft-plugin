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

    // active choice UI per player
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
        String sessionKey = npcName + "_" + player.getUniqueId().toString();
        activeEntities.put(sessionKey, entity);

        // создаём сессию если её нет
        ConversationSession session = repo.ensureSessionForNpc(sessionKey, npcName);
        if (session == null) {
            plugin.getLogger().info("No dialogue for NPC: " + npcName + " (sessionKey=" + sessionKey + ")");
            player.sendMessage("§cЭтот NPC пока не разговаривает.");
            activeEntities.remove(sessionKey);
            return;
        }

        // стартуем таск, чтобы NPC смотрел на игрока
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
                // показали новую строчку после смены стадии
                Bukkit.getScheduler().runTask(plugin, () -> {
                    showNPCReplice(sessionKey, player);
                    showPlayerOption(sessionKey, player);
                });
            }
        });
    }

    public void showNPCReplice(String sessionKey, Player player) {
        // строка теперь берётся через repo
        if (!repo.checkSession(sessionKey)) return;
        String text = repo.getCurrentLineText(sessionKey);
        if (text == null) return;

        // Entity npc = activeEntities.get(sessionKey);
        // if (npc == null || npc.isDead()) return;
        // Location npcLoc = npc.getLocation();

        org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().normalize();
        double forward = cfg.hologram.forward;
        double height = cfg.hologram.height;
        Location baseLoc = player.getLocation().clone().add(dir.multiply(forward)).add(0, height, 0);

        // очистить старые дисплеи (репозиторий отдаёт их)
        List<TextDisplay> old = repo.getHologramLines(sessionKey);
        if (old != null) {
            removeHologramLines(this.plugin, old);
            repo.setHologramLines(sessionKey, null);
        }

        List<String> splitLines = SplitNPCReplice(text.isEmpty() ? "TEST DISPLAY" : text);
        List<String> allLines = FromateNPCReplice(splitLines);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                List<TextDisplay> newLines = CreateHologramLines(allLines, player, baseLoc);
                // репозиторий теперь хранит ссылки на созданные дисплеи
                repo.setHologramLines(sessionKey, newLines);
                repo.setLastDisplayedText(sessionKey, String.join("\n", allLines));
            } catch (Throwable t) {
                plugin.getLogger().severe("Failed to create TextDisplays: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    // --- выбор/UI ---
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

    // утилита: принимает hex (#rrggbb) или возвращает переданную секвенцию, в противном случае fallback
    private String toMinecraftColor(String hexOr, String fallback) {
        if (hexOr == null) return fallback;
        String s = hexOr.trim();
        if (s.startsWith("#") && s.length() == 7) {
            // Bukkit/MC не имеет стандартного парсинга в §x везде — возвращаем специальную секвенцию §x§R§R... если нужно.
            // Простая реализация: попробуем вернуть §x формат
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

    public List<TextDisplay> CreateHologramLines(List<String> alllines, Player player, Location npcLocation) {
        List<TextDisplay> newLines = new ArrayList<>();
        double yOffset = 0.0;

        ConversationConfig.HologramConfig hc = (this.cfg == null) ? new ConversationConfig.HologramConfig() : this.cfg.hologram;
        double spacing = hc.lineSpacing;
        int lineWidth = hc.maxWidth;
        String color = hc.color;
        boolean shadow = hc.shadow;

        final double forwardPush = hc.forwardPush;

        for (String lineText : alllines) {
            // направление от NPC к глазам игрока
            org.bukkit.util.Vector toPlayer = player.getEyeLocation().toVector().subtract(npcLocation.toVector());
            if (toPlayer.lengthSquared() == 0) toPlayer = player.getLocation().getDirection();
            toPlayer.normalize();

            // позиция: немного вперед от NPC в сторону игрока + вертикальный оффсет
            Location loc = npcLocation.clone()
                    .add(toPlayer.multiply(forwardPush))            // смещаем вперед в сторону игрока
                    .add(0.0, hc.height - yOffset, 0.0);             // поднимаем над головой и учитываем yOffset

            TextDisplay td = null;
            try {
                td = player.getWorld().spawn(loc, TextDisplay.class, display -> {
                    // текст (строка)
                    try { display.setText(lineText); } catch (Throwable ignored) {}

                    // ориентация к игроку
                    try { display.setBillboard(Display.Billboard.CENTER); } catch (Throwable ignored) {}

                    // опции визуала — используем напрямую значения из cfg
                    try { display.setLineWidth(lineWidth); } catch (Throwable ignored) {}
                    try { display.setShadowed(shadow); } catch (Throwable ignored) {}
                    try { display.setTextOpacity((byte)255); } catch (Throwable ignored) {}
                    display.setSeeThrough(true);
                    if (hc.setBillboard) {
                        display.setBillboard(Display.Billboard.CENTER);
                    } 
                    try { display.setGlowing(true); } catch (Throwable ignored) {}
                    try { display.setPersistent(true); } catch (Throwable ignored) {}
                    try { display.setGravity(false); } catch (Throwable ignored) {}

                    // подстраховка: если есть метод для установки цвета/компонента — не критично
                    // (оставляем как есть, чтобы не ломаться на разных версиях)
                });
            } catch (Throwable t) {
                plugin.getLogger().warning("Spawn TextDisplay failed for loc " + loc + ": " + t.getMessage());
            }

            if (td != null) newLines.add(td);
            yOffset += spacing;
        }

        return newLines;
    }

    private void removeHologramLines(JavaPlugin plugin, List<TextDisplay> lines) {
        if (lines == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (TextDisplay td : lines) {
                if (td != null && !td.isDead()) {
                    td.remove();
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
            // Подтверждаем выбор: сохраняем индекс и сразу удаляем сессию выбора
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

    // Если игрок вышел из игры – отменяем активный выбор
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        activeChoices.remove(player);
    }
}

