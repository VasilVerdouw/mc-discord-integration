package nl.vasilverdouw.mcdiscord.discord;

import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.server.level.ServerPlayer;
import nl.vasilverdouw.mcdiscord.config.IConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class DiscordBot {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final IConfig config;
    private GuildData guildData;

    private JDA jda;

    public DiscordBot(IConfig config) {
        this.config = config;
    }

    public void start() {
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
                        public void onReady(ReadyEvent event) {
                            LOGGER.info(
                                    "Discord bot connected as {}",
                                    event.getJDA().getSelfUser().getName()
                            );
                        }
                    })
                    .build();
            guildData = new GuildData(jda, config);
        } catch (Exception e) {
            LOGGER.error("Failed to start Discord bot", e);
        }
    }

    public void stop() {
        if (jda != null) {
            showOnlinePlayersInActivity(new ArrayList<>());
            giveOnlinePlayersRole(new ArrayList<>());


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

        showOnlinePlayersInActivity(onlinePlayers);

        giveOnlinePlayersRole(onlinePlayers);
    }

    private void showOnlinePlayersInActivity(List<ServerPlayer> onlinePlayers) {
        StringBuilder playerList = new StringBuilder("Online Players: " + onlinePlayers.size() + "\n\n");
        for (ServerPlayer player : onlinePlayers) {
            playerList.append("- ").append(player.getName().getString()).append("\n");
        }

        jda.getPresence().setActivity(net.dv8tion.jda.api.entities.Activity.playing(playerList.toString()));
    }

    private void giveOnlinePlayersRole(List<ServerPlayer> onlinePlayers) {
        Role onlineRole = guildData.ensureOnlineRole();
        if (onlineRole == null) {
            LOGGER.warn("Online role could not be ensured. Cannot give online players the role.");
            return;
        }

        guildData.updateGuildMembers();

        // Create a list of Discord members that correspond to the online Minecraft players
        List<Member> onlineMembers = new ArrayList<>();
        for (ServerPlayer player : onlinePlayers) {
            String playerName = player.getName().getString();
            Member member = guildData.findDiscordUserByMinecraftName(playerName);
            if (member != null) {
                onlineMembers.add(member);
            } else {
                LOGGER.warn("No Discord user found for Minecraft player: " + playerName);
            }
        }

        // For each member in the guild, if they are in the onlineMembers list, give them the Online role.
        // If they are not in the list, remove the Online role from them.
        for (Member member : guildData.getGuildMembers()) {
            if (onlineMembers.contains(member)) {
                guildData.giveRoleToMember(member, onlineRole);
            } else {
                guildData.removeRoleFromMember(member, onlineRole);
            }
        }
    }
}
