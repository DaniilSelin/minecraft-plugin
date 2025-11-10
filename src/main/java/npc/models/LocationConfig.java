package npc.models;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationConfig {
    @JsonProperty("world")
    public String world;
    @JsonProperty("x")
    public double x;
    @JsonProperty("y")
    public double y;
    @JsonProperty("z")
    public double z;
    @JsonProperty("yaw")
    public float yaw = 0f;
    @JsonProperty("pitch")
    public float pitch = 0f;

    public Location toBukkitLocation(JavaPlugin plugin) {
        World w = plugin.getServer().getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return "LocationConfig{" + world + ":" + x + "," + y + "," + z + " yaw=" + yaw + " pitch=" + pitch + "}";
    }
}
