package dialogue.manage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;

import dialogue.models.PlayerOption;
import dialogue.store.AbstractDialogueRepository;
import dialogue.store.session.ChoiceSession;
import dialogue.store.session.ConversationSession;
import npc.trait.ITraitManager;
import listeners.api.ISteerVehicleHandler;

public class ConversationManager implements Listener, ISteerVehicleHandler, ITraitManager {
    private final JavaPlugin plugin;
    private final AbstractDialogueRepository repo;
    private final String name = "ConversationManager";

    // active choice UI per player
    private final Map<Player, ChoiceSession> activeChoices = new ConcurrentHashMap<>();

    public ConversationManager(JavaPlugin plugin, AbstractDialogueRepository repo) {
        this.plugin = plugin;
        this.repo = repo;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void begin(Player player, String npcName) {
        String sessionKey = npcName + "_" + player.getUniqueId().toString();

        // создаём сессию если её нет
        ConversationSession session = repo.ensureSessionForNpc(sessionKey, npcName);
        if (session == null) {
            // лог и подконтрольное сообщение игроку — полезно при отладке
            plugin.getLogger().info("No dialogue for NPC: " + npcName + " (sessionKey=" + sessionKey + ")");
            player.sendMessage("§cЭтот NPC пока не разговаривает.");
            return;
        }

        showNPCReplice(sessionKey, player);
        showPlayerOption(sessionKey, player);
    }

    private void endConversation(Player player, String sessionKey) {
        // снимем UI и параметры игрока сразу (локально)
        activeChoices.remove(player);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        // выполняем удаление сущностей и очистку репозитория в одном sync-таске
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
            // гарантированно завершить сессию (удалить состояние)
            repo.endSession(sessionKey);

            // очистить actionbar
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

        org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().normalize();
        double forward = 3.0;
        double height = 1.5;
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

        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < cs.SizeOption(); i++) {
            if (i == cs.getCurrentIndex()) {
                msg.append("§c"); // выделяем красным текущую опцию
            } else {
                msg.append("§7");
            }
            msg.append(cs.GetTextOp(i));
            if (i < cs.SizeOption() - 1) {
                msg.append(" §7| "); // разделитель между опциями
            }
        }
        player.sendActionBar(msg.toString());
    }
    
    public List<String> SplitNPCReplice(String text) {
        List<String> splitLines = new ArrayList<>();
        int maxLen = 35;
        while (!text.isEmpty()) {
            int end = Math.min(maxLen, text.length());
            splitLines.add(text.substring(0, end));
            text = text.substring(end);
        }
        return splitLines;
    }

    public List<String> FromateNPCReplice(List<String> splitLines) {
        List<String> allLines = new ArrayList<>();
        for (String s : splitLines) {
            allLines.add("§a" + s);
        }
        return allLines;
    }

    public List<TextDisplay> CreateHologramLines(List<String> alllines, Player player, Location baseLoc) {
        List<TextDisplay> newLines = new ArrayList<>();
        double yOffset = 0.0;

        for (String lineText : alllines) {
            Location loc = baseLoc.clone().add(0, -yOffset, 0);

            TextDisplay td = null;
            try {
                td = player.getWorld().spawn(loc, TextDisplay.class, display -> {
                    display.setText(lineText);

                    // ориентация
                    try { display.setBillboard(Display.Billboard.CENTER); } catch (Throwable ignored) {}

                    // видимость и контраст
                    try { display.setViewRange(256.0f); } catch (Throwable ignored) {}
                    try { display.setLineWidth(200); } catch (Throwable ignored) {}
                    try { display.setSeeThrough(true); } catch (Throwable ignored) {}
                    try { display.setTextOpacity((byte)255); } catch (Throwable ignored) {}
                    try { display.setShadowed(true); } catch (Throwable ignored) {}
                    try { display.setGlowing(true); } catch (Throwable ignored) {}
                    try { display.setPersistent(true); } catch (Throwable ignored) {}
                    try { display.setGravity(false); } catch (Throwable ignored) {}
                });
            } catch (Throwable t) {
                plugin.getLogger().warning("Spawn TextDisplay failed for loc " + loc + ": " + t.getMessage());
            }

            if (td != null) newLines.add(td);
            yOffset += 0.25;
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

