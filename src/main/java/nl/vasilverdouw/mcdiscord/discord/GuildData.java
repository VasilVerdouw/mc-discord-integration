package nl.vasilverdouw.mcdiscord.discord;

import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import nl.vasilverdouw.mcdiscord.config.IConfig;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class GuildData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long ROLE_REQUEST_TIMEOUT_SECONDS = 10;

    private final JDA jda;
    private String guildId;
    private String onlineUpdatesChannelId;

    private Role previousOnlineRole;
    private Set<Long> previousOnlineMembers = Set.of();

    public GuildData(JDA jda, IConfig config) {
        this.jda = jda;
        guildId = config.getGuildId();
        onlineUpdatesChannelId = config.getOnlineUpdatesChannelId();
    }

    public void syncOnlineRole(List<String> onlinePlayers) {
        Guild guild = getGuild();
        if (guild == null) {
            LOGGER.warn("Discord bot is not in the specified (or any if none specified) guild. Cannot sync online roles.");
            return;
        }

        Role onlineRole = ensureOnlineRole(guild);
        List<Member> members = guild.getMembers();
        Set<Long> onlineMemberIds = new HashSet<>();
        for (String playerName : onlinePlayers) {
            Member member = findDiscordUserByMinecraftName(playerName, members);
            if (member != null) {
                onlineMemberIds.add(member.getIdLong());
            } else {
                LOGGER.warn("No Discord user found for Minecraft player: " + playerName);
            }
        }

        List<CompletableFuture<Void>> updates = new ArrayList<>();
        for (Member member : members) {
            long id = member.getIdLong();
            boolean shouldHaveRole = onlineMemberIds.contains(id);
            // Gateway role events can arrive after the REST response. Reassert a change in
            // desired state even if the cache still shows the state before the last update.
            boolean changed = onlineRole.equals(previousOnlineRole)
                    && (previousOnlineMembers.contains(id) != shouldHaveRole);
            if (changed || member.getRoles().contains(onlineRole) != shouldHaveRole) {
                var action = shouldHaveRole ? guild.addRoleToMember(member, onlineRole)
                        : guild.removeRoleFromMember(member, onlineRole);
                updates.add(action.timeout(ROLE_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .submit().exceptionally(error -> {
                            LOGGER.warn("Failed to sync Online role for Discord member {}", id, error);
                            return null;
                        }));
            }
        }
        // Wait on the Discord worker so a later snapshot cannot overtake these role changes.
        CompletableFuture.allOf(updates.toArray(CompletableFuture<?>[]::new)).join();
        previousOnlineRole = onlineRole;
        previousOnlineMembers = onlineMemberIds;
    }

    private Role ensureOnlineRole(Guild guild) {
        Role onlineRole = guild.getRoles().stream()
                .filter(role -> role.getName().equalsIgnoreCase("Online"))
                .findFirst()
                .orElse(null);

        if (onlineRole == null) {
            LOGGER.info("Online role does not exist. Creating it...");
            onlineRole = guild.createRole()
                    .setName("Online")
                    .setMentionable(true)
                    .setColor(0x00ff3c)
                    .timeout(ROLE_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .complete();
            LOGGER.info("Online role created.");
        }

        return onlineRole;
    }

    private Member findDiscordUserByMinecraftName(String name, List<Member> members) {
        for (Member member : members) {
            for (String roleName : member.getRoles().stream().map(Role::getName).toList()) {
                if (roleName.equalsIgnoreCase(name)) {
                    return member;
                }
            }
        }
        return null;
    }

    public void updateOnlineMessage(List<String> onlinePlayers, Instant lastPlayerOnline) {
        if (onlineUpdatesChannelId == null || onlineUpdatesChannelId.isBlank()) {
            return;
        }

        Guild guild = getGuild();
        if (guild == null) {
            LOGGER.warn("Discord bot is not in the specified (or any if none specified) guild. Cannot update online message.");
            return;
        }

        var channel = guild.getTextChannelById(onlineUpdatesChannelId);
        if (channel == null) {
            LOGGER.warn("Online updates channel ID is not set or invalid. Cannot update online message.");
            return;
        }

        var previousMessages = channel.getIterableHistory().stream()
                .filter(message -> message.getAuthor().equals(jda.getSelfUser()))
                .limit(1)
                .toList();

        String content = formatOnlineMessage(onlinePlayers, lastPlayerOnline);

        if (!previousMessages.isEmpty()) {
            var previousMessage = previousMessages.getFirst();
            previousMessage.editMessage(content)
                    .timeout(ROLE_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .queue(
                            success -> {
                            },
                            error -> LOGGER.warn("Failed to update online players message in Discord.", error)
                    );
        } else {
            channel.sendMessage(content)
                    .timeout(ROLE_REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .queue(
                            success -> {
                            },
                            error -> LOGGER.warn("Failed to update online players message in Discord.", error)
                    );
        }
    }

    private String formatOnlineMessage(List<String> onlinePlayers, Instant lastPlayerOnline) {
        if (onlinePlayers.isEmpty() && lastPlayerOnline != null) {
            return "# 0 player(s) online\nLast player online <t:" + lastPlayerOnline.getEpochSecond() + ":R>";
        }

        StringBuilder playerList = new StringBuilder("# " + onlinePlayers.size() + " player(s) online\n");
        for (String player : onlinePlayers) {
            playerList.append("- ").append(player).append("\n");
        }
        return playerList.toString();
    }

    private Guild getGuild() {
        if (guildId != null && !guildId.isBlank()) {
            return jda.getGuildById(guildId);
        }
        List<Guild> guilds = jda.getGuilds();
        if (guilds.isEmpty()) {
            return null;
        }
        Guild guild = guilds.getFirst();
        guildId = guild.getId();
        return guild;
    }
}
