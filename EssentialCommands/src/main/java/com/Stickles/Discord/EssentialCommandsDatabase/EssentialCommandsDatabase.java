package com.Stickles.Discord.EssentialCommandsDatabase;

import java.util.ArrayList;

import javax.xml.bind.annotation.*;

import com.Stickles.DiscordBot;
import com.Stickles.Discord.Database;

import net.dv8tion.jda.api.entities.Guild;

@XmlRootElement
public class EssentialCommandsDatabase implements Database {

    private DiscordGuildList guildList;

    public EssentialCommandsDatabase(){
        this.guildList = getGuildList();
    }

    public EssentialCommandsDatabase(DiscordGuildList guildList) {
        this.guildList = guildList;
    }
    
	static DiscordGuildList createDiscordGuildList() {
		ArrayList<DiscordGuild> dGuilds = new ArrayList<DiscordGuild>();
		if (DiscordBot.jda != null)
			for (Guild guild : DiscordBot.jda.getGuilds())
				dGuilds.add(new DiscordGuild(guild.getId(),"DiscordBot"));
		return new DiscordGuildList(dGuilds);
	}
    
    @XmlElement
    public DiscordGuildList getGuildList() {
    	if (guildList == null)
    		guildList = new DiscordGuildList();
        return guildList;
    }

    public EssentialCommandsDatabase setGuildList(DiscordGuildList guildList) {
        this.guildList = guildList;
        DiscordBot.writeDatabase(this);
        return this;
    }
}