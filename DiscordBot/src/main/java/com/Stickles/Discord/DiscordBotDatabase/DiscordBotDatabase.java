package com.Stickles.Discord.DiscordBotDatabase;

import javax.xml.bind.annotation.*;
import com.Stickles.DiscordBot;
import com.Stickles.Discord.Database;

@XmlRootElement
public class DiscordBotDatabase implements Database {

	private String token;
	private String prefix;
    private String game;

    public DiscordBotDatabase(){
    	this.token = getToken();
        this.prefix = getPrefix();
        this.game = getGame();
    }

    public DiscordBotDatabase(String token, String prefix, String game) {
        this.token = token;
        this.prefix = prefix;
        this.game = game;
    }

    @XmlElement
    public String getToken() {
    	if (token == null)
    		token = "";
    	return token;
    }

    public DiscordBotDatabase setToken(String token) {
        this.token = token;
        DiscordBot.writeDatabase(this);
        return this;
    }
    
    @XmlElement
    public String getPrefix() {
    	if (prefix == null)
    		prefix = ".";
        return prefix;
    }

    public DiscordBotDatabase setPrefix(String prefix) {
        this.prefix = prefix;
        DiscordBot.writeDatabase(this);
        return this;
    }
    
    @XmlElement
    public String getGame() {
    	if (game == null)
    		game = "something cool";
        return game;
    }

    public DiscordBotDatabase setGame(String game) {
        this.game = game;
        DiscordBot.writeDatabase(this);
        return this;
    }
}