package com.Stickles.Discord;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkDecoder;
import com.sedmelluq.discord.lavaplayer.format.transcoder.AudioChunkEncoder;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;

public class InputStreamDataFormat extends AudioDataFormat {

	public InputStreamDataFormat(int channelCount, int sampleRate, int chunkSampleCount) {
		super(channelCount, sampleRate, chunkSampleCount);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String codecName() {
		return null;
	}

	@Override
	public byte[] silenceBytes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int expectedChunkSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int maximumChunkSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public AudioChunkDecoder createDecoder() {
		return null;
	}

	@Override
	public AudioChunkEncoder createEncoder(AudioConfiguration configuration) {
		return null;
	}

}
