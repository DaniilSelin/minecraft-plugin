package Dialogue.Manage;

import Dialogue.Dialogue;
import Dialogue.DialogueLine;
import Dialogue.DialogueStage;
import Dialogue.PlayerOption;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.TextDisplay;
import org.bukkit.Location;
import listeners.api.ISteerVehicleHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Display;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.Consumer;

import Dialogue.Manage.Session.ConversationSession;
import Dialogue.Manage.Session.ChoiceSession;

public class DialogManager implements Listener, ISteerVehicleHandler {

    // костыль(
    private final JavaPlugin plugin;
    // Сессии диалогов: ключ = npcName + "_" + playerUUID
    private final Map<String, ConversationSession> conversationSessions = new HashMap<>();
    // Активные выборы
    private final Map<Player, ChoiceSession> activeChoices = new HashMap<>();

    public DialogManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void beginConversation(Player player, String npcName, Dialogue dialogue) {
        String sessionKey = npcName + "_" + player.getUniqueId().toString();
        ConversationSession session = conversationSessions.get(sessionKey);
        if (session == null) {
            session = new ConversationSession(dialogue, npcName);
            conversationSessions.put(sessionKey, session);
        }
        showCurrentLine(player, session);
    }

    private void showCurrentLine(Player player, ConversationSession session) {
        DialogueStage stage = session.getCurrentStage();
        if (stage == null) {
            // Если стадия не найдена (либо диалог кончился) – завершаем разговор
            endConversation(player, session);
            return;
        }

        showNPCReplice(session, player);
        showPlayerOption(session, player);
    }

    private void showPlayerOption(ConversationSession session, Player player) {
        DialogueLine line = session.getCurrentLine();

        List<PlayerOption> display = new ArrayList<>();
        if (line.options != null) display.addAll(line.options);

        PlayerOption cont = new PlayerOption();
        cont.id = -1;
        cont.text = "...";
        cont.nextStageId = session.getCurrentStage().id;
        display.add(cont);

        startChoice(player, display, selectedIndex -> {
            PlayerOption chosen = display.get(selectedIndex);
            if (chosen.id == -1) {
                // Опция "..." – завершаем диалог
                Bukkit.getScheduler().runTask(plugin, () -> endConversation(player, session));
            } else {
                session.setStage(chosen.nextStageId);
                // Продолжаем диалог
                Bukkit.getScheduler().runTask(plugin, () -> showCurrentLine(player, session));
            }
        });
    }

    public void showNPCReplice(ConversationSession session, Player player) {
        DialogueLine line = session.getCurrentLine();
        String text = (line == null || line.text == null) ? "" : line.text;

        org.bukkit.util.Vector dir = player.getEyeLocation().getDirection().normalize();
        double forward = 3.0;
        double height = 1.5;

        Location baseLoc = player.getLocation().clone().add(dir.multiply(forward)).add(0, height, 0);

        List<TextDisplay> old = session.getHologramLines();
        if (old != null) {
            removeHologramLines(this.plugin, old);
            session.setHologramLines(null);
        }

        List<String> splitLines = SplitNPCReplice(text.isEmpty() ? "TEST DISPLAY" : text);
        List<String> allLines = FromateNPCReplice(splitLines);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                List<TextDisplay> newLines = CreateHologramLines(allLines, player, baseLoc);
                session.setHologramLines(newLines);
                session.setLastDisplayedText(String.join("\n", allLines));
            } catch (Throwable t) {
                plugin.getLogger().severe("Failed to create TextDisplays: " + t.getMessage());
                t.printStackTrace();
            }
        });
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

    private void endConversation(Player player, ConversationSession session) {
        activeChoices.remove(player);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);

        removeHologramLines(this.plugin, session.getHologramLines());

        player.sendActionBar("");
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
