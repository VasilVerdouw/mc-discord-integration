package nl.vasilverdouw.mcdiscord;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.vasilverdouw.mcdiscord.discord.DiscordBot;
import nl.vasilverdouw.mcdiscord.minecraft.OnlinePlayers;
import org.slf4j.Logger;

@Mod(value = McDiscordMod.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class McDiscordMod {

    private final DiscordBot discordBot = new DiscordBot();
    private final OnlinePlayers onlinePlayers = new OnlinePlayers();
    public static final String MOD_ID = "mcdiscord";
    public static final Logger LOGGER = LogUtils.getLogger();

    public McDiscordMod() {
        LOGGER.info("McDiscord is initializing!");

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLeave);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mcdiscord")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("refresh")
                        .executes(context -> {
                            discordBot.updatePlayersOnline(context.getSource().getServer().getPlayerList().getPlayers());
                            return 1;
                        })));
    }

    private void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("McDiscord: Minecraft server started!");
        discordBot.updatePlayersOnline(event.getServer().getPlayerList().getPlayers());
        discordBot.start();
    }

    private void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % (30 * 20) == 0) {
            onlinePlayers.refresh(server);
            discordBot.updatePlayersOnline(onlinePlayers.getOnlinePlayers());
        }
    }

    private void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("McDiscord: Minecraft server stopping!");
        discordBot.stop();
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        LOGGER.info("McDiscord: Player joined the server: " + getPlayerName(player));
        onlinePlayers.playerLoggedIn(player);
        discordBot.updatePlayersOnline(onlinePlayers.getOnlinePlayers());
    }

    private void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        LOGGER.info("McDiscord: Player left the server: " + getPlayerName(player));
        onlinePlayers.playerLoggedOut(player);
        discordBot.updatePlayersOnline(onlinePlayers.getOnlinePlayers());
    }

    private String getPlayerName(Player player) {
        return player.getName().getString();
    }
}
