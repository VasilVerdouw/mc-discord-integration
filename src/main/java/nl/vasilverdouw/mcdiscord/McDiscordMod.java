package nl.vasilverdouw.mcdiscord;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import nl.vasilverdouw.mcdiscord.config.NeoForgeConfig;
import nl.vasilverdouw.mcdiscord.config.IConfig;
import nl.vasilverdouw.mcdiscord.discord.DiscordBot;
import nl.vasilverdouw.mcdiscord.minecraft.OnlinePlayers;
import org.slf4j.Logger;

@Mod(value = McDiscordMod.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class McDiscordMod {

    public final IConfig config;
    private final DiscordBot discordBot;
    private final OnlinePlayers onlinePlayers = new OnlinePlayers();
    public static final String MOD_ID = "mcdiscord";
    public static final Logger LOGGER = LogUtils.getLogger();

    public McDiscordMod(ModContainer container) {
        NeoForgeConfig fileConfig = new NeoForgeConfig();
        // COMMON configs are not synced to clients, keeping the bot token on the server.
        container.registerConfig(ModConfig.Type.COMMON, fileConfig.getSpec());
        config = fileConfig;
        discordBot = new DiscordBot(config);
        LOGGER.info("McDiscord is initializing!");

        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLeave);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mcdiscord")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("refresh")
                        .executes(context -> {
                            discordBot.updatePlayersOnline(onlinePlayers.getOnlinePlayers());
                            return 1;
                        })));
    }

    private void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("McDiscord: Minecraft server started!");
        discordBot.start();
        discordBot.updatePlayersOnline(onlinePlayers.getOnlinePlayers());
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
