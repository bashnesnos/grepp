wgrep
=====

Trying groovy and gradle to build useful CLI log-analysing tool.

Has configured gradle build, which will fetch needed libs etc. 
Once pulled you can run "$wgrep_home/gradle makeEmbeddedJar" to build embedded jar containing all the dependencies. For deeper configuration fill ._tmplt files and remove "_tmplt" suffix in $wgrep_home/dev or $wgrep_home/release folders. 
After that you'll be able to execute "gradle releaseDev" or "gradle release" which will allow to distribute jar and other resources into those folders.

TODO:
Spring bean-context introduced. Now deal with actual dependency injection.  
Research if logging systems configuration(logback/commons/log4j) files could be parsed into regex patterns and to config's automatically 
