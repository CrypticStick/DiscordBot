package com.Stickles.Discord;

import java.awt.Color;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.Stickles.DiscordBot;
import com.github.natanbc.lavadsp.distortion.DistortionPcmAudioFilter;
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.github.natanbc.lavadsp.vibrato.VibratoPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Holder for both the player and a track scheduler for one guild.
 */
public class GuildMusicManager extends ListenerAdapter {
  /**
   * Audio player for the guild.
   */
  public final AudioPlayer player;
  /**
   * Track scheduler for the player.
   */
  public final TrackScheduler scheduler;
  /**
   * Channel for music commands.
   */
  public final MusicPlayerMessage playerMessage;
  /**
   * Music player frontend
   */
  
  
  public TextChannel textChannel;
  
  enum MusicMode {
	  Normal,
	  Nightcore,
	  BassBoosted;
  }
  
  public class UserData {
	  final private User user;
	  private long lastTrackPosition = 0;
	  private long lastOffsetPosition = 0;
	  private double speed = 1;
	  
	  UserData(User user) {
		  this.user = user;
	  }
	  
	  User getUser() {
		  return user;
	  }
	  
	  long setSpeed(long currentPosition, double speed) {
		  getPosition(currentPosition);
		  this.speed = speed;
		  return lastOffsetPosition;
	  }
	  
	  long getPosition(long currentPosition) {
		  lastOffsetPosition += (currentPosition - lastTrackPosition) * speed;
		  lastTrackPosition = currentPosition;
		  return lastOffsetPosition;
	  }
	  
	  void resetTimeData() {
		  lastTrackPosition = 0;
		  lastOffsetPosition = 0;
		  speed = 1;
	  }
  }
  
  private void normalMode() {
	  	float[] EQ_Band_Multi = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
	    for (int i = 0; i < EQ_Band_Multi.length; i++) 
	        equalizer.setGain(i, EQ_Band_Multi[i]); // -0.25 <= x <= 1;

		timescale.setPitch(1);
		timescale.setRate(1);
		timescale.setSpeed(1);

		if (player.getPlayingTrack() != null) {
			UserData ud = player.getPlayingTrack().getUserData(UserData.class);
			if (ud != null)
				ud.setSpeed(player.getPlayingTrack().getPosition(), 1);
		}
  }
  
  private void nightcoreMode() {
	  	float[] EQ_Band_Multi = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
	    for (int i = 0; i < EQ_Band_Multi.length; i++) 
	        equalizer.setGain(i, EQ_Band_Multi[i]);
	    
		timescale.setPitch(1.25);
		timescale.setRate(1);
		timescale.setSpeed(1.5);
		
		if (player.getPlayingTrack() != null) {
			UserData ud = player.getPlayingTrack().getUserData(UserData.class);
			if (ud != null)
				ud.setSpeed(player.getPlayingTrack().getPosition(), 1.5);
		}
  }
  
  private void bassboostedMode() {
	  	float[] EQ_Band_Multi = { 1f, 1f, 0.7f, 0.5f, 0.0f, -0.05f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f };
	    for (int i = 0; i < EQ_Band_Multi.length; i++) 
	        equalizer.setGain(i, EQ_Band_Multi[i]);
	    
		timescale.setPitch(1);
		timescale.setRate(1);
		timescale.setSpeed(1);
		
		if (player.getPlayingTrack() != null) {
			UserData ud = player.getPlayingTrack().getUserData(UserData.class);
			if (ud != null)
				ud.setSpeed(player.getPlayingTrack().getPosition(), 1);
		}
  }
  
  public void updateMusicMode() {
  	if (equalizer == null) return;
	if (timescale == null) return;

	  if (player.getPlayingTrack() == null)
		  return;
	  if(!player.getPlayingTrack().getState().equals(AudioTrackState.PLAYING))
		  return;
	  if (playerMessage.getMusicMode().equals(MusicMode.Normal)) {
		  normalMode();
	  } else if (playerMessage.getMusicMode().equals(MusicMode.Nightcore)) {
		  nightcoreMode();
	  } else if (playerMessage.getMusicMode().equals(MusicMode.BassBoosted)) {
		  bassboostedMode();
	  }
  }
  
