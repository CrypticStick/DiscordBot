@ECHO OFF
java -Xdebug -Xrunjdwp:transport=dt_socket,address=8001,server=y,suspend=y -jar DiscordBot.jar