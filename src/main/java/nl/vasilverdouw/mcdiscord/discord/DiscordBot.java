package nl.vasilverdouw.mcdiscord.discord;

import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import nl.vasilverdouw.mcdiscord.config.IConfig;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscordBot {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final IConfig config;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            task -> new Thread(task, "McDiscord sync"));

    // Only the worker accesses JDA and the guild synchronization state.
    private JDA jda;
    private GuildData guildData;

    // Guarded by this object's monitor. No Discord work runs with this lock held.
    private List<String> latestPlayers = List.of();
    private boolean syncQueued;

    public DiscordBot(IConfig config) {
        this.config = config;
    }

    public void start() {
        worker.execute(this::connect);
    }

    private void connect() {
        String token = config.getBotToken();

        if (token == null || token.isBlank()) {
            LOGGER.error("Discord bot token is missing.");
            return;
        }

        try {
            jda = JDABuilder.createLight(token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .addEventListeners(new ListenerAdapter() {
                        @Override
                        public void onStatusChange(StatusChangeEvent event) {
                            if (event.getNewStatus() == JDA.Status.CONNECTED) {
                                LOGGER.info("Discord bot connected as {}", event.getJDA().getSelfUser().getName());
                                queueSync();
                            }
                        }
                    })
                    .build();
            guildData = new GuildData(jda, config);
        } catch (Exception e) {
            LOGGER.error("Failed to start Discord bot", e);
        }
    }

    public synchronized void stop() {
        if (!worker.isShutdown()) {
            worker.execute(this::disconnect);
            worker.shutdown();
        }
    }

    private void disconnect() {
        if (jda != null) {
            try {
                if (jda.getStatus() == JDA.Status.CONNECTED) {
                    sync(List.of());
                }
            } finally {
                jda.shutdown();
                LOGGER.info("Discord bot shutting down.");
            }
        }
    }

    public synchronized void updatePlayersOnline(List<String> players) {
        latestPlayers = List.copyOf(players);
        queueSync();
    }

    private synchronized void queueSync() {
        if (syncQueued || worker.isShutdown()) {
            return;
        }
        syncQueued = true;
        worker.execute(() -> {
            List<String> players = takeSnapshot();
            if (!worker.isShutdown() && jda != null && jda.getStatus() == JDA.Status.CONNECTED) {
                sync(players);
            }
        });
    }

    private synchronized List<String> takeSnapshot() {
        syncQueued = false;
        return latestPlayers;
    }

    private void sync(List<String> onlinePlayers) {
        try {
            showOnlinePlayersInActivity(onlinePlayers);
            guildData.syncOnlineRole(onlinePlayers);
            guildData.updateOnlineMessage(onlinePlayers);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to sync online players to Discord", e);
        }
    }

    private void showOnlinePlayersInActivity(List<String> onlinePlayers) {
        jda.getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.playing("Online Players: " + onlinePlayers.size()));
    }
}