  private Equalizer equalizer;
  private TimescalePcmAudioFilter timescale;
  /**
   * Creates a player and a track scheduler.
   * @param manager Audio player manager to use for creating the player.
   */
  public GuildMusicManager(AudioPlayerManager manager, TextChannel tc) {
	  manager.getConfiguration().setFilterHotSwapEnabled(true);
    player = manager.createPlayer();
    player.setFilterFactory((track, format, output)->{
        timescale = new TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate);
        equalizer = new Equalizer(format.channelCount, timescale);
        return Arrays.asList(equalizer, timescale);
    });
    player.setFrameBufferDuration(500);
    scheduler = new TrackScheduler(player, this);
    textChannel = tc;
    player.addListener(scheduler);
    playerM
    DiscordBot.jda.addEventListener(this);
  }
  
  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
	  if (event.getTextChannel() == null) return;
	  if (!event.getTextChannel().getId().equals(textChannel.getId()) || playerMessage == null)
		  return;
	  if (event.getMessageIdLong() == lastpmid)
		  return;
//	  GuildVoiceState gvs = textChannel.getGuild().getSelfMember().getVoiceState();
//	  if (!gvs.inVoiceChannel() && !textChannel.getGuild().getAudioManager().isAttemptingToConnect()) 
//		  return;
	  
	  if (event.getAuthor().getId().equals(DiscordBot.jda.getSelfUser().getId())) {
		  CommandHandler.volatileMessage(event.getMessage(), 5);
	  } else {
		  event.getMessage().delete().queue();
	  }
  }
  
  @Override
  public void onMessageDelete(MessageDeleteEvent event) {
	  if (event.getMessageIdLong() == lastpmid && !destroyed) {
		  resetPlayerMessage(true);
	  } 
  }
  
  @Override
  public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
	  if (event.getUser().getId().equals(DiscordBot.jda.getSelfUser().getId()) || playerMessage == null) 
		  return;
	  if (!event.getMessageId().equals(playerMessage.getId()))
		  return;
	  if (!MusicCommands.memberInVoiceChannel(event.getChannel(), event.getUser(), true)) {
		  event.getReaction().removeReaction(event.getUser()).queue();
		  return;
	  }	  
	  
	  MessageReaction reaction = event.getReaction();
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

	  boolean newConnection = false;
	  boolean trackData = false;
	  if (required.contains(reaction.getReactionEmote().getName())) {
		  if (!event.getChannel().getGuild().getSelfMember().getVoiceState().inVoiceChannel())
			  newConnection = MusicCommands.connectToMemberVoiceChannel(event.getChannel(),event.getUser());
		  boolean canRunCommand = false;
		  if (player.getPlayingTrack() != null)
			  if (player.getPlayingTrack().getUserData(UserData.class) != null) {
				  trackData = true;
				  if (event.getUser().getId().equals(player.getPlayingTrack().getUserData(UserData.class).getUser().getId()))
					  canRunCommand = true;
			  }
		  String djRole = MusicCommands.database.getGuildList().getGuild(event.getGuild().getId()).getDJRoleId();
		  if (djRole == null)
			  CommandHandler.sendMessage(event.getUser(), textChannel,"Warning: Please set the DJ role with `.setDJ`!", false);
		  else
			  for (Role role : event.getMember().getRoles()) {
				  if (role.getId().equals(djRole))
					  canRunCommand = true;
				  break;
			  }
		  
		  if (canRunCommand)
			  switch (reaction.getReactionEmote().getName()) {
		  		case DiscordEmojis.lastTrack:
		  			lastTrack(event.getUser());
		  			break;
		  		case DiscordEmojis.rewind:
		  			if (trackData)
		  				player.getPlayingTrack().getUserData(UserData.class).updatePositionOffset(
		  						player.getPlayingTrack().getPosition(), speedChange
		  						);
		  			scheduler.moveTrackPosition(-10);
		  			if (trackData)
		  				player.getPlayingTrack().getUserData(UserData.class).updatePositionOffset(
		  						player.getPlayingTrack().getPosition(), 1
		  						);
		  			break;
		  		case DiscordEmojis.playPause:
		  			player.setPaused(!player.isPaused());
		  			break;
		  		case DiscordEmojis.fastForward:
		  			if (trackData)
		  				player.getPlayingTrack().getUserData(UserData.class).updatePositionOffset(
		  						player.getPlayingTrack().getPosition(), speedChange
		  						);
		  			scheduler.moveTrackPosition(10);
		  			if (trackData)
		  				player.getPlayingTrack().getUserData(UserData.class).updatePositionOffset(
		  						player.getPlayingTrack().getPosition(), 1
		  						);
		  			break;
		  		case DiscordEmojis.nextTrack:
		  			scheduler.nextTrack();
		  			break;
		  		case DiscordEmojis.shuffle:
		  			scheduler.toggleShuffleMode();
		  			break;
		  		case DiscordEmojis.loop:
		  			scheduler.toggleLoopMode();
		  			break;
		  		case DiscordEmojis.loopOne:
		  			scheduler.toggleLoopOneMode();
		  			break;
			  }
		  else
			  if (reaction.getReactionEmote().getName().equals(DiscordEmojis.nextTrack))
				  addVoteSkip(event.getUser());
	  }
	  if (!newConnection)
		  reaction.removeReaction(event.getUser()).queue();
  }
  
  @Override
  public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
	  VoiceChannel vc = textChannel.getGuild().getMember(DiscordBot.jda.getSelfUser()).getVoiceState().getChannel();
	  if (vc == null) return;
	  if (vc.getIdLong() != event.getChannelLeft().getIdLong()) return;
	  removeVoteSkipAndCheck(event.getMember().getUser());
  }
  
  List<String> voteSkip = new ArrayList<String>();
  String lastVoted = "";
  public void addVoteSkip(User u) {
	  if (player.getPlayingTrack() == null) return;
	  if (player.getPlayingTrack().getIdentifier() != lastVoted) {
		  voteSkip.clear();
		  lastVoted = player.getPlayingTrack().getIdentifier();
	  }
	  VoiceChannel vc = textChannel.getGuild().getMember(DiscordBot.jda.getSelfUser()).getVoiceState().getChannel();
	  if (vc == null) return;
	  if (voteSkip.contains(u.getId())) return;
	  voteSkip.add(u.getId());
	  int votesNeeded = (int) Math.ceil((vc.getMembers().size()-1)/2.0);
	  if (votesNeeded <= voteSkip.size()) {
		  voteSkip.clear();
		  scheduler.nextTrack();
	  } else {
		  int votesRemaining = votesNeeded - voteSkip.size();
		  CommandHandler.sendMessage(u, textChannel, String.format("Vote skip added: %s more needed.", votesRemaining), false);
	  }
  }
  
  public void removeVoteSkipAndCheck(User u) {
	  if (player.getPlayingTrack() == null) return;
	  if (player.getPlayingTrack().getIdentifier() != lastVoted) {
		  voteSkip.clear();
		  lastVoted = player.getPlayingTrack().getIdentifier();
	  }
	  VoiceChannel vc = textChannel.getGuild().getMember(DiscordBot.jda.getSelfUser()).getVoiceState().getChannel();
	  if (vc == null) return;
	  if (vc.getMembers().size() <= 1) return;
	  voteSkip.remove(u.getId());
	  int votesNeeded = (int) Math.ceil((vc.getMembers().size()-1)/2.0);
	  if (votesNeeded <= voteSkip.size()) {
		  voteSkip.clear();
		  scheduler.nextTrack();
	  }
  }
  
  public void lastTrack(User u) {
		if (scheduler.getLoopMode() || scheduler.getLoopOneMode())
			scheduler.playLastTrack();
		else
			CommandHandler.sendMessage(u, textChannel, String.format("%s, rewind only works in loop mode!",u.getAsMention()), false);
  }
    
  /**
   * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
   */
  public AudioPlayerSendHandler getSendHandler() {
    return new AudioPlayerSendHandler(player);
  }
}
