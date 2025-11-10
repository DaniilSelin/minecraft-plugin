package npc.trait;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ManagerRegistry {
    private static final Map<String, ITraitManager> REG = new ConcurrentHashMap<>();
    private ManagerRegistry() {}

    public static void register(ITraitManager mgr) {
        if (mgr.getName() == null || mgr == null) return;
        REG.put(mgr.getName().toLowerCase(), mgr);
    }

    public static ITraitManager get(String name) {
        if (name == null) return null;
        return REG.get(name.toLowerCase());
    }

    public static void unregister(String name) {
        if (name == null) return;
        REG.remove(name.toLowerCase());
    }
}
