package com.Stickles.Discord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.Stickles.Discord.DiscordCommand;
import com.Stickles.Discord.DiscordEmojis;
import com.Stickles.ModularCoding.Module;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class FunCommands implements Module {

	final String MODULE_NAME = "Fun Commands";
	final List<String> DEPENDENCIES = Arrays.asList("Command Handler");
	
	@DiscordCommand(Name = "echo",
			Aliases = {"repeat","copy"},
			Summary = "I will copy what you say!",
			Syntax = "echo [command]")
	public void wowAnECHOCommand(MessageReceivedEvent e, ArrayList<String> args, MessageInfo info) {

		CommandHandler.sendMessage(e,String.join(" ", args),false);
		if (e.getChannelType() == ChannelType.TEXT) {
			e.getMessage().delete().queue();
		}
	}
	
	@DiscordCommand(Name = "random", 
			Summary = "Provides a random number between 0 and 100")
	public void superRanDOmLOL(MessageReceivedEvent e, ArrayList<String> args, MessageInfo info) {

		Random rnd = new Random();
		Integer randomInt = rnd.nextInt(100);
		CommandHandler.sendMessage(e,randomInt.toString(),false);
	}
	
	@DiscordCommand(Name = "clap", 
			Summary = "Will clap for free")
	public void mEMErEVIEWclapCLAP(MessageReceivedEvent e, ArrayList<String> args, MessageInfo info) {
		Random rnd = new Random();
		int randomInt = 0;
		while (randomInt == 0)
			randomInt = rnd.nextInt(10);
		CommandHandler.sendMessage(e,String.join("", Collections.nCopies(randomInt, DiscordEmojis.clap)),false);
		if (e.getChannelType() == ChannelType.TEXT)
			e.getMessage().delete().queue();
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
