package listeners.api;

import org.bukkit.entity.Player;

public interface ISteerVehicleHandler {
    void handleSteerVehicle(Player player, boolean forward, boolean backward, boolean jump);
}