package com.Stickles.Discord.EssentialCommandsDatabase;

import javax.xml.bind.annotation.XmlAttribute;

public class DiscordGuild {

    private String id;
    private String name;
    
    public DiscordGuild() {
    }

    public DiscordGuild(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @XmlAttribute
    public String getId() {
        return id;
    }

    public DiscordGuild setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public DiscordGuild setName(String name) {
        this.name = name;
        return this;
    }
}