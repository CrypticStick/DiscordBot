package com.Stickles.Discord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.Stickles.DiscordBot;
import com.Stickles.Discord.DiscordCommand;
import com.Stickles.ModularCoding.Module;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AFJROTCCommands extends ListenerAdapter implements Module {

	final String MODULE_NAME = "AFJROTC Commands";
	final List<String> DEPENDENCIES = Arrays.asList("Command Handler");
	
	private HashMap<String,String> requestBuffer = new HashMap<String,String>();
	private HashMap<String,MessageReactionAddEvent> requestQueue = new HashMap<String,MessageReactionAddEvent>();
	
	@Override
	public void onMessageReceived(MessageReceivedEvent e) {
		if (e.getAuthor().isBot()) return;
		if (e.getChannelType() == ChannelType.PRIVATE && requestBuffer.containsKey(e.getAuthor().getId())) {
			requestBuffer.put(e.getAuthor().getId(), e.getMessage().getContentStripped());
		}
	}
	
	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent e) {
		if (e.getUser().isBot()) return;
		if (requestQueue.containsKey(e.getMessageId())) {
			requestQueue.put(e.getMessageId(), e);
		}
	}
	
	public static int distance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        int [] costs = new int [b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }
	
	public String getClosestValueInArray(String[] array, String value) {
		int closestIndex = -1;
		int closestDist = 999999999;
		for (int i = 0; i < array.length; i++) {
			int dist = distance(array[i].toLowerCase(),value.toLowerCase());
			if (dist < closestDist) {
				closestIndex = i;
				closestDist = dist;
			}
		}
		if (closestDist > 4) return null;
		else return array[closestIndex];
	}
	
	@DiscordCommand(Name = "request",
			Summary = "Register account with server (Mandatory!)"
			)
	public void uhhhYeahIdLikeToRegister(MessageReceivedEvent e, ArrayList<String> args, MessageInfo info) {
		requestBuffer.put(e.getAuthor().getId(), "");
		CommandHandler.sendMessage(e,"Welcome to Yakkie's AFJROTC Discord Registration!\nPlease Answer the following questions:",true);
		String[] answers = {"N/A","N/A","N/A","N/A","N/A","N/A"};
		String requestAnswer = "";
		EmbedBuilder eb = null;
		int questionIndex = 0;
		boolean fixing = false;
		boolean done = false;
		while (!done) {
			switch (questionIndex) {
			case 0:
				CommandHandler.sendMessage(e,"What is your full name? (Ex. John Smith)",true);
				break;
			case 1:
				CommandHandler.sendMessage(e,"What is your affiliation? (Student, Parent, Alumni)",true);
				break;
			case 2:
				CommandHandler.sendMessage(e,"What flight are you in? (e.g. Alpha, Bravo, Charlie)",true);
				break;
			case 3:
				CommandHandler.sendMessage(e,"What rank do you have? (e.g. Airman Basic, Airman, Airman First Class)",true);
				break;
			case 4:
				CommandHandler.sendMessage(e,"What teams (if any) are you part of? (e.g. Cyberpatriot, Color Guard, Unarmed Drill)",true);
				break;
			case 5:
				CommandHandler.sendMessage(e,"What staff positions (if any) do you have? (e.g. Finance, Public Affairs, Recruiting)",true);
				break;
			case 6:
				CommandHandler.sendMessage(e,"Please confirm the following information is correct (type `yes`, `cancel`, or the number of the entry you would like to modify):",true);
				eb = new EmbedBuilder();
				eb.setTitle("AFJROTC Profile Request Form");
				eb.addField("1. Full Name", answers[0], false);
				eb.addField("2. Affiliation", answers[1], false);
				eb.addField("3. Flight", answers[2], false);
				eb.addField("4. Rank", answers[3], false);
				eb.addField("5. Team", answers[4], false);
				eb.addField("6. Staff Positions", answers[5], false);
				CommandHandler.sendMessage(e, eb.build(), true);
				break;
			}
			
			while (requestBuffer.get(e.getAuthor().getId()).contentEquals("")) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			
			requestAnswer = requestBuffer.get(e.getAuthor().getId()).trim();
			requestBuffer.put(e.getAuthor().getId(),"");
			switch (questionIndex) {
			case 0:
				if (requestAnswer.contains(" ")) {
					answers[0] = requestAnswer;
					if (fixing) questionIndex = 6;
					else questionIndex++;
				}
				break;
			case 1:
				answers[1] = getClosestValueInArray(Constants.affiliations,requestAnswer);
				if (answers[1] != null) {
					if(!answers[1].contentEquals("Student")) {
						questionIndex = 6;
						answers[2] = "N/A";
						answers[3] = "N/A";
						answers[4] = "N/A";
						answers[5] = "N/A";
					} else {
						if (fixing) questionIndex = 6;
						else questionIndex++;
					}
				}
				break;
			case 2:
				answers[2] = getClosestValueInArray(Constants.flights,requestAnswer);
				if (answers[2] != null)
					if (fixing) questionIndex = 6;
					else questionIndex++;
				break;
			case 3:
				String rShort = getClosestValueInArray(Constants.ranksShort,requestAnswer);
				String rFull = getClosestValueInArray(Constants.ranksFull,requestAnswer);
				if (rShort != null && rFull != null) answers[3] = getClosestValueInArray(new String[] {answers[3],rFull},requestAnswer);
				else if (rShort == null) answers[3] = rFull;
				else answers[3] = Constants.ranksFull[Arrays.asList(Constants.ranksShort).indexOf(answers[3])];
				if (answers[3] != null)
					if (fixing) questionIndex = 6;
					else questionIndex++;
				break;
			case 4:
				answers[4] = getClosestValueInArray(Constants.teams,requestAnswer);
				if (answers[4] != null)
					if (fixing) questionIndex = 6;
					else questionIndex++;
				break;
			case 5:
				answers[5] = getClosestValueInArray(Constants.staff,requestAnswer);
				if (answers[5] != null)
					if (fixing) questionIndex = 6;
					else questionIndex++;
				break;
			case 6:
				if (requestAnswer.toLowerCase().contentEquals("cancel")) {
					CommandHandler.sendMessage(e,"Cancelled the request.",true);
					requestBuffer.remove(e.getAuthor().getId());
					return;
				}
				if (requestAnswer.toLowerCase().contentEquals("yes")) {
					CommandHandler.sendMessage(e,"Thank you! A server moderator will confirm your request shortly.",true);
					requestBuffer.remove(e.getAuthor().getId());
					profileRequest(e, info, eb);
					return;
				}
				try {
					int potIndex = Integer.parseInt(requestAnswer);
					if (potIndex > 0 && potIndex < 7)
						fixing = true;
						questionIndex = potIndex-1;
				} catch (NumberFormatException ex) {
					//oh well...
				}
				break;
			}
		}
	}
	
	private void profileRequest(MessageReceivedEvent e, MessageInfo info, EmbedBuilder eb) {
		if (info.getGuild().getTextChannelsByName("requests", true).size() == 0)
			info.getGuild().createTextChannel("requests").complete();
		TextChannel requestChannel = info.getGuild().getTextChannelsByName("requests", true).get(0);
		CommandHandler.sendMessage(e.getAuthor(), requestChannel, "A new profile request has been submitted:", false);
		Message request = CommandHandler.sendMessage(e.getAuthor(), requestChannel, eb.build(), false);
		request.addReaction(DiscordEmojis.check).queue();
		request.addReaction(DiscordEmojis.cross).queue();
		requestQueue.put(request.getId(), null);
		
		MessageReactionAddEvent response = null;
		while (response == null) {
			response = requestQueue.get(request.getId());
			if (response != null) {
				if (response.getReactionEmote().getEmoji().contentEquals(DiscordEmojis.check) || 
						response.getReactionEmote().getEmoji().contentEquals(DiscordEmojis.cross)) {
					break;
				} else {
					response.getReaction().removeReaction(response.getUser()).queue();
					response = null;
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		requestQueue.remove(request.getId());
		
		if (response.getReactionEmote().getEmoji().contentEquals(DiscordEmojis.cross)) {
			CommandHandler.sendMessage(e.getAuthor(), requestChannel, "Request not approved! (by " + response.getUser().getAsMention() + ")", false);
			return;
		}

		CommandHandler.sendMessage(e.getAuthor(), requestChannel, "Request approved! (by " + response.getUser().getAsMention() + ")", false);
		e.getMember().modifyNickname(eb.getFields().get(0).getValue());
		//e.getGuild().addRoleToMember(e.getMember(), role);
		//e.getGuild().addRoleToMember(e.getMember(), role);
		//e.getGuild().addRoleToMember(e.getMember(), role);
		//e.getGuild().addRoleToMember(e.getMember(), role);
		//e.getGuild().addRoleToMember(e.getMember(), role);
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
		DiscordBot.jda.addEventListener(this);
	}

	@Override
	public void shutdown() {
		DiscordBot.jda.removeEventListener(this);
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
