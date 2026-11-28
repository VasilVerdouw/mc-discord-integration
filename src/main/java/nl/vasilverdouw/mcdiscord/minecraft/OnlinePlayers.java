package nl.vasilverdouw.mcdiscord.minecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class OnlinePlayers {
    private List<ServerPlayer> onlinePlayers = new ArrayList<>();

    public List<String> getOnlinePlayerNames() {
        return onlinePlayers.stream().map(player -> player.getName().getString()).toList();
    }

    // Get all online players from the just logged in player's minecraft server.
    // If the player is not on a server, leave the list unchanged.
    public void playerLoggedIn(Player player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        updateOnlinePlayers(server);
    }

    public void playerLoggedOut(Player player) {
        onlinePlayers.removeIf(p -> p.getUUID().equals(player.getUUID()));
    }

    // Force an update to the online players list from the given server. If the server is null, leave the list unchanged.
    public void updateOnlinePlayers(MinecraftServer server) {
        if (server == null) {
            return;
        }

        onlinePlayers = new ArrayList<>(server.getPlayerList().getPlayers());
    }
}
