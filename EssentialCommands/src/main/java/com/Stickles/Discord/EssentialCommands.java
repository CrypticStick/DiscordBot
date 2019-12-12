package com.Stickles.Discord;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.xml.sax.SAXException;

import com.Stickles.DiscordBot;
import com.Stickles.Discord.DiscordCommand;
import com.Stickles.Discord.EssentialCommandsDatabase.EssentialCommandsDatabase;
import com.Stickles.ModularCoding.DynamicModuleLoader.NoModuleDirException;
import com.Stickles.ModularCoding.Module;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EssentialCommands implements Module {

	final String MODULE_NAME = "Essential Commands";
	final List<String> DEPENDENCIES = Arrays.asList("Command Handler");
	
	static EssentialCommandsDatabase database = null;
	
	@DiscordCommand(Name = "help",
			Summary = "Lists information about available commands",
			Syntax = "help {command}")
	public void IcanHELPyoU(MessageReceivedEvent e, ArrayList<String> args) {

		EmbedBuilder eb = new EmbedBuilder();
		eb.setColor(0x0080FF);
		StringBuilder commandsSB = new StringBuilder();
		
		List<Module> mods = new ArrayList<Module>(DiscordBot.getModule("Command Handler").getDependants());
		Module current = this;
		Collections.sort(mods,new Comparator<Module>()	//Sorting the modules so that this one is always first
		  {
		     public int compare(Module o1, Module o2)
		     {
		         if (o1.equals(o2))
		           return 0;
		         if (o1.equals(current))
		           return -1;
		         if (o2.equals(current))
		           return 1;
		         return 0;
		     }
		});	
				
		boolean found = false;
		for (Module c : mods) {	//searches all command modules
			
			if (!args.isEmpty()) 
				found = false;
			
			final Method[] commandMethods = c.getClass().getDeclaredMethods(); 	// gets all of the methods in the class
			for (Method m : commandMethods) { 									// for each method in the class
				if (m.isAnnotationPresent(DiscordCommand.class)) { 					// if method is a command...
					DiscordCommand cmd = m.getAnnotation(DiscordCommand.class);
					
					String Name = "";
					String formattedAliases = "";
					String Summary = "";
					String SpecialPerms = "";
					String Syntax = "";
					boolean matchingAlias = false;

					Name = cmd.Name();
					String[] Aliases = cmd.Aliases();
					for (String a : Aliases) {
						if (args.contains(a)) matchingAlias = true;
					}
					if (args.isEmpty() || args.contains(Name) || matchingAlias) {
						found = true;
						if (cmd.Aliases().length > 0)
							formattedAliases = " (also " + String.join(", ", Aliases) + ")";
						Summary = cmd.Summary();
						if (cmd.SpecialPerms())
							SpecialPerms = " **<Mods Only>**";
						if (!cmd.Syntax().isEmpty())
							Syntax = " ~ `" + cmd.Syntax() + "`";
						commandsSB.append(String.format("`%s%s%s` - %s%s%s\n",DiscordBot.database.getPrefix(),Name,formattedAliases,Summary,SpecialPerms,Syntax));
					}
					
				}
			}
			String fieldname = String.format("%s:", c.getName());
			if (found)
				eb.addField(fieldname, commandsSB.toString(), true);
			commandsSB = new StringBuilder();
		}	
		
		if (!found) CommandHandler.sendMessage(e,String.format("Sorry %s, the requested command(s) do not exist.", e.getAuthor().getAsMention()),false);
		else CommandHandler.sendMessage(e, eb.build(), true);
	}
	
	@DiscordCommand(Name = "bot", 
			Summary = "Edits properties of the bot",
			Syntax = "bot [name] [game] {new text}",
			SpecialPerms = true)
	public static void itsTIMEtoEDITneoBOT(MessageReceivedEvent e, ArrayList<String> args) {
		
		if (args.isEmpty()) {
			CommandHandler.sendMessage(e,String.format("%s, please type `name` or `game`, along with the new text.", e.getAuthor().getAsMention()),false);
			return;
		}
		
		if (args.get(0).equals("name")) {
			args.remove(args.get(0));
			if (args.size() < 1) {
				CommandHandler.sendMessage(e,String.format("%s, please enter what you would like the new text to be.", e.getAuthor().getAsMention()),false);
				return;
			}
			e.getGuild().getMember(DiscordBot.jda.getSelfUser()).modifyNickname(
				String.join(" ", args)
			).queue();
			database.getGuildList().getGuild(e.getGuild().getId()).setName(String.join(" ", args));
			DiscordBot.writeDatabase(database);
			
			CommandHandler.sendMessage(e,String.format("NeoBot will now be called \"%s\"!", database.getGuildList().getGuild(e.getGuild().getId()).getName()),false);
			return;
		} else if (args.get(0).equals("game")) {
			args.remove(args.get(0));
			if (args.size() < 1) {
				CommandHandler.sendMessage(e,String.format("%s, please enter what you would like the new text to be.", e.getAuthor().getAsMention()),false);
				return;
			}
			DiscordBot.database.setGame(String.join(" ", args));
			DiscordBot.writeDatabase(DiscordBot.database);
			DiscordBot.jda.getPresence().setActivity(
					Activity.of(ActivityType.DEFAULT, String.format("%s (Type %shelp)", DiscordBot.database.getGame(), DiscordBot.database.getPrefix()))
					);
			CommandHandler.sendMessage(e,String.format("NeoBot is now playing \"%s\"!", String.join(" ", args)),false);
			return;
		}
	}
	
	@DiscordCommand(Name = "modules", 
			Summary = "Enables / disables bot code",
			Syntax = "modules [load] [unload] {module name}",
			SpecialPerms = true)
	public static void carefulWithTheseMODULES(MessageReceivedEvent e, ArrayList<String> args) {
		if (args.isEmpty()) {
			CommandHandler.sendMessage(e,String.format("%s, please specify what you want to do with the modules.", e.getAuthor().getAsMention()),false);
			return;
		}

		if (args.size() < 2 && !args.get(0).toLowerCase().equals("list")) {
			CommandHandler.sendMessage(e,"Please specify the module you wish to use.",false);
			return;
		}
		
		File module = null;
		if (args.size() > 1) {
			List<String> blacklisted = Arrays.asList("commandhandler","essentialcommands");
			if (blacklisted.contains(args.get(1).toLowerCase())) {
				CommandHandler.sendMessage(e,String.format("Module \"%s\" is a critical module and cannot be modified through Discord.", args.get(1)),false);
				return;
			}		

			try {
				module = new File("modules/" + args.get(1) + ".jar").getCanonicalFile();
			} catch (IOException ex) {
				ex.printStackTrace();
				return;
			}
			
			if (!module.exists())  {
				CommandHandler.sendMessage(e,String.format("Module \"%s\" does not exist.", args.get(1)),false);
				return;
			}
		}
		
		try {
			switch (args.get(0)) {
			case "load":
				boolean loaded = DiscordBot.loadJar(module, true);
				if (loaded)
					CommandHandler.sendMessage(e,String.format("Module \"%s\" has been loaded.", args.get(1)),false);
				else
					if (DiscordBot.getLoadedJars().contains(args.get(1)))
						CommandHandler.sendMessage(e,String.format("Module \"%s\" is already loaded!", args.get(1)),false);
					else
						CommandHandler.sendMessage(e,String.format("Module \"%s\" failed to load!", args.get(1)),false);
				break;
				
			case "reload":
				DiscordBot.unloadJar(module);
				boolean loaded2 = DiscordBot.loadJar(module, true);
				if (loaded2)
					CommandHandler.sendMessage(e,String.format("Module \"%s\" has been reloaded.", args.get(1)),false);
				else
					CommandHandler.sendMessage(e,String.format("Module \"%s\" failed to reload!", args.get(1)),false);
				break;
				
			case "unload":
				boolean unloaded = DiscordBot.unloadJar(module);
				if (unloaded)
					CommandHandler.sendMessage(e,String.format("Module \"%s\" has been unloaded.", args.get(1)),false);
				else
					if (DiscordBot.getUnloadedJars().contains(args.get(1)))
						CommandHandler.sendMessage(e,String.format("Module \"%s\" is already unloaded!", args.get(1)),false);
					else
						CommandHandler.sendMessage(e,String.format("Module \"%s\" failed to unload!", args.get(1)),false);
				break;
				
			case "list":
				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(0x99004C);
				
				StringBuilder sb = new StringBuilder();
				ArrayList<String> loadedJars = DiscordBot.getLoadedJars();
				ArrayList<String> unloadedJars = DiscordBot.getUnloadedJars();
				ArrayList<String> allJars = new ArrayList<String>();
				allJars.addAll(loadedJars);
				allJars.addAll(unloadedJars);
				java.util.Collections.sort(allJars);
				
				if (allJars.isEmpty())
					sb.append("None!" + System.lineSeparator());
				else {
					for (String jar : allJars)
						sb.append(
							((loadedJars.contains(jar)) ? DiscordEmojis.check : DiscordEmojis.cross) + 
							" " + 
							jar + System.lineSeparator()
						);
				}
				eb.addField("Modules:", sb.toString(), true);
				
				CommandHandler.sendMessage(e,eb.build(),false);
				break;
				
			default:
				CommandHandler.sendMessage(e,String.format("Parameter '%s' is not recognized.", args.get(0)),false);
				
			}
		} catch (NoModuleDirException ex) {
			CommandHandler.sendMessage(e,"Error: A module directory has not been set!",false);
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
	public void initialize() {
		try {
			database = DiscordBot.readDatabase(EssentialCommandsDatabase.class);
		} catch (SAXException e1) {
			e1.printStackTrace();
		}
		if (database == null)
			database = DiscordBot.writeDatabase(new EssentialCommandsDatabase());
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
