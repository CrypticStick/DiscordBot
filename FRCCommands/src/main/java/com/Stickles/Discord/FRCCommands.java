package com.Stickles.Discord;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.Stickles.Discord.DiscordCommand;
import com.Stickles.ModularCoding.Module;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class FRCCommands implements Module {

	final String MODULE_NAME = "FRC Commands";
	final List<String> DEPENDENCIES = Arrays.asList("Command Handler");

	static String HttpBlueAllianceGet(String url) {       
	    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
	        
            HttpGet request = new HttpGet(url);
            request.setHeader("Content-type", "application/json");
	        request.addHeader("X-TBA-Auth-Key", "0NiCsg5pJzCGOVmZTbYk0LdTZOXcDMIQJKThzoqIVBuEWSZ5dXbtTouAspaayL5B");
            HttpResponse response = client.execute(request);

            BufferedReader bufReader = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));

            StringBuilder builder = new StringBuilder();

            String line;

            while ((line = bufReader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }

            return builder.toString();
        } catch (UnsupportedOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return "";
	}

	@DiscordCommand(Name = "team", 
			Summary = "Returns information about FRC teams")
	public static void javascriptMORElikeJAVAisBETTERwahhaahaha(MessageReceivedEvent e, ArrayList<String> args) {
		int teamNumber = -1;
		int switchArg = -1;
		for (int i = 0; i < args.size(); i++)
		try {
			teamNumber = Integer.parseInt(args.get(i));
			switchArg = i;
		} catch (Exception ex) {
		}
		
		if (teamNumber == -1 || args.size() == 0) {
			CommandHandler.sendMessage(e,"Please specify a team number.",false);
			return;
		}
		
		if (switchArg == 0)
			switchArg = 1;
		else 
			switchArg = 0;
		
		JSONObject info;
		StringBuilder list;
		String value = "info";
		
		if (args.size() > 1)
			value = args.get(switchArg);
		
		switch (value) {
			case "awards" :
				info = new JSONObject(HttpBlueAllianceGet(
						String.format("http://www.thebluealliance.com/api/v3/team/frc%s/awards",teamNumber)));
				int total = info.get().length;
				list = new StringBuilder();
				list.append(String.format("**Team %s's Awards**%s", teamNumber,System.lineSeparator()));
				for (int i=0; i < total; i++)
					list.append(String.format("%s - %s %s",info.get(String.valueOf(i),"year")[0],info.get(String.valueOf(i),"name")[0],System.lineSeparator()));
				CommandHandler.sendMessage(e,list.toString(),false);
				break;
			case "info" :
				info = new JSONObject(HttpBlueAllianceGet(
						String.format("http://www.thebluealliance.com/api/v3/team/frc%s",teamNumber)));
				if (info.get("team_number")[0].isEmpty()) {
					CommandHandler.sendMessage(e,String.format("Sorry, Team '%s' doesn't seem to exist.",teamNumber),false);
					return;
				}
				
				EmbedBuilder eb = new EmbedBuilder();
				eb.setColor(Color.red);
				eb.setDescription(String.format("**Team %s's Info**", teamNumber));
				
				eb.addField("Nickname", info.get("nickname")[0], true);
				eb.addField("Name", (info.get("name")[0].equals("null")) ? "N/A" : info.get("name")[0], true);
				eb.addField("City", (info.get("city")[0].equals("null")) ? "N/A" : info.get("city")[0], true);
				eb.addField("Country", (info.get("country")[0].equals("null")) ? "N/A" : info.get("country")[0], true);
				eb.addField("State/Providence", (info.get("state_prov")[0].equals("null")) ? "N/A" : info.get("state_prov")[0], true);
				eb.addField("Address", (info.get("address")[0].equals("null")) ? "N/A" : info.get("address")[0], true);
				eb.addField("Google Maps", (info.get("gmaps_url")[0].equals("null")) ? "N/A" : info.get("gmaps_url")[0], false);
				eb.addField("Website", (info.get("website")[0].equals("null")) ? "N/A" : info.get("website")[0], false);
				eb.addField("Rookie Year", (info.get("rookie_year")[0].equals("null")) ? "N/A" : info.get("rookie_year")[0], false);
				eb.addField("Motto", (info.get("motto")[0].equals("null")) ? "N/A" : info.get("motto")[0], false);

				CommandHandler.sendMessage(e,eb.build(),false);
				break;
			default :
				CommandHandler.sendMessage(e,String.format("Sorry, I have no information on '%s'",args.get(switchArg)),false);
				break;
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
