package com.Stickles.Discord.MusicCommandsDatabase;

import java.util.ArrayList;

import javax.xml.bind.annotation.*;

import com.Stickles.DiscordBot;
import com.Stickles.Discord.Database;

import net.dv8tion.jda.core.entities.Guild;

@XmlRootElement
public class MusicCommandsDatabase implements Database {

    private DiscordGuildList guildList;

    public MusicCommandsDatabase(){
        this.guildList = getGuildList();
    }

    public MusicCommandsDatabase(DiscordGuildList guildList) {
        this.guildList = guildList;
    }
    
	static DiscordGuildList createDiscordGuildList() {
		ArrayList<DiscordGuild> dGuilds = new ArrayList<DiscordGuild>();
		if (DiscordBot.jda != null)
			for (Guild guild : DiscordBot.jda.getGuilds())
				dGuilds.add(new DiscordGuild(guild.getId(),"",new MusicModeList()));
		return new DiscordGuildList(dGuilds);
	}
    
    @XmlElement
    public DiscordGuildList getGuildList() {
    	if (guildList == null)
    		guildList = new DiscordGuildList();
        return guildList;
    }

    public MusicCommandsDatabase setGuildList(DiscordGuildList guildList) {
        this.guildList = guildList;
        DiscordBot.writeDatabase(this);
        return this;
    }
}