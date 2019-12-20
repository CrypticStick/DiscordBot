package com.Stickles.Discord;

import net.dv8tion.jda.api.exceptions.VerificationLevelException;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xml.sax.SAXException;

import com.Stickles.Discord.DiscordCommand;
import com.Stickles.Discord.CommandHandlerDatabase.CommandHandlerDatabase;
import com.Stickles.Discord.CommandHandlerDatabase.DiscordGuild;
import com.Stickles.ModularCoding.Module;
import com.Stickles.DiscordBot;

public class CommandHandler extends ListenerAdapter implements Module {

	final String MODULE_NAME = "Command Handler";
	
	private static List<Module> listeners = new ArrayList<Module>();
	private final static Object lock = new Object();
	static CommandHandlerDatabase database = null;
	
	@DiscordCommand(Name = "setmod",
			Summary = "Sets the mod role!",
			Syntax = "setmod {role name}",
			SpecialPerms = true)
	public void areYOUaMod(MessageReceivedEvent e, ArrayList<String> args) {
		if (args.size() == 0) {
			CommandHandler.sendMessage(e, String.format("%s, please type the name of the role!", e.getAuthor().getAsMention()),false);
			return;
		}
		String roleName = String.join(" ", args);
		List<Role> rolesWithName = e.getGuild().getRolesByName(roleName, true);
		if (rolesWithName.isEmpty()) {
			CommandHandler.sendMessage(e, String.format("%s, that role doesn't exist!", e.getAuthor().getAsMention()),false);
			return;
		} else {
			DiscordGuild g = database.getGuildList().getGuild(e.getGuild().getId());
			g.setModRoleId(rolesWithName.get(0).getId());
			database.setGuildList(database.getGuildList().addGuild(g));
			CommandHandler.sendMessage(e, String.format("%s is now mod!", rolesWithName.get(0).getAsMention()),false);
		}
	}
	
	public static Message sendMessage(User user, TextChannel mChannel, String msg, boolean isPrivate) {
		Message sent = null;
		try {
			if (isPrivate) {
				user.openPrivateChannel().queue((channel) -> channel
						.sendMessage(msg).queue());
			} else {
				sent = mChannel.sendMessage(msg).complete();
			}
		} catch (InsufficientPermissionException | VerificationLevelException e) {
			user.openPrivateChannel().queue((channel) -> channel
					.sendMessage(String.format(
							"%s currently does not have permission to speak in %s, %s.\n"
									+ "If you feel this is a mistake, please contact the server administrator.",
									DiscordBot.jda.getSelfUser().getName(), mChannel.getGuild().getName(), mChannel.getName()))
					.queue());
		} catch (IllegalArgumentException e) {
			if (msg.isEmpty())
				sendMessage(user, mChannel, String.format("%s, I can't send an empty message!", user.getAsMention()),false);
			else
				sendMessage(user, mChannel, String.format("%s, the message is too long!", user.getAsMention()),false);
		}
		return sent;
	}
	
	public static Message sendMessage(User user, TextChannel mChannel, MessageEmbed eb, boolean isPrivate) {
		Message sent = null;
		try {
			if (isPrivate) {
				user.openPrivateChannel().queue((channel) -> channel
						.sendMessage(eb).queue());
			} else {
				sent = mChannel.sendMessage(eb).complete();
			}
		} catch (InsufficientPermissionException | VerificationLevelException e) {
			user.openPrivateChannel().queue((channel) -> channel
					.sendMessage(String.format(
							"%s currently does not have permission to speak in %s, %s.\n"
									+ "If you feel this is a mistake, please contact the server administrator.",
									DiscordBot.jda.getSelfUser().getName(), mChannel.getGuild().getName(), mChannel.getName()))
					.queue());
		} catch (IllegalArgumentException e) {
			if (eb.isEmpty())
				sendMessage(user, mChannel, String.format("%s, I can't send an empty message!", user.getAsMention()),false);
			else
				sendMessage(user, mChannel, String.format("%s, the message is too long!", user.getAsMention()),false);
		}
		return sent;
	}
	
	public static Message sendMessage(MessageReceivedEvent e, MessageEmbed eb, boolean isPrivate) {
		TextChannel destination;
		try {
			destination = e.getTextChannel();
		} catch (IllegalStateException ex) {
			isPrivate = true;
			destination = null;
		}
		return sendMessage(e.getAuthor(),destination,eb,isPrivate);
	}
	
	public static Message sendMessage(MessageReceivedEvent e, String msg, boolean isPrivate) {
		TextChannel destination;
		try {
			destination = e.getTextChannel();
		} catch (IllegalStateException ex) {
			isPrivate = true;
			destination = null;
		}
		return sendMessage(e.getAuthor(),destination,msg,isPrivate);
	}
	
