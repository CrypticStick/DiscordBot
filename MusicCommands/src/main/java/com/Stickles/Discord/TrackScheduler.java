package com.Stickles.Discord;

import com.Stickles.DiscordBot;
import com.Stickles.Discord.GuildMusicManager.UserData;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import net.dv8tion.jda.core.entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
  private final AudioPlayer player;
  private final BlockingQueue<AudioTrack> queue;
  private List<AudioTrack> queueCopy;
  private GuildMusicManager gmm;
  private boolean shuffleMode = false;
  private boolean loopMode = false;
  private boolean loopOneMode = false;
  
  /**
   * @param player The audio player this scheduler uses
   */
  public TrackScheduler(AudioPlayer player, GuildMusicManager gmm) {
	  this.player = player;
	  this.queue = new LinkedBlockingQueue<>();
	  this.gmm = gmm;
  }

  /**
   * Add the next track to queue or play right away if nothing is in the queue.
   *
   * @param track The track to play or add to queue.
   */
  public void queue(AudioTrack track, User author) {
    // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
    // something is playing, it returns false and does nothing. In that case the player was already playing so this
    // track goes to the queue instead.
	track.setUserData(gmm.new UserData(author));
    if (!player.startTrack(track, true)) {
      if (queue.offer(track) && shuffleMode)
    	  queueCopy.add(track);
    }
  }
  
  public boolean getShuffleMode() {
	  return shuffleMode;
  }
  
  public void toggleShuffleMode() {
	  shuffleMode = !shuffleMode;
	  if (shuffleMode) {
			queueCopy = new ArrayList<AudioTrack>(Arrays.asList(queue.toArray(new AudioTrack[0])));
					  
			queue.removeAll(queueCopy);
			List<AudioTrack> queueCopyCopy = new ArrayList<AudioTrack>(queueCopy);
			Collections.sort(queueCopyCopy,new Comparator<AudioTrack>()	//Sorting the modules so that this one is always first
					  {
					     public int compare(AudioTrack o1, AudioTrack o2)
					     {
					    	 Random rand = new Random(); 
					    	 return rand.nextInt(2)-1; 
					     }
					});	
			queue.addAll(queueCopyCopy);
	  } else {
		  queue.removeAll(queueCopy);
		  queue.addAll(queueCopy);
	  }
  }
  
  public boolean getLoopMode() {
	  return loopMode;
  }
  
  public void toggleLoopMode() {
	  loopMode = !loopMode;
	  if (loopMode)
		  loopOneMode = false;
  }
  
  public boolean getLoopOneMode() {
	  return loopOneMode;
  }
  
  public void toggleLoopOneMode() {
	  loopOneMode = !loopOneMode;
	  if (loopOneMode) {
		  loopMode = false;
	  }
  }
  
  public void playLastTrack() {
	  if (loopOneMode && player.getPlayingTrack() != null) {
		  player.getPlayingTrack().setPosition(0);
		  return;
	  }
	  AudioTrack[] q = queue.toArray(new AudioTrack[0]);
	  AudioTrack lastTrack = q[q.length-1];
	  
	  if (loopMode)
		  if (player.getPlayingTrack() != null) {
			  AudioTrack currentTrack = player.getPlayingTrack().makeClone();
			  currentTrack.setUserData(player.getPlayingTrack().getUserData());
			  List<AudioTrack> tracks = new ArrayList<AudioTrack>();
			  queue.drainTo(tracks);
			  tracks.add(0, currentTrack);
			  queue.addAll(tracks);
			  if (shuffleMode)
				  queueCopy.add(0, currentTrack);
		  }
	  
	  queue.remove(lastTrack);
	  if (shuffleMode)
		  queueCopy.remove(lastTrack);
	  player.startTrack(lastTrack, false);
  }
  
  void moveTrackPosition (int seconds) {
	  if (player.getPlayingTrack() != null) {
		  player.getPlayingTrack().setPosition(player.getPlayingTrack().getPosition() + seconds*1000);
	  }
  }
  
  public boolean isQueueEmpty() {
	  return queue.isEmpty();
  }
  
  public void clearQueue() {
	  queue.clear();
	  player.playTrack(null);
	  CommandHandler.sendMessage(DiscordBot.jda.getSelfUser(), gmm.textChannel, "Queue has been cleared!", false);
  }
  
  public String listOfTracks() {
	  StringBuilder sb = new StringBuilder();
	  int i = 1;
	  if (player.getPlayingTrack() != null)
		  sb.append(String.format("%s) **(Current track)** `%s` (%s) Requested By `%s`", 
				  i++, 
				  player.getPlayingTrack().getInfo().title, 
				  (player.getPlayingTrack().getInfo().isStream) ? "Stream" : MusicCommands.millisToTime(player.getPlayingTrack().getInfo().length),
				  (player.getPlayingTrack().getUserData(User.class) != null) ? 
						  gmm.textChannel.getGuild().getMember(player.getPlayingTrack().getUserData(User.class)).getNickname() : "Unknown"
				  ) + System.lineSeparator());

	  for (AudioTrack at : queue) {
		  sb.append(String.format("%s) `%s` (%s)",
				  i++, 
				  at.getInfo().title, 
				  (player.getPlayingTrack().getInfo().isStream) ? "Stream" : MusicCommands.millisToTime(at.getInfo().length),
				  (at.getUserData(User.class) != null) ? at.getUserData(User.class).getName() : "Unknown"
				  ) + System.lineSeparator());
	  }
	  return (sb.length() == 0) ? "The queue is currently empty." : "Songs in queue:" + System.lineSeparator() + sb.toString();
  }
  
  /**
   * Start the next track, stopping the current one if it is playing.
   */
  public void nextTrack() {
    // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
    // giving null to startTrack, which is a valid argument and will simply stop the player.
	  if (loopOneMode) {
		  if (player.getPlayingTrack() != null)
			  player.getPlayingTrack().setPosition(0);
	  } else if (loopMode) {
		  if (player.getPlayingTrack() != null) {
			  AudioTrack current = player.getPlayingTrack().makeClone();
			  current.setUserData(player.getPlayingTrack().getUserData());
			  current.getUserData(UserData.class).resetTimeData();
			  if (queue.offer(current) && shuffleMode)
				  queueCopy.add(current);
		  }
		  AudioTrack at = queue.poll();
		  if (shuffleMode)
			  queueCopy.remove(at);
		  player.startTrack(at, false);
		  
	  } else {
		  AudioTrack at = queue.poll();
		  player.startTrack(at, false);
		  if (shuffleMode)
			  queueCopy.remove(at);
	  }
  }
	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		// Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
		if (loopOneMode) {
			AudioTrack at = track.makeClone();
			at.setUserData(track.getUserData());
			at.getUserData(UserData.class).resetTimeData();
			player.startTrack(at, false);
			return;
		}
		if (endReason.mayStartNext) 
			nextTrack();
	}
  
  @Override
  public void onPlayerPause(AudioPlayer player) {
	  CommandHandler.sendMessage(DiscordBot.jda.getSelfUser(), gmm.textChannel, "Pausing player.", false);
  }

  @Override
  public void onPlayerResume(AudioPlayer player) {
	  AudioTrack track = player.getPlayingTrack();
	  if (track != null) {	  
		  CommandHandler.sendMessage(DiscordBot.jda.getSelfUser(), gmm.textChannel, String.format("Resuming `%s` (%s).", track.getInfo().title, (track.getInfo().isStream) ? "Stream" : MusicCommands.millisToTime(track.getInfo().length)), false);
	  }
  }

  @Override
  public void onTrackStart(AudioPlayer player, AudioTrack track) {
	  	if (!loopOneMode) 
	  		CommandHandler.sendMessage(DiscordBot.jda.getSelfUser(), gmm.textChannel, String.format("Playing `%s` (%s).", track.getInfo().title, (track.getInfo().isStream) ? "Stream" : MusicCommands.millisToTime(track.getInfo().length)), false);
  }

  @Override
  public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
    // An already playing track threw an exception (track end event will still be received separately)
  }

  @Override
  public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
    // Audio track has been unable to provide us any audio, might want to just start a new track
	  nextTrack();
  }
}
