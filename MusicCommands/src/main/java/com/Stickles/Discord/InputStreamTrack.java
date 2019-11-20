package com.Stickles.Discord;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sound.sampled.AudioInputStream;

import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameProvider;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;

public class InputStreamTrack implements AudioTrack, AudioFrameProvider {

	private AudioInputStream is;
	Object userData = null;
	
	InputStreamTrack(AudioInputStream is) {
		this.is = is;
	}
	
	
	@Override
	public AudioFrame provide() {
		byte[] buffer = new byte[is.getFormat().getFrameSize()];
		int result = -1;
		
		try {
			result = is.read(buffer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (result == -1)
			return null;
		return new InputStreamFrame(
				buffer,
				getPosition(),
				100,
				is.getFormat().getFrameSize(),
				new InputStreamDataFormat(
						is.getFormat().getChannels(),
						(int)is.getFormat().getSampleRate(),
						is.getFormat().getSampleSizeInBits()
				)
		);
	}

	@Override
	public AudioFrame provide(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
		return provide();
	}

	@Override
	public boolean provide(MutableAudioFrame targetFrame) {
		targetFrame = (MutableAudioFrame) provide();
		return true;
	}

	@Override
	public boolean provide(MutableAudioFrame targetFrame, long timeout, TimeUnit unit)
			throws TimeoutException, InterruptedException {
		targetFrame = (MutableAudioFrame) provide();
		return true;
	}
	
	@Override
	public AudioTrackInfo getInfo() {
		String title = "Unknown";
		String author = "Unknown";
		long length = (long) (1000 * is.getFrameLength() / is.getFormat().getFrameRate());
		String identifier = is.toString();
		boolean isStream = true;
		String uri = "Unknown";
		return new AudioTrackInfo(title, author, length, identifier, isStream, uri);
	}

	@Override
	public String getIdentifier() {
		return is.toString();
	}

	@Override
	public AudioTrackState getState() {
		return AudioTrackState.PLAYING;
	}

	@Override
	public void stop() {
		return;
	}

	@Override
	public boolean isSeekable() {
		return true;
	}

	@Override
	public long getPosition() {
		long remainingFrames = 0;
		try {
			remainingFrames = (is.available()/is.getFormat().getFrameSize());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return (long) (1000 * remainingFrames / is.getFormat().getFrameRate());
	}

	@Override
	public void setPosition(long position) {	
		try {
			is.reset();	//goes back to start
			is.skip((long) (
					(position / 1000) *	 //position in seconds
					(is.getFormat().getFrameRate()*	//bytes
					is.getFormat().getFrameSize())	//per second
					));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setMarker(TrackMarker marker) {
		return;
	}

	@Override
	public long getDuration() {
		return (long) (1000 * is.getFrameLength() / is.getFormat().getFrameRate());
	}

	@Override
	public AudioTrack makeClone() {
		return new InputStreamTrack(new AudioInputStream(is,is.getFormat(),is.getFrameLength()));
	}

	@Override
	public AudioSourceManager getSourceManager() {
		return null;
	}

	@Override
	public void setUserData(Object userData) {
		this.userData = userData;
	}

	@Override
	public Object getUserData() {
		return userData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getUserData(Class<T> klass) {
		if (userData.getClass().equals(klass))
			return (T)userData;
		return null;
	}
}
