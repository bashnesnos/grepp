wgrep
=====

Mixing groovy and java to build useful CLI log-analysing tool.

Configured gradle build, which will fetch needed libs etc. is included.
Once pulled you can run "$wgrep_home/gradle test" to gather dependencies and see if it compiles and all is good.
As it is done, the next step can be "gradle install", which will create start scripts, gather jar's and everything for you in releaseInstall folder. It will have basic configuration only though.

For deeper configuration fill in *_tmplt files and remove "_tmplt" suffix in $wgrep_home/dev or $wgrep_home/release folders. 
Each of those will be applied for packaging via "gradle installDev" or "gradle install" correspondingly.

Try "wgrep -?" for further clues
To make some configuration from scratch having just your ".poperties" file, use "wgrep --parse logconfig.properties". It'll fill config.xml and give you a hint.

TODO:
Add some explanatory/description methods for existing options
May be add configuration for properties parsing.
