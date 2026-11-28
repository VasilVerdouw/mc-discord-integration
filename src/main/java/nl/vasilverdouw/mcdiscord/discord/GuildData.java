package nl.vasilverdouw.mcdiscord.discord;

import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import nl.vasilverdouw.mcdiscord.config.IConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GuildData {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final JDA jda;
    private final String guildId;

    private List<Member> guildMembers;
    private Guild guild;

    public GuildData(JDA jda, IConfig config) {
        this.jda = jda;
        guildId = config.getGuildId();
    }

    public List<Member> getGuildMembers() {
        return guildMembers;
    }

    public void updateGuildMembers() {
        if (jda == null) {
            LOGGER.warn("Discord bot is not running. Cannot update guild members.");
            return;
        }

        Guild guild = getGuild();
        if (guild == null) {
            LOGGER.warn("Discord bot is not in the specified (or any if none specified) guild. Cannot update guild members.");
            return;
        }

        guildMembers = new ArrayList<>(guild.loadMembers().get());
    }

    // This method returns a Role object called "Online" from the guild. If it doesn't exist, it creates it.
    public Role ensureOnlineRole(boolean createIfMissing)
    {
        Guild guild = getGuild();
        if (guild == null) {
            LOGGER.warn("Discord bot is not in the specified (or any if none specified) guild. Cannot ensure Online role.");
            return null;
        }

        Role onlineRole = guild.getRoles().stream()
                .filter(role -> role.getName().equalsIgnoreCase("Online"))
                .findFirst()
                .orElse(null);

        if (onlineRole == null && createIfMissing) {
            LOGGER.info("Online role does not exist. Creating it...");
            onlineRole = guild.createRole()
                    .setName("Online")
                    .setMentionable(true)
                    .setColor(0x00ff3c)
                    .complete();
            LOGGER.info("Online role created.");
        }

        return onlineRole;
    }

    public Member findDiscordUserByMinecraftName(String name, Role onlineRole) {
        Member match = null;
        for (Member member : guildMembers) {
            boolean matches = member.getRoles().stream().anyMatch(role ->
                    !role.equals(onlineRole) && role.getName().equalsIgnoreCase(name));
            if (matches) {
                if (match != null) {
                    LOGGER.warn("Multiple Discord members match Minecraft player {}", name);
                    return null;
                }
                match = member;
            }
        }
        return match;
    }

    public CompletableFuture<Void> setOnlineRole(Member member, Role role, boolean online) {
        return (online ? guild.addRoleToMember(member, role) : guild.removeRoleFromMember(member, role))
                .submit();
    }

    private Guild getGuild() {
        if (guild != null) {
            return guild;
        }

        if (guildId != null && !guildId.isBlank()) {
            guild = jda.getGuildById(guildId);
        } else {
            List<Guild> guilds = jda.getGuilds();
            if (guilds.isEmpty()) {
                return null;
            }

            guild = guilds.getFirst();
        }

        if (guild == null) {
            return null;
        }
        return guild;
    }
}
