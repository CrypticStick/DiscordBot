package com.Stickles;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.security.auth.login.LoginException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.Stickles.Discord.Database;
import com.Stickles.Discord.DiscordBotDatabase.DiscordBotDatabase;
import com.Stickles.ModularCoding.DynamicModuleLoader;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Activity.ActivityType;

public class DiscordBot extends DynamicModuleLoader {
	
	public static JDA jda;
	public static File configDir = new File("config/");
	public static File logDir = new File("log/");
	public static File logFile = new File(logDir.toString() + "/log.txt");
	public static DiscordBotDatabase database;
	static Console c;

	public static void main(String[] args) throws InterruptedException {
		
		if (!configDir.exists())	//create config folder if needed
			configDir.mkdir();
		
		try {
			database = readDatabase(DiscordBotDatabase.class);
		} catch (SAXException e1) {
			e1.printStackTrace();
		}
		if (database == null)
			database = writeDatabase(new DiscordBotDatabase());
		
		if (database.getToken().isEmpty()) {
			System.err.println("Error: token is invalid! Please edit the token in the config.");
			throw new InterruptedException("Configuration failed.");	//crash if configuration fails
		}
		
		System.out.println("Starting up bot...");
		
		c = System.console();	//attempt to get console
		if (c == null) {
			System.err.println("No console.");
			System.err.println("This bot will continue to run, but no commands can be entered from this window.");
		}
		
		try {
		jda = new JDABuilder(AccountType.BOT)	//configure and start the bot
				.setToken(database.getToken()).setStatus(OnlineStatus.ONLINE)
				.setActivity(Activity.of(ActivityType.DEFAULT, String.format("%s | %shelp", database.getGame(), database.getPrefix())))
				.build();
		} catch (LoginException e) {
			System.err.println("Error: the bot failed to connect. "
					+ "This could either be because the token is incorrect, "
					+ "there's no internet, or because Stickles completely broke the code :P");
			throw new InterruptedException("Connection failed.");	//crash if login fails
		}

		while (jda.getStatus() != Status.CONNECTED) { // Wait for connection
				System.out.println(jda.getStatus().toString());
				Thread.sleep(500);
		}
		
		setModuleDirectory("modules/");
		try {
			loadAllModules();
			startWatcher();
		} catch (NoModuleDirException e) {
			e.printStackTrace();
			System.err.println("Error: modules failed to load.");
			throw new InterruptedException("DML Failure.");	//crash if DML fails
		}

		if (c != null) {	//start terminal code if console available
			localTerminal();
		}
	}
	
	public static <T extends Database> T writeDatabase(T database) {
        try {
        	String name = database.getClass().getName();
        	int index = name.lastIndexOf('.')+1;
        	File file = new File(configDir.toString() + String.format("/%s.xml",name.substring(index)));	//creates file name
            if (!file.exists()) {	//if no database exists, make a new one
            	file.createNewFile();
            }
            
        	JAXBContext jaxbContext = JAXBContext.newInstance(database.getClass());
        	Marshaller marshaller = jaxbContext.createMarshaller();
        	marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        	marshaller.marshal(database, file);
        } catch (JAXBException | IOException ex) {
        	ex.printStackTrace();
        }
		return database;
	}
	
