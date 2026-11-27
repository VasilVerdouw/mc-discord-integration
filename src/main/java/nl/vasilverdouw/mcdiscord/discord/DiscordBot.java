package nl.vasilverdouw.mcdiscord.discord;

import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;

public class DiscordBot {

    private static final Logger LOGGER = LogUtils.getLogger();

    private JDA jda;

    public void start() {
        String token = System.getenv("DISCORD_BOT_TOKEN");

        if (token == null || token.isBlank()) {
            LOGGER.error("Discord bot token is missing.");
            return;
        }

        try {
            jda = JDABuilder.createLight(token)
                    .addEventListeners(new ListenerAdapter() {
                        @Override
                        public void onReady(ReadyEvent event) {
                            LOGGER.info(
                                    "Discord bot connected as {}",
                                    event.getJDA().getSelfUser().getName()
                            );
                        }
                    })
                    .build();

        } catch (Exception e) {
            LOGGER.error("Failed to start Discord bot", e);
        }
    }

    public void stop() {
        if (jda != null) {
            jda.shutdown();
            jda = null;

            LOGGER.info("Discord bot shutting down.");
        }
    }

    public void updatePlayersOnline(List<ServerPlayer> onlinePlayers) {
        if (jda == null) {
            LOGGER.warn("Discord bot is not running. Cannot update online players.");
            return;
        }

        StringBuilder playerList = new StringBuilder("Online Players: " + onlinePlayers.size() + "\n\n");
        for (ServerPlayer player : onlinePlayers) {
            playerList.append("- ").append(player.getName().getString()).append("\n");
        }

        jda.getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.playing(playerList.toString()));
    }
}