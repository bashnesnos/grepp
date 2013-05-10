wgrep
=====

Trying groovy and gradle to build useful CLI log-analysing tool.

Configured gradle build, which will fetch needed libs etc. is included.
Once pulled you can run "$wgrep_home/gradle test" to gather dependencies and see if it compiles and all is good.
As it is done, the next step can be "gradle install", which will create start scripts, gather jar's and everything for you in releaseInstall folder. It will have basic configuration only though.

For deeper configuration fill in *_tmplt files and remove "_tmplt" suffix in $wgrep_home/dev or $wgrep_home/release folders. 
Each of those will be applied for packaging via "gradle installDev" or "gradle install" correspondingly.

TODO:
Try to find out if it is possible to reduce groovy reflection PogoMetaMethodSite effect on performance. UPD: it goes away if printing out is put inside filtering. May be output should decorate filter chain for example.
It seems that automatic logging pattern conversion will require mapping of layouts to regexes, may be it is convinient to make a separate tool for that.
