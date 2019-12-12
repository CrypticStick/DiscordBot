package com.Stickles.Discord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.Stickles.Discord.DiscordCommand;
import com.Stickles.ModularCoding.Module;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class AFJROTCCommands implements Module {

	final String MODULE_NAME = "AFJROTC Commands";
	final List<String> DEPENDENCIES = Arrays.asList("Command Handler");
	
	@DiscordCommand(Name = "request",
			Summary = "Register account with server (Mandatory!)"
			)
	public void uhhhYeahIdLikeToRegister(MessageReceivedEvent e, ArrayList<String> args) {
		CommandHandler.sendMessage(e,"Welcome to Yakkie's AFJROTC Discord Registration!\nPlease Answer the following questions:",true);
		
		CommandHandler.sendMessage(e,"Test",false);
		if (e.getChannelType() == ChannelType.TEXT) {
			e.getMessage().delete().queue();
		}
	}

	@Override
	public String getName() {
		return MODULE_NAME;
	}
	
	@Override
	public List<String> getDependencies() {
		return DEPENDENCIES;
	}
	
	@Override
	public void initialize() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addDependants(Module toAdd) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeDependants(Module toRemove) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Module> getDependants() {
		// TODO Auto-generated method stub
		return null;
	}
}
