package com.Stickles.Discord.MusicCommandsDatabase;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;

public class MusicModeList {

    private ArrayList<CustomMusicMode> musicModeList;

    public MusicModeList() {
    	musicModeList = new ArrayList<CustomMusicMode>();
    }

    public MusicModeList(ArrayList<CustomMusicMode> musicModeList) {
        this.musicModeList = musicModeList;
    }

    @XmlElement(name = "musicMode")
    public ArrayList<CustomMusicMode> getMusicModeList() {
    	if (musicModeList == null)
    		musicModeList = new ArrayList<CustomMusicMode>();
        return musicModeList;
    }

    public MusicModeList setMusicModeList(ArrayList<CustomMusicMode> musicModeList) {
        this.musicModeList = musicModeList;
        return this;
    }
    
    private CustomMusicMode getMusicMode(String name) {
    	if (musicModeList == null)
    		musicModeList = new ArrayList<CustomMusicMode>();
    	
    	for(int i = 0; this.musicModeList.size() > i ; i++) 
    		if (musicModeList.get(i).getName().equals(name)) 
    			return musicModeList.get(i);
    	
        return null;
    }
    
    public MusicModeList addMusicMode(CustomMusicMode musicMode) {
    	if (getMusicMode(musicMode.getName()) != null)
    		getMusicModeList().remove(getMusicMode(musicMode.getName()));
    	getMusicModeList().add(musicMode);
    		
    	return this;
    }

}