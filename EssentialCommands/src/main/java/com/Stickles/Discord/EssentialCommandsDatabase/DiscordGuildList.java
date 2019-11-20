package com.Stickles.Discord.EssentialCommandsDatabase;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;

public class DiscordGuildList {

    private ArrayList<DiscordGuild> guildList;

    public DiscordGuildList() {
    }

    public DiscordGuildList(ArrayList<DiscordGuild> guildList) {
        this.guildList = guildList;
    }

    @XmlElement(name = "guild")
    public ArrayList<DiscordGuild> getGuildList() {
    	if (guildList == null)
    		guildList = new ArrayList<DiscordGuild>();
        return guildList;
    }

    public DiscordGuildList setGuildList(ArrayList<DiscordGuild> guildList) {
        this.guildList = guildList;
        return this;
    }
    
    private DiscordGuild isGuildSaved(String id) {
    	if (guildList == null)
    		guildList = new ArrayList<DiscordGuild>();
    	
    	for(int i = 0; this.guildList.size() > i ; i++) 
    		if (guildList.get(i).getId().equals(id)) 
    			return guildList.get(i);
    	
        return null;
    }
    
    public DiscordGuildList addGuild(DiscordGuild guild) {
    	if (isGuildSaved(guild.getId()) == null)
    		getGuildList().add(guild);
    	return this;
    }
    
    public DiscordGuild getGuild(String id) {
    	DiscordGuild guild = isGuildSaved(id);
    	if (guild == null) {
    		guild = new DiscordGuild(id, "DiscordBot");
    		addGuild(guild);
    	}
    	return guild;
    }
    
    public DiscordGuildList removeGuild(DiscordGuild guild) {
    	if (isGuildSaved(guild.getId()) != null)
    		getGuildList().remove(guild);
    	return this;
    }

}