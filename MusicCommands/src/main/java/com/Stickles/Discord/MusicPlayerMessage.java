package com.Stickles.Discord;

import java.awt.Color;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.Stickles.Discord.GuildMusicManager.MusicMode;
import com.Stickles.Discord.GuildMusicManager.UserData;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class MusicPlayerMessage {
	  private Message playerMessage = null;
	  private final Object pmLock = new Object();
	  private final AudioPlayer player;
	  private final TrackScheduler scheduler;
	  private TextChannel channel;
	  private boolean destroyed = false;
	  private long lastpmid = 0;
	  private int idleMinutes = 5;	//max time in minutes allowed to stay silently connected
	  private int updateFrequency = 3; //interval in seconds between player message updates
	  private MusicMode currentMode = MusicMode.Normal;
	  
	  MusicPlayerMessage(GuildMusicManager gmm) {
		  this.player = gmm.player;
		  this.scheduler = gmm.scheduler;
		  this.channel = gmm.textChannel;
		  idleMinutes = 5;
		  updateFrequency = 3;
	  }
	  
	  void destroy() {
		  destroyed = true;
	  }
	  
	  void setIdleMinutes(int min) {
		  idleMinutes = min;
	  }
	  
	  void setUpdateFrequency(int sec) {
		  updateFrequency = sec;
	  }
	  
	  void setMusicMode(MusicMode mode) {
		  currentMode = mode;
	  }
	  
	  long getLastId() {
		  return lastpmid;
	  }
	  
	  String getId() {
		  return playerMessage.getId();
	  }
	  
	  boolean isDestroyed() {
		  return destroyed;
	  }
	  
	  MusicMode getMusicMode() {
		  return currentMode;
	  }
	  
	  void setChannel(TextChannel channel) {
		  if (this.channel.getId().equals(channel.getId())) return;
		  this.channel = channel;
		  resetPlayerMessage(true);
	  }
	  
	  void addPlayerEmotes() {
		  List<String> required = new ArrayList<String>(Arrays.asList(
				  DiscordEmojis.lastTrack,
				  DiscordEmojis.rewind,
				  DiscordEmojis.playPause,
				  DiscordEmojis.fastForward,
				  DiscordEmojis.nextTrack,
				  DiscordEmojis.shuffle,
				  DiscordEmojis.loop,
				  DiscordEmojis.loopOne
				  ));
		  
		  synchronized (pmLock) {
			  for (String missing : required) {
				  if (playerMessage == null || destroyed) return;
				  playerMessage.addReaction(missing).complete();
			  }
		  }
	  }
	  

	  public void resetPlayerMessage(boolean attemptDelete) {
			if (playerMessage != null && attemptDelete) {
				deletePlayerMessage();
				return;
			}
		synchronized (pmLock) {
			playerMessage = CommandHandler.sendMessage(channel.getGuild().getSelfMember().getUser(), channel, getPlayerEmbed().build(),false);
			lastpmid = playerMessage.getIdLong();
			new Thread(() -> {addPlayerEmotes();}).start();
		}
	  }
	  
	  public void deletePlayerMessage() {
		  synchronized (pmLock) {
			  if (playerMessage != null) {
			  	lastpmid = playerMessage.getIdLong();
				playerMessage.delete().queue();
				playerMessage = null;
			  }
		  }
	  }
	  
	  public void refreshPlayerMessage() {
		  synchronized (pmLock) {
			  if (playerMessage != null) {
				  lastpmid = playerMessage.getIdLong();
				  playerMessage.editMessage(getPlayerEmbed().build()).queue();
			  }
		  }
	  }
	  
	  public void backgroundThread() {
			new Thread(() ->  {
				Long reloadEmbedTime = updateFrequency * 1000 + System.currentTimeMillis();
				Long waitTime = (long) (1000 * 60 * idleMinutes) + System.currentTimeMillis();
				boolean waitTimeReset = true;
				while (!destroyed) {
					try {
						
						if (playerMessage != null) {
							if (reloadEmbedTime < System.currentTimeMillis()) {
								refreshPlayerMessage();
								reloadEmbedTime = updateFrequency * 1000 + System.currentTimeMillis();
							}
							waitTimeReset = true;
						} else if (!Thread.holdsLock(pmLock)) {
							resetPlayerMessage(false);
						}
						
						try {
							Thread.sleep(750);	//just to avoid excessive CPU usage
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						GuildVoiceState gvs = channel.getGuild().getSelfMember().getVoiceState();
						if (!gvs.inVoiceChannel()) {
							waitTimeReset = true;
							continue;
						}
						
						if (waitTimeReset) {	//reset wait time if (just reconnected to voice chat || Queue not empty)
							waitTime = (long) (1000 * 60 * idleMinutes) + System.currentTimeMillis();
							waitTimeReset = false;
						}
						
						if (gvs.getChannel().getMembers().size() <= 1 && channel.getGuild().getAudioManager().isConnected()) {
							MusicCommands.leaveVoiceChannel(channel.getGuild().getIdLong(),false,"All users have left the voice channel. Queue has paused.");
						}
						
						if ((scheduler.isQueueEmpty() && player.getPlayingTrack() == null) || player.isPaused()) {
							if (System.currentTimeMillis() > waitTime) {
								MusicCommands.leaveVoiceChannel(channel.getGuild().getIdLong(),false,"It's been pretty quiet for a while. Leaving for now.");
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
		}).start();
	}
	  
	  private String progressBar(AudioTrack at) {
		  int scaledTotal = 30;
		  long total = 0;
		  long current = 0;
		  StringBuilder sb = new StringBuilder();
		  if (at != null) {
			  total = at.getDuration();
			  current = at.getUserData(UserData.class).getPosition(at.getPosition());
			  if (at.getInfo().isStream) 
				  return "Stream";
		  }
		  int position = (int)((1 - (double)(total - current) / total) * scaledTotal);
		  position = (position < 0) ? 0 : (position > scaledTotal) ? scaledTotal : position;
		  sb.append(MusicCommands.millisToTime(current) + " `");
		  sb.append(String.join("", Collections.nCopies(position, "\u2500")));
		  sb.append(DiscordEmojis.circle);
		  sb.append(String.join("", Collections.nCopies(scaledTotal-position, "\u2500")));
		  sb.append("` " + MusicCommands.millisToTime(total));
		  return sb.toString();
	  }
	  
	public EmbedBuilder getPlayerEmbed() {
		  EmbedBuilder eb = new EmbedBuilder();
		  eb.setColor(Color.MAGENTA);
		  AudioTrack at = player.getPlayingTrack();
		  
		  eb.setAuthor(String.format("Now %s \uD834\uDD60", (player.isPaused()) ? "paused" : "playing"));
		  
		  String currentlyPlaying = (at == null) ? "Nothing" : at.getInfo().title;
		  eb.setTitle(currentlyPlaying, (at == null) ? "" : at.getInfo().uri);
		  
		  String progressBar = (at == null) ? progressBar(at) : (at.getInfo().isStream) ? "Streaming " + DiscordEmojis.redCircle : progressBar(at);
		  eb.addField("",progressBar, false);
		  
		  eb.addField("", 
				  String.format("Volume: %s%% | ", player.getVolume()) +
				  ((scheduler.getShuffleMode()) ? DiscordEmojis.shuffle + " " : "") +
				  ((scheduler.getLoopMode()) ? DiscordEmojis.loop  + " " : "") +
				  ((scheduler.getLoopOneMode()) ? DiscordEmojis.loopOne  + " " : "") + 
				  ((currentMode == MusicMode.Normal) ? "" : String.format("`%s` mode is active!", currentMode.toString()))
				  , false);
		  if (at != null) {
			  eb.addField("`Requested by:` ", (at.getUserData(UserData.class) != null) ? at.getUserData(UserData.class).getUser().getAsMention() : "Unknown", false);
			  eb.setThumbnail(getThumbnail(at.getInfo()));
		  } else {
			  eb.addBlankField(false);
		  }

		  return eb;
	  }
	  
	  private String getThumbnail(AudioTrackInfo info) {
		    try {
		        URI uri = new URI(info.uri);
		        if (uri.getHost().contains("youtube.com") || uri.getHost().contains("youtu.be")) {
		            return String.format("https://img.youtube.com/vi/%s/0.jpg", info.identifier);
		        }
		    } catch (URISyntaxException e) {
		        e.printStackTrace();
		    }
		    return null;
		}

}
