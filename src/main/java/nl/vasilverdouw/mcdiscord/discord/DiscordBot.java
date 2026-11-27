package nl.vasilverdouw.mcdiscord.discord;

import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DiscordBot {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ACTIVITY_LIMIT = 128;

    private volatile List<String> playerNames = List.of();
    private volatile JDA readySession;
    private JDA jda;
    private ScheduledExecutorService worker;
    private List<String> lastSynced;

    public void start() {
        String token = System.getenv("DISCORD_BOT_TOKEN");
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
                        public void onReady(ReadyEvent event) {
                            readySession = event.getJDA();
                        }
                    })
                    .build();
            worker = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "McDiscord sync");
                thread.setDaemon(true);
                return thread;
            });
            worker.scheduleWithFixedDelay(() -> sync(false), 0, 1, TimeUnit.SECONDS);
            worker.scheduleWithFixedDelay(() -> sync(true), 30, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOGGER.error("Failed to start Discord bot", e);
        }
    }

    public void stop() {
        if (jda == null) {
            return;
        }

        try {
            if (worker != null && readySession == jda) {
                worker.submit(() -> reconcile(jda, List.of(), false)).get(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            LOGGER.warn("Could not finish clearing Discord Online roles before shutdown", e);
        } finally {
            if (worker != null) {
                worker.shutdownNow();
            }
            jda.shutdownNow();
        }
    }

    public void updatePlayersOnline(List<ServerPlayer> onlinePlayers) {
        playerNames = new ArrayList<>(onlinePlayers.stream()
                .map(player -> player.getGameProfile().getName()).toList());
    }

    private void sync(boolean force) {
        JDA session = readySession;
        List<String> snapshot = playerNames;
        if (session == null || session.getStatus() != JDA.Status.CONNECTED) {
            return;
        }
        if (!force && snapshot == lastSynced) {
            return;
        }

        // Retry a failed synchronization on the periodic pass, or as soon as the roster changes.
        lastSynced = snapshot;
        try {
            reconcile(session, snapshot, true);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not synchronize Minecraft players with Discord", e);
        }
    }

    private void reconcile(JDA session, List<String> names, boolean updateActivity) {
        if (updateActivity) {
            session.getPresence().setActivity(Activity.playing(activityText(names)));
        }

        GuildData guildData = new GuildData(session);
        Role onlineRole = guildData.ensureOnlineRole(updateActivity && !names.isEmpty());
        if (onlineRole == null) {
            return;
        }

        guildData.updateGuildMembers();
        var onlineMembers = new HashSet<Member>();
        for (String name : names) {
            Member member = guildData.findDiscordUserByMinecraftName(name, onlineRole);
            if (member != null) {
                onlineMembers.add(member);
            }
        }

        var changes = new ArrayList<CompletableFuture<Void>>();
        for (Member member : guildData.getGuildMembers()) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            boolean shouldHaveRole = onlineMembers.contains(member);
            boolean hasRole = member.getRoles().contains(onlineRole);
            if (shouldHaveRole != hasRole) {
                changes.add(guildData.setOnlineRole(member, onlineRole, shouldHaveRole));
            }
        }
        CompletableFuture.allOf(changes.toArray(CompletableFuture<?>[]::new)).join();
    }

    private String activityText(List<String> names) {
        StringBuilder text = new StringBuilder("Online Players: ").append(names.size());
        for (String name : names) {
            if (text.length() + name.length() + 2 > ACTIVITY_LIMIT - 4) {
                return text.append(" ...").toString();
            }
            text.append(", ").append(name);
        }
        return text.toString();
    }
}
