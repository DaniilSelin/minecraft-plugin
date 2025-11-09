package dialogue.store.session;

import java.util.List;
import java.util.function.Consumer;

import dialogue.models.PlayerOption;

public class ChoiceSession {
    private final List<PlayerOption> options;
    private final Consumer<Integer> onSelect;
    private int currentIndex = 0;

    public int getCurrentIndex() {
        return currentIndex;
    }

    public String GetTextOp(int i) {
        return options.get(i).text;
    }

    public void Accept(int sel) {
        onSelect.accept(sel);
    }

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
    public int SizeOption() {
        return options.size();
    }
}
