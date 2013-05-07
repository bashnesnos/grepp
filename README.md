wgrep
=====

Trying groovy and gradle to build useful CLI log-analysing tool.

Has configured gradle build, which will fetch needed libs etc. 
Once pulled you can run "$wgrep_home/gradle makeEmbeddedJar" to build embedded jar containing all the dependencies. For deeper configuration fill ._tmplt files and remove "_tmplt" suffix in $wgrep_home/dev or $wgrep_home/release folders. 
After that you'll be able to execute "gradle releaseDev" or "gradle release" which will allow to distribute jar and other resources into those folders.

TODO:
Introfuce Spring IoC for autowiring of the config with the modules. 
Refactor .bat compiling and application content assembling via gradle 'application' plugin //Could be quite hard due to desired classpath variable and custom JVM opts addition during gradle build. 
Research if logging systems configuration(logback/commons/log4j) files could be parsed into regex patterns and to config's automatically 
