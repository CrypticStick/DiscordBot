package com.Stickles.Discord;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DiscordCommand {
    public String Name();

    public String[] Aliases() default {};

    public String Summary() default "This command does not have a summary yet :/";

    public String Syntax() default "";

    public boolean SpecialPerms() default false;
}