package com.Stickles.Discord.MusicCommandsDatabase;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class DiscordGuild {

    private String id;
    private String djRoleId;
   private MusicModeList musicModeList;
    
    public DiscordGuild() {
    }

    public DiscordGuild(String id, String djRoleId, MusicModeList musicModeList) {
        this.id = id;
        this.djRoleId = djRoleId;
        this.musicModeList = musicModeList;
    }

    @XmlAttribute
    public String getId() {
        return id;
    }

    public DiscordGuild setId(String id) {
        this.id = id;
        return this;
    }

    public String getDJRoleId() {
        return djRoleId;
    }

    public DiscordGuild setDJRoleId(String id) {
        this.djRoleId = id;
        return this;
    }

    @XmlElement
    public MusicModeList getMusicModeList() {
    	if (musicModeList == null)
    		musicModeList = new MusicModeList();
        return musicModeList;
    }

    public DiscordGuild setMusicModeList(MusicModeList musicModeList) {
        this.musicModeList = musicModeList;
        return this;
    }
    
}