package com.Stickles.Discord;

import net.dv8tion.jda.api.entities.Guild;

public class MessageInfo {

	private Guild guild;
	
	public MessageInfo(Guild guild) {
		this.guild = guild;
	}
	
	public Guild getGuild() {
		return guild;
	}
	
}
