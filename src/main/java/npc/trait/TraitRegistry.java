package npc.trait;

import npc.models.TraitConfig;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public final class TraitRegistry {
    private static final Map<String, BiFunction<Entity, TraitConfig, Trait>> REGISTRY = new ConcurrentHashMap<>();

    private TraitRegistry() {}

    public static void register(String name, BiFunction<Entity, TraitConfig, Trait> factory) {
        if (name == null || factory == null) return;
        REGISTRY.put(name.toLowerCase(), factory);
    }

    public static Trait create(String name, Entity entity, TraitConfig cfg) {
        if (name == null) return null;
        BiFunction<Entity, TraitConfig, Trait> f = REGISTRY.get(name.toLowerCase());
        if (f == null) return null;
        return f.apply(entity, cfg);
    }

    public static void unregister(String name) {
        if (name != null) REGISTRY.remove(name.toLowerCase());
    }

    public static boolean isRegistered(String name) {
        return name != null && REGISTRY.containsKey(name.toLowerCase());
    }
}
