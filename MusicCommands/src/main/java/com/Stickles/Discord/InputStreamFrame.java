package com.Stickles.Discord;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

public class InputStreamFrame implements AudioFrame {

	byte[] frame;
	long timecode;
	int volume;
	int length;
	AudioDataFormat format;
	
	InputStreamFrame(byte[] frame, long timecode, int volume, int length, AudioDataFormat format) {
		this.frame = frame;
		this.timecode = timecode;
		this.volume = volume;
		this.length = length;
		this.format = format;
	}
	
	@Override
	public long getTimecode() {
		return timecode;
	}

	@Override
	public int getVolume() {
		return volume;
	}

	@Override
	public int getDataLength() {
		return length;
	}

	@Override
	public byte[] getData() {
		return frame;
	}

	@Override
	public void getData(byte[] buffer, int offset) {
		
		if (offset != 0) {
			byte[] toReturn = new byte[frame.length-offset];
			for (int i = 0; i < frame.length; i++) {
				if (i < offset) continue;
				toReturn[i-offset] = frame[i];
			}
			buffer = toReturn;
		} else {
			buffer = frame;
		}
	}

	@Override
	public AudioDataFormat getFormat() {
		return format;
	}

	@Override
	public boolean isTerminator() {
		return false;
	}

}
