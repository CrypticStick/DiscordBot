package com.Stickles.Discord.CommandHandlerDatabase;

import javax.xml.bind.annotation.XmlAttribute;

public class DiscordGuild {

    private String id;
    private String modRoleId;
    
    public DiscordGuild() {
    }

    public DiscordGuild(String id, String modRoleId) {
        this.id = id;
        this.modRoleId = modRoleId;
    }

    @XmlAttribute
    public String getId() {
        return id;
    }

    public DiscordGuild setId(String id) {
        this.id = id;
        return this;
    }

    public String getModRoleId() {
        return modRoleId;
    }

    public DiscordGuild setModRoleId(String id) {
        this.modRoleId = id;
        return this;
    }
}