package listeners.protocol;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.PacketType;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

import listeners.api.ISteerVehicleHandler;

public class SteerVehicleManager {

    private final Consumer<String> infoLogger;
    private final Consumer<String> errorLogger;

    public SteerVehicleManager(Consumer<String> infoLogger, Consumer<String> errorLogger) {
        this.infoLogger = infoLogger;
        this.errorLogger = errorLogger;
    }

    public void registerSteerVehicleListener(Plugin plugin, List<ISteerVehicleHandler> handlers) {
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.STEER_VEHICLE) {
                    @Override
                    public void onPacketReceiving(PacketEvent event) {
                        handleSteerVehiclePacket(event, handlers);
                    }
                }
        );
    }

    private void handleSteerVehiclePacket(PacketEvent event, List<ISteerVehicleHandler> handlers) {
        PacketContainer packet = event.getPacket();
        Player player = event.getPlayer();

        try {
            Object input = packet.getModifier().read(0);
            if (input == null) return;

            Class<?> inputClass = input.getClass();
            Field forwardField = inputClass.getDeclaredField("forward");
            Field backwardField = inputClass.getDeclaredField("backward");
            Field jumpField = inputClass.getDeclaredField("jump");

            forwardField.setAccessible(true);
            backwardField.setAccessible(true);
            jumpField.setAccessible(true);

            boolean forward = forwardField.getBoolean(input);
            boolean backward = backwardField.getBoolean(input);
            boolean jump = jumpField.getBoolean(input);

            infoLogger.accept("пакет пойман и обработан");

            event.setCancelled(true);
            for (ISteerVehicleHandler handler : handlers) {
                handler.handleSteerVehicle(player, forward, backward, jump);
            }

        } catch (Exception e) {
            errorLogger.accept("Ошибка при чтении STEER_VEHICLE: " + e.getMessage());
            e.printStackTrace();
        }
    }
}