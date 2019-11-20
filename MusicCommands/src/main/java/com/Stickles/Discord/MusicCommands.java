package com.Stickles.Discord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.xml.sax.SAXException;

import com.Stickles.DiscordBot;
import com.Stickles.Discord.DiscordCommand;
import com.Stickles.Discord.GuildMusicManager.MusicMode;
import com.Stickles.Discord.GuildMusicManager.UserData;
import com.Stickles.Discord.MusicCommandsDatabase.DiscordGuild;
import com.Stickles.Discord.MusicCommandsDatabase.MusicCommandsDatabase;
import com.Stickles.ModularCoding.Module;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

public class MusicCommands implements Module {

	final String MODULE_NAME = "Music Commands";
	final List<Class<? extends Module>> DEPENDENCIES = Arrays.asList(CommandHandler.class);
	
	private static AudioPlayerManager playerManager;
	private static Map<Long, GuildMusicManager> musicManagers;
	static MusicCommandsDatabase database = null;
	  
	private synchronized static GuildMusicManager getGuildAudioPlayer(TextChannel tc) {
		  long guildId = tc.getGuild().getIdLong();
		  GuildMusicManager musicManager = musicManagers.get(guildId);

		  if (musicManager == null) {
		    musicManager = new GuildMusicManager(playerManager,tc);
		    musicManagers.put(guildId, musicManager);
		  }

		  tc.getGuild().getAudioManager().setSendingHandler(musicManager.getSendHandler());
		  return musicManager;
	}
	
	public static String millisToTime(long millis) {
		long m_millis = millis;
		long hours = TimeUnit.MILLISECONDS.toHours(m_millis);
		m_millis -= TimeUnit.HOURS.toMillis(hours);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(m_millis);
		m_millis -= TimeUnit.MINUTES.toMillis(minutes);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(m_millis);
		
		return String.format("%02d:%02d:%02d", 
				hours,
			    minutes,
			    seconds
			);
	}
	