	@Override
	public void onGuildJoin(GuildJoinEvent e) {
		database.getGuildList().getGuild(e.getGuild().getId());
		database.setGuildList(database.getGuildList());
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent e) { // runs when message is received

		String prefix = DiscordBot.database.getPrefix();
		String mention = DiscordBot.jda.getSelfUser().getAsMention();
		
		if (e.getAuthor().isBot())
			return; // quits if message came from bot
		
		// if(e.getChannelType() != ChannelType.TEXT) return;
		// uncomment if DM commands aren't allowed
		
		if (!e.getMessage().getContentRaw().startsWith(prefix)
				&& !e.getMessage().getContentRaw().startsWith(mention))
			return; // quits if message doesn't start with command prefix

		ArrayList<String> args;
		if (e.getMessage().getContentRaw().startsWith(prefix)) // message is chopped up into command and arguments
			args = new ArrayList<String>(
					Arrays.asList(e.getMessage().getContentRaw().substring(prefix.length()).trim().split(" ")));
		
		else if (e.getMessage().getContentRaw().startsWith(mention + " " + prefix))
			args = new ArrayList<String>(Arrays.asList(e.getMessage().getContentRaw()
					.substring(mention.length() + 1 + prefix.length()).trim().split(" ")));
		
		else
			args = new ArrayList<String>(
					Arrays.asList(e.getMessage().getContentRaw().substring(mention.length()).trim().split(" ")));

		String cmd = args.remove(0).toLowerCase(); // command is removed from the arguments and put into its own variable
		
		for (Module c : listeners) {												//searches all command modules
			final Method[] commandMethods = c.getClass().getDeclaredMethods(); 		// gets all of the methods in the class
			for (Method m : commandMethods) { 										// for each method in the class
				if (m.isAnnotationPresent(DiscordCommand.class)) { 						// if method is a command...
					String annotationName = m.getAnnotation(DiscordCommand.class).Name(); 	// gets the command name of the method
					ArrayList<String> Aliases = new ArrayList<String>(				// gets the other aliases of the method
							Arrays.asList(m.getAnnotation(DiscordCommand.class).Aliases()));
					if (annotationName.equals(cmd) || Aliases.contains(cmd)) { 		// if the method has the command we are looking for...
						Guild server;
						if (e.getChannelType() == ChannelType.TEXT) {
							server = e.getGuild();
						} else {	//if message did not come from a text channel, we need to find the guild associated with the command
							List<DiscordGuild> guildList = new ArrayList<DiscordGuild>();
							for (DiscordGuild guild : database.getGuildList().getGuildList())
								if (DiscordBot.jda.getGuildById(guild.getId()).getMember(e.getAuthor()) != null)
									guildList.add(guild);
							if (guildList.size() == 0) {
								sendMessage(e,"You are not a member of any guilds with this bot!",false);
								return;
							} else if (guildList.size() == 1) {
								server = DiscordBot.jda.getGuildById(guildList.get(0).getId());
							} else {
								if (args.get(args.size()-1).length() == 18) {
									try {
										server = DiscordBot.jda.getGuildById(args.remove(args.size()-1));
									} catch (NumberFormatException ex) {
										sendMessage(e,"That is not a proper guild ID!",false);
										return;
									}
								} else {
									sendMessage(e,"What guild is this command for? Please paste the guild id at the end of your command (or, just enter the command in your guild!).",false);
									return;
								}
							}
						}
						if (m.getAnnotation(DiscordCommand.class).SpecialPerms()) {
							boolean canGo = false;
							String modRoleId;
							modRoleId = database.getGuildList().getGuild(server.getId()).getModRoleId();
							if (modRoleId == null || modRoleId == "") {
								sendMessage(e,"`Warning:` No valid moderator role has been set. Please use .setmod as soon as possible!",false);
								canGo = true;
							} else if (server.getRoleById(modRoleId) == null) {
								sendMessage(e,"`Warning:` No valid moderator role has been set. Please use .setmod as soon as possible!",false);
								canGo = true;
							} else {
								for (Role r : server.getMember(e.getAuthor()).getRoles()) {
									if (r.getId().equals(modRoleId)) {
										canGo = true;
										break;
									}
								}
							}
							if (!canGo) {
								sendMessage(e,"You do not have permission to use this command!",false);
								return;
							}
						}
						Thread commandThread = new Thread() {
							public void run() {
								try {
									m.invoke(c, e, args, new MessageInfo(server));
								} catch (InvocationTargetException e1) {
									Throwable fe = e1.getCause();
									if (fe == null) fe = e1;
									fe.printStackTrace();
									sendMessage(e, 
											"`Error: Command crashed! This isn't supposed to happen :/" + 
											((e.getAuthor().getId().equals("215507031375740928")) ? 
											(System.lineSeparator() + "Since you're the Bot Tech, here's the error:" + System.lineSeparator() + fe.toString()) : "") + 
											"`", 
											false);
								} catch (Exception e2) {
									e2.printStackTrace();
									sendMessage(e, "`Critical: CommandHandler Failed!`", false);
								}
							}
						};
						commandThread.start();
						return;
						
					}
				}
			}
		}

		sendMessage(e,String.format("Sorry %s, I do not recognize this command.", e.getAuthor().getAsMention()), false);
	}
	
	public static void volatileMessage(Message msg, int seconds) {
		new Thread(() -> {
			long endTime = seconds*1000 + System.currentTimeMillis();
			while (endTime > System.currentTimeMillis()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			msg.delete().complete();
		}).start();
	}
	
	@Override
	public void initialize() {
		try {
			database = DiscordBot.readDatabase(CommandHandlerDatabase.class);
		} catch (SAXException e1) {
			e1.printStackTrace();
		}
		if (database == null)
			database = DiscordBot.writeDatabase(new CommandHandlerDatabase());
		
		for (Guild gu : DiscordBot.jda.getGuilds()) {
			database.getGuildList().getGuild(gu.getId());
			database.setGuildList(database.getGuildList());
		}
		
		DiscordBot.jda.addEventListener(this);
		addDependants(this);
	}

	@Override
	public void shutdown() {
		DiscordBot.jda.removeEventListener(this);
		synchronized (lock) {
			listeners = null;
		}
	}
	
	@Override
	public String getName() {
		return MODULE_NAME;
	}

	@Override
	public void addDependants(Module toAdd) {
		synchronized (lock) {
			listeners.add(toAdd);
		}
	}

	@Override
	public void removeDependants(Module toRemove) {
		synchronized (lock) {
			listeners.remove(toRemove);
		}
	}

	@Override
	public List<Module> getDependants() {
		synchronized (lock) {
			return listeners;
		}
	}

	@Override
	public List<String> getDependencies() {
		return null;
	}
}