	@SuppressWarnings("deprecation")
	public static <T extends Database>  T readDatabase(Class<T> klass) throws SAXParseException {
    	String name = klass.getName();
    	int index = name.lastIndexOf('.')+1;
    	File file = new File(configDir.toString() + String.format("/%s.xml",name.substring(index)));	//creates file name
		try {
            if (!file.exists()) {	//if no database exists, make a new one
            	file.createNewFile();
            	return writeDatabase(klass.newInstance());
            }
            
            JAXBContext jaxbContext = JAXBContext.newInstance(klass);    
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();    
            
            T database = klass.cast(jaxbUnmarshaller.unmarshal(file));   
            return database;
            
          } catch (JAXBException ex) {
        	  try {
				return writeDatabase(klass.newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
          } catch (IOException | IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
          }
		
		return null;
	}
	
	static void localTerminal() {
		while (true) {	
			try {
				jda.awaitStatus(Status.CONNECTED);
			
				String[] command = c.readLine(String.format("%s>", jda.getSelfUser().getName())).split(" ");
		
				switch (command[0]) {
		
				case "quit":
					jda.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB,
							Activity.of(ActivityType.DEFAULT, "Shutting down..."));
					System.out.println("Shutting down...\n");
					System.exit(0);
					break;
					
				case "load":
					if (command.length < 2) {
						System.out.println("Please specify which file you would like to load.");
						break;
					}
					
					if (command[1].toLowerCase().equals("all")) {
						loadAllModules();
						break;
					}
					
					File toLoad;
					try {
						toLoad = new File("modules/" + command[1] + ".jar").getCanonicalFile();
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
					
					if (!toLoad.exists())  {
						System.out.println(String.format("Module \"%s.jar\" does not exist.", command[1]));
						break;
					}
					else {
						boolean loaded = loadJar(toLoad, true);
						if (loaded)
							System.out.println(String.format("Module \"%s.jar\" has been loaded.", command[1]));
						else
							System.out.println(String.format("Module \"%s.jar\" failed to load!", command[1]));
					}
					break;
					
				case "reload":
					if (command.length < 2) {
						System.out.println("Please specify which file you would like to reload.");
						break;
					}
					
					if (command[1].toLowerCase().equals("all")) {
						reloadAllModules();
						break;
					}
					
					File toReload;
					try {
						toReload = new File("modules/" + command[1] + ".jar").getCanonicalFile();
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
					
					if (!toReload.exists())  {
						System.out.println(String.format("Module \"%s.jar\" does not exist.", command[1]));
						break;
					}
					else {
						unloadJar(toReload);
						boolean reloaded = loadJar(toReload, true);
						if (reloaded)
							System.out.println(String.format("Module \"%s.jar\" has been loaded.", command[1]));
						else
							System.out.println(String.format("Module \"%s.jar\" failed to load!", command[1]));
					}
					break;
					
				case "unload":
					if (command.length < 2) {
						System.out.println("Please specify which file you would like to unload.");
						break;
					}
					
					if (command[1].toLowerCase().equals("all")) {
						unloadAllModules();
						break;
					}
					
					File toUnload;
					try {
						toUnload = new File("modules/" + command[1] + ".jar").getCanonicalFile();
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
					
					if (!toUnload.exists())  {
						System.out.println(String.format("Module \"%s.jar\" does not exist.", command[1]));
						break;
					}
					else {
						boolean unloaded = unloadJar(toUnload);
						if (unloaded)
							System.out.println(String.format("Module \"%s.jar\" has been unloaded.", command[1]));
						else
							System.out.println(String.format("Module \"%s.jar\" failed to unload!", command[1]));
					}
					break;
					
				case "modules":
					StringBuilder sb = new StringBuilder();
					sb.append(System.lineSeparator());
					
					sb.append("Modules currently loaded:" + System.lineSeparator());
					ArrayList<String> loaded = getLoadedJars();
					if (loaded.isEmpty())
						sb.append("None!" + System.lineSeparator());
					else {
						for (String module : loaded)
							sb.append(module + System.lineSeparator());
					}
					
					sb.append(System.lineSeparator());
					
					sb.append("Modules currently unloaded:" + System.lineSeparator());
					ArrayList<String> unloaded = getUnloadedJars();
					if (unloaded.isEmpty())
						sb.append("None!" + System.lineSeparator());
					else {
						for (String module : unloaded)
							sb.append(module + System.lineSeparator());
					}
					
					System.out.println(sb.toString());
					break;
					
				default:
					System.out.println(String.format("command '%s' not recognized.", command[0]));
					
				}		
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (NoModuleDirException e1) {
				e1.printStackTrace();
			}
		}
			
	}
}