	  private void loadAndPlay(final MessageReceivedEvent e, final String trackUrl) {
		  GuildMusicManager musicManager = getGuildAudioPlayer(e.getTextChannel());
		  playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
		      @Override
		      public void trackLoaded(AudioTrack track) {
		    	  play(e, musicManager, track);
		    	  CommandHandler.sendMessage(
		    			  e,
		    			  String.format("Adding `%s` (%s) to queue.", track.getInfo().title, (track.getInfo().isStream) ? "Stream" : millisToTime(track.getInfo().length)),
		    			  false);
		      }

		      @Override
		      public void playlistLoaded(AudioPlaylist playlist) {
		        AudioTrack firstTrack = playlist.getSelectedTrack();
		        if (firstTrack == null) {
		          firstTrack = playlist.getTracks().get(0);
		        }
		        
		        if (playlist.getTracks().size() <= 1 || playlist.isSearchResult()) {
			    	  play(e, musicManager, firstTrack);
			    	  CommandHandler.sendMessage(
			    			  e,
			    			  String.format("Adding `%s` (%s) to queue.", firstTrack.getInfo().title, (firstTrack.getInfo().isStream) ? "Stream" : millisToTime(firstTrack.getInfo().length)),
			    			  false);
		        } else {
			        long totalTime = 0;
			        for (int i = 0; i < playlist.getTracks().size(); i++) {
			        	totalTime += playlist.getTracks().get(i).getInfo().length;
			        }
			        for (int i = 0; i < playlist.getTracks().size(); i++) {
			        	play(e, musicManager, playlist.getTracks().get(i));
			        }
			        CommandHandler.sendMessage(
			        		e,
			        		String.format("Adding playlist `%s` (%s) to queue.", playlist.getName(), millisToTime(totalTime)),
			    			false); 
		        }
		        
		      }

		      @Override
		      public void noMatches() {
		    	  CommandHandler.sendMessage(
		    			  e,
		    			  String.format("There are no results for `%s`.", trackUrl),
		    			  false);
		      }

		      @Override
		      public void loadFailed(FriendlyException exception) {
		    	  CommandHandler.sendMessage(
		    			  e,
		    			  String.format("Could not play `%s`: %s", trackUrl, exception.getMessage()),
		    			  false);
		      }
		    });
		  }

	private void play(MessageReceivedEvent e, GuildMusicManager musicManager, AudioTrack track) {
		connectToMemberVoiceChannel(e.getTextChannel(),e.getAuthor());
		musicManager.scheduler.queue(track, e.getAuthor());
	}

	static boolean connectToMemberVoiceChannel(TextChannel tc, User u) {
		if (!memberInVoiceChannel(tc,u,false)) return false;
		VoiceChannel vc = tc.getGuild().getMember(u).getVoiceState().getChannel();
		VoiceChannel bvc = tc.getGuild().getSelfMember().getVoiceState().getChannel();
		if (vc.equals(bvc))
			return false;
		AudioManager audioManager = tc.getGuild().getAudioManager();
		if (!tc.getGuild().getAudioManager().isConnected() && !audioManager.isAttemptingToConnect()) {
			setTextChannel(tc);
			audioManager.openAudioConnection(vc);
			//getGuildAudioPlayer(tc).resetPlayerMessage(false);
			return true;
		}
		return false;
	}
	
	static void leaveVoiceChannel(long guildId, boolean wipeTrackList, String reason) {
		AudioManager audioManager = DiscordBot.jda.getGuildById(guildId).getAudioManager();
		TextChannel tc = musicManagers.get(guildId).textChannel;
		if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
			musicManagers.get(guildId).player.setPaused(true);
			audioManager.closeAudioConnection();
		}
		if (wipeTrackList) {
			destroyPlayerManager(guildId);
		}
		CommandHandler.sendMessage(DiscordBot.jda.getSelfUser(), tc, reason, false);
	}
	
	static void destroyPlayerManager(long guildId) {
		GuildMusicManager byebye = musicManagers.remove(guildId);
		if (byebye != null) {
			DiscordBot.jda.removeEventListener(byebye);
			byebye.destroyed = true;
			byebye.deletePlayerMessage();
			byebye.player.destroy();
			byebye = null;
		}
	}
	
	static boolean inCorrectTextChannel(TextChannel tc, User u, boolean requiresPlayer) {
		if (musicManagers.get(tc.getGuild().getIdLong()) == null && requiresPlayer) {
			CommandHandler.sendMessage(u,tc, String.format("%s, I haven't started a player yet!", u.getAsMention()),false);
			return false;	
		}
		if (tc.getGuild().getSelfMember().getVoiceState().inVoiceChannel()) {
			if (tc != getGuildAudioPlayer(tc).textChannel) {
				CommandHandler.sendMessage(u,tc, String.format("%s, please use %s for music commands!", u.getAsMention(),getGuildAudioPlayer(tc).textChannel.getAsMention()),false);
				return false;
			}
		} else {
			setTextChannel(tc);
		}
		return true;
	}
	
	static void setTextChannel(TextChannel tc) {
		getGuildAudioPlayer(tc).setTextChannel(tc);
	}
	
	static VoiceChannel botVoiceChannel(TextChannel tc) {
		return tc.getGuild().getSelfMember().getVoiceState().getChannel();
	}
	
	static boolean memberInVoiceChannel(TextChannel tc, User u, boolean withBot) {
		GuildVoiceState uvs = tc.getGuild().getMember(u).getVoiceState();
		if (!uvs.inVoiceChannel()) {
			CommandHandler.sendMessage(
					u,
					tc, 
					String.format("%s, please join %s voice channel before using this command!", u.getAsMention(),(withBot && botVoiceChannel(tc) != null) ? "my" : "a"),
					false
					);
			return false;
		}
		if (withBot && botVoiceChannel(tc) != null) {
			if (!uvs.getChannel().equals(botVoiceChannel(tc))) {
				CommandHandler.sendMessage(u,tc, String.format("%s, please join my voice channel before using this command!", u.getAsMention()),false);
				return false;
			}
		}
		return true;
	}
	
	static boolean allowedToUsePlayerCommands(TextChannel tc, User u, boolean botMustBeInCall) {
		if (!inCorrectTextChannel(tc,u,true)) return false;
		if (botVoiceChannel(tc) == null && botMustBeInCall) {
			CommandHandler.sendMessage(u,tc, String.format("%s, I'm not in a voice channel!", u.getAsMention()),false);
			return false;
		}
		if (!memberInVoiceChannel(tc,u,botMustBeInCall)) return false;
		boolean canRunCommand = false;
		  if (getGuildAudioPlayer(tc).player.getPlayingTrack() != null)
			  if (getGuildAudioPlayer(tc).player.getPlayingTrack().getUserData(UserData.class) != null)
				  if (u.getId().equals(getGuildAudioPlayer(tc).player.getPlayingTrack().getUserData(UserData.class).getUser().getId()))
					  canRunCommand = true;
		  String djRole = MusicCommands.database.getGuildList().getGuild(tc.getGuild().getId()).getDJRoleId();
		  if (djRole == null) {
			  CommandHandler.sendMessage(u, tc,"Warning: Please set the DJ role with `.setDJ`!", false);
			  canRunCommand = true;
		  } else
			  for (Role role : tc.getGuild().getMember(u).getRoles()) {
				  if (role.getId().equals(djRole)) {
					  canRunCommand = true;
				  		break;
				  }
			  }
		if (!canRunCommand)
			CommandHandler.sendMessage(u, tc,String.format("%s, you are not DJ!", u.getAsMention()), false);
		  
		return canRunCommand;
	}

	@DiscordCommand(Name = "join",
			Summary = "I will join your music channel and set a new text channel!")
	public void timeTojoinaNEWGANG(MessageReceivedEvent e, ArrayList<String> args) {
		if (!memberInVoiceChannel(e.getTextChannel(),e.getAuthor(),false)) return;
		connectToMemberVoiceChannel(e.getTextChannel(),e.getAuthor());
	}
	
	@DiscordCommand(Name = "leave",
			Summary = "I will leave your music channel!")
	public void timeToGoAlone0n0(MessageReceivedEvent e, ArrayList<String> args) {
		if (!inCorrectTextChannel(e.getTextChannel(),e.getAuthor(),true)) return;
		if (!memberInVoiceChannel(e.getTextChannel(),e.getAuthor(),true)) return;
		if (botVoiceChannel(e.getTextChannel()) != null)
			leaveVoiceChannel(e.getGuild().getIdLong(),false,"Leaving voice call!");
		else
			CommandHandler.sendMessage(e, String.format("%s, I'm not in a voice channel!", e.getAuthor().getAsMention()),false);
	}
	
	@DiscordCommand(Name = "quit",
			Summary = "I will end the player and leave.")
	public void STOPNOWquitquitLEAVE(MessageReceivedEvent e, ArrayList<String> args) {
		if (!inCorrectTextChannel(e.getTextChannel(),e.getAuthor(),true)) return;
		if (!memberInVoiceChannel(e.getTextChannel(),e.getAuthor(),true)) return;
		if (botVoiceChannel(e.getTextChannel()) != null)
			leaveVoiceChannel(e.getGuild().getIdLong(),true,"Leaving voice call and closing player!");
		else
			leaveVoiceChannel(e.getGuild().getIdLong(),true,"Closing player!");
	}
	
	@DiscordCommand(Name = "play",
			Summary = "I will play the song you request!")
	public void nowTHATswhatICALLmusic911(MessageReceivedEvent e, ArrayList<String> args) {
		if (!inCorrectTextChannel(e.getTextChannel(),e.getAuthor(),false)) return;
		if (!memberInVoiceChannel(e.getTextChannel(),e.getAuthor(),true)) return;
		getGuildAudioPlayer(e.getTextChannel()).player.setPaused(false);
		if (args.size() < 1) {
			connectToMemberVoiceChannel(e.getTextChannel(),e.getAuthor());
			return;
		}
		String song = String.join(" ", args);
		loadAndPlay(e, (song.startsWith("http:") || song.startsWith("https:")) ? song : "ytsearch:" + song);
	}
	
	@DiscordCommand(Name = "pause",
			Summary = "Pauses the current track.")
	public void guessIllstopthen(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) return;
		getGuildAudioPlayer(e.getTextChannel()).player.setPaused(true);
	}
	
	@DiscordCommand(Name = "volume",
			Summary = "Sets the volume of the player.")
	public void itsTimetochangeVOLUME(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) return;
		if (args.size() == 0) {
			CommandHandler.sendMessage(e, String.format("%s, please type what volume you want (0-100)!", e.getAuthor().getAsMention()),false);
			return;
		}
		int volume;
		try {
			volume = Integer.parseInt(args.get(0));
		} catch (NumberFormatException ex) {
			CommandHandler.sendMessage(e, String.format("%s, please type a number for volume (0-100)!", e.getAuthor().getAsMention()),false);
			return;
		}
		volume = (volume < 0) ? 0 : (volume > 100) ? 100 : volume;
		getGuildAudioPlayer(e.getTextChannel()).player.setVolume(volume);
		CommandHandler.sendMessage(e, String.format("Volume is now %s%%!", getGuildAudioPlayer(e.getTextChannel()).player.getVolume()),false);
	}
	
	@DiscordCommand(Name = "back",
			Summary = "Attempts to play the last track (only works in Loop mode)")
	public void letsgoBackILikedthatOneSong(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) return;
		getGuildAudioPlayer(e.getTextChannel()).scheduler.playLastTrack();
	}
	
	@DiscordCommand(Name = "skip",
			Summary = "Skips the current track.")
	public void skIPITIDontWANTit(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) {
			if (memberInVoiceChannel(e.getTextChannel(),e.getAuthor(),true))
				getGuildAudioPlayer(e.getTextChannel()).addVoteSkip(e.getAuthor());
			return;
		}
		getGuildAudioPlayer(e.getTextChannel()).scheduler.nextTrack();
	}
	
	@DiscordCommand(Name = "shuffle",
			Summary = "Toggles the current shuffle mode.")
	public void letsSHAKEthingsUPaLittle(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) return;
		getGuildAudioPlayer(e.getTextChannel()).scheduler.toggleShuffleMode();
	}
	
	@DiscordCommand(Name = "loop",
			Summary = "Toggles the current loop mode.")
	public void looptyLOOPandPULL(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) return;
		getGuildAudioPlayer(e.getTextChannel()).scheduler.toggleLoopMode();
	}
	
	@DiscordCommand(Name = "loopone",
			Summary = "Toggles whether or not to only play the current track.")
	public void IreallyLIKEthisTrack(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) return;
		getGuildAudioPlayer(e.getTextChannel()).scheduler.toggleLoopOneMode();
	}
	
	@DiscordCommand(Name = "normal",
			Summary = "Changes the sound back to normal.")
	public void itdoesntSOUNDrightFIXit(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) return;
		getGuildAudioPlayer(e.getTextChannel()).updateMusicMode(MusicMode.Normal);

	}
	
	@DiscordCommand(Name = "nightcore",
			Summary = "MAKE THE MUSIC SOUND SUPER EPIC")
	public void NIGHTCOREbetterthanFORTNITE(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) return;
		getGuildAudioPlayer(e.getTextChannel()).updateMusicMode(MusicMode.Nightcore);
	}
	
	@DiscordCommand(Name = "bassboost",
			Summary = "THUD THUD THUD")
	public void somebodyCALL911ItsBASSBOOSTED(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),true)) return;
		getGuildAudioPlayer(e.getTextChannel()).updateMusicMode(MusicMode.BassBoosted);
	}
	
	@DiscordCommand(Name = "setdj",
			Summary = "Sets the DJ role!",
			Syntax = "setdj {role name}",
			SpecialPerms = true)
	public void areYOUaDJ(MessageReceivedEvent e, ArrayList<String> args) {
		//if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),false)) return;
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
			g.setDJRoleId(rolesWithName.get(0).getId());
			database.setGuildList(database.getGuildList().addGuild(g));
			CommandHandler.sendMessage(e, String.format("%s is now DJ!", rolesWithName.get(0).getAsMention()),false);
		}
	}
	
	@DiscordCommand(Name = "clear",
			Summary = "Erases the queue and stops the current song.")
	public void deleteALLtheSONGS(MessageReceivedEvent e, ArrayList<String> args) {
		if (!allowedToUsePlayerCommands(e.getTextChannel(),e.getAuthor(),false)) return;
		getGuildAudioPlayer(e.getTextChannel()).scheduler.clearQueue();
	}
	
	@DiscordCommand(Name = "queue",
			Summary = "Displays the current queue.")
	public void hereAreMySongs(MessageReceivedEvent e, ArrayList<String> args) {
		if (botVoiceChannel(e.getTextChannel()) == null) {
			CommandHandler.sendMessage(e, String.format("%s, I'm not in a voice channel!", e.getAuthor().getAsMention()),false);
			return;
		}
		CommandHandler.sendMessage(e,getGuildAudioPlayer(e.getTextChannel()).scheduler.listOfTracks(),false);
	}

	@Override
	public String getName() {
		return MODULE_NAME;
	}

	@Override
	public List<Class<? extends Module>> getDependencies() {
		return DEPENDENCIES;
	}
	
	@Override
	public void initialize() throws Exception {
		try {
			database = DiscordBot.readDatabase(MusicCommandsDatabase.class);
		} catch (SAXException e1) {
			e1.printStackTrace();
		}
		if (database == null)
			database = DiscordBot.writeDatabase(new MusicCommandsDatabase());
		
		musicManagers = new HashMap<>();
	    playerManager = new DefaultAudioPlayerManager();
	    AudioSourceManagers.registerRemoteSources(playerManager);
	    AudioSourceManagers.registerLocalSource(playerManager);
	}

	@Override
	public void shutdown() {
		for (GuildMusicManager gmm : musicManagers.values())
			if (gmm != null)
				destroyPlayerManager(gmm.textChannel.getGuild().getIdLong());
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
