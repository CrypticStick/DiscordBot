package com.Stickles.Discord.MusicCommandsDatabase;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class CustomMusicMode {

    private String name;
    private int speed;
    private int pitch;
    private int vibratoDepth;
    
    
    public CustomMusicMode() {
    }

    public CustomMusicMode(String name) {
        this.name = name;
    }

    @XmlAttribute
    public String getName() {
        return name;
    }

    public CustomMusicMode setName(String name) {
        this.name = name;
        return this;
    }
}