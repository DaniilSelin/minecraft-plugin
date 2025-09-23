package Dialogue.Manage;

import Dialogue.Dialogue;
import Dialogue.DialogueLine;
import Dialogue.DialogueStage;
import Dialogue.PlayerOption;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import listeners.api.ISteerVehicleHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DialogManager implements Listener, ISteerVehicleHandler {

    // Сессии диалогов: ключ = npcName + "_" + playerUUID
    private final Map<String, ConversationSession> conversationSessions = new HashMap<>();
    // Активные выборы для каждого игрока (отображаются в ActionBar)
    private final Map<Player, ChoiceSession> activeChoices = new HashMap<>();

    // Начало или возобновление диалога игрока с данным NPC
    public void beginConversation(Player player, String npcName, Dialogue dialogue) {
        String sessionKey = npcName + "_" + player.getUniqueId().toString();
        ConversationSession session = conversationSessions.get(sessionKey);
        if (session == null) {
            // Если новой сессии ещё нет – создаём её, начиная с первой стадии
            session = new ConversationSession(dialogue, npcName);
            conversationSessions.put(sessionKey, session);
        }
        // Показываем текущее состояние диалога (реплика NPC и варианты ответа, если они есть)
        showCurrentLine(player, session);
    }

    // Отображает текущую линию диалога и варианты ответа игроку
    private void showCurrentLine(Player player, ConversationSession session) {
        DialogueStage stage = session.getCurrentStage();
        if (stage == null) {
            // Если стадия не найдена (либо диалог кончился) – завершаем разговор
            endConversation(player, session);
            return;
        }

        List<DialogueLine> lines = stage.lines;
        int idx = session.getCurrentLineIndex();

        // Если вышли за границы реплик текущей стадии, диалог завершается (без перехода)
        if (idx >= lines.size()) {
            endConversation(player, session);
            return;
        }

        DialogueLine line = lines.get(idx);
        // Отправляем игроку текст NPC (например, "NPC: Привет, путник...")
        if (line.text != null && !line.text.isEmpty()) {
            player.sendMessage(line.speaker + ": " + line.text);
        }

        // Получаем варианты ответа из текущей реплики (PlayerOption)
        List<PlayerOption> options = line.options;
        if (options != null && !options.isEmpty()) {
            // Если есть опции, запускаем выбор игрока
            startChoice(player, options, selectedIndex -> {
                PlayerOption chosen = options.get(selectedIndex);
                if (chosen.nextStageId != null) {
                    // Переход к следующей стадии диалога
                    session.setStage(chosen.nextStageId);
                    session.setLineIndex(0);
                    showCurrentLine(player, session);
                } else {
                    // Если nextStageId == null – диалог закончился
                    endConversation(player, session);
                }
            });
        } else {
            // Если опций нет, переходим к следующей реплике текущей стадии
            session.incrementLineIndex();
            showCurrentLine(player, session);
        }
    }

    // Инициализация сессии выбора (ActionBar) с вариантами ответов
    private void startChoice(Player player, List<PlayerOption> options, Consumer<Integer> onSelect) {
        ChoiceSession choiceSession = new ChoiceSession(options, onSelect);
        activeChoices.put(player, choiceSession);
        sendChoiceMessage(player);
        // Блокируем движение игрока во время выбора
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
    }

    // Отправка ActionBar сообщения с вариантами; текущий выделенный вариант – красным цветом
    private void sendChoiceMessage(Player player) {
        ChoiceSession cs = activeChoices.get(player);
        if (cs == null) return;

        StringBuilder msg = new StringBuilder();
        for (int i = 0; i < cs.options.size(); i++) {
            if (i == cs.currentIndex) {
                msg.append("§c"); // выделяем красным текущую опцию
            } else {
                msg.append("§7");
            }
            msg.append(cs.options.get(i).text);
            if (i < cs.options.size() - 1) {
                msg.append(" §7| "); // разделитель между опциями
            }
        }
        player.sendActionBar(msg.toString());
    }

    // Завершение разговора: удаляем сессию выбора, восстанавливаем управление игроком, очищаем сессию диалога
    private void endConversation(Player player, ConversationSession session) {
        activeChoices.remove(player);              // убираем активные варианты
        player.setWalkSpeed(0.2f);                 // восстанавливаем скорость игрока
        player.setFlySpeed(0.1f);
        String sessionKey = session.getNpcName() + "_" + player.getUniqueId().toString();
        conversationSessions.remove(sessionKey);   // удаляем сохранённую сессию диалога
        player.sendActionBar("");                 // очищаем ActionBar
    }

    // Обработка нажатий: вперёд/назад для смены варианта, прыжок – выбор текущего
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
            int sel = cs.currentIndex;
            activeChoices.remove(player);
            cs.onSelect.accept(sel);
        }
    }

    // Если игрок вышел из игры – отменяем активный выбор (прогресс диалога сохраняется)
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        activeChoices.remove(player);
        // Диалог в conversationSessions оставляем для сохранения прогресса (при следующем входе он возобновится)
    }

    // Класс, хранящий состояние разговора (диалог, текущая стадия и индекс реплики)
    private static class ConversationSession {
        private final Dialogue dialogue;
        private final String npcName;
        private String currentStageId;
        private int lineIndex = 0;

        public ConversationSession(Dialogue dialogue, String npcName) {
            this.dialogue = dialogue;
            this.npcName = npcName;
            // Стартуем с первой стадии списка
            if (dialogue.stages != null && !dialogue.stages.isEmpty()) {
                this.currentStageId = dialogue.stages.get(0).id;
            }
        }

        public String getNpcName() {
            return npcName;
        }

        public DialogueStage getCurrentStage() {
            if (dialogue.stages == null) return null;
            for (DialogueStage st : dialogue.stages) {
                if (st.id.equals(currentStageId)) {
                    return st;
                }
            }
            return null;
        }

        public int getCurrentLineIndex() {
            return lineIndex;
        }
        public void incrementLineIndex() {
            lineIndex++;
        }
        public void setLineIndex(int idx) {
            this.lineIndex = idx;
        }
        public void setStage(String nextStageId) {
            this.currentStageId = nextStageId;
            this.lineIndex = 0;
        }
    }

    // Класс для хранения текущих опций выбора (используется ActionBar)
    private static class ChoiceSession {
        private final List<PlayerOption> options;
        private final Consumer<Integer> onSelect;
        private int currentIndex = 0;

        public ChoiceSession(List<PlayerOption> options, Consumer<Integer> onSelect) {
            this.options = options;
            this.onSelect = onSelect;
        }
        public void next() {
            currentIndex = (currentIndex + 1) % options.size();
        }
        public void previous() {
            currentIndex = (currentIndex - 1 + options.size()) % options.size();
        }
    }
}
