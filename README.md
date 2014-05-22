Grepp
=====

# Overview

An attempt to build an ultimate text-file analysing tool.
* Java for cross-platforming
* Groovy to make it more dynamic
* Gradle to flex the distribution process

## Features

### Searching the file

Regex-based, i.e. regexes are enabled by default. Which doesn't restrict you to search for simple strings and means you would need to escape regex characters if they shouldn't be treated as a regex. 

A no-regex option is provided as well, if you really need to match regex special symbols in your pattern.

### Splitting file into log entries

You can specify a regex, or some static text which will be used to 'split' the file into the entries. It will literally split the file, since each match would be treated as the end and start of the entry at the same time.

Or, you can specify and start and end pattern (e.g. &lt;root&gt; and &lt;/root&gt;) to check certain blocks of text.

### Narrowing search by date range

You can specify a date extraction regex & date format; which will be used to filter your search results and to display only the relevant matches. 

Is regex-based, though common date formats would be provided out of the box and most likely you'll just need to choose the one which suits you.

### Joining your threads

Sometimes it's essential to get the whole logging info for a thread, where something interesting has happened. So why not?
Specify thread start pattern, thread end pattern and grepp will join them into one piece and order by the appearance order.

### Parse most of a regex configuration required from your logging subsystem configuration file

Just point grepp to a properties file and let it suck all the info it can find, so you can work with all the above straight away. Supports basic log4j.properties now.

Other logging configuration systems could be supported via plugins.

### Build a report from log file

Different sorts of: group by pattern, count stuff, calculate average (i.e. of your response times). Dump it to a csv (so you can build a nice graph). Or make your own report aggregator, plug it in and dump it to whatever format you want.

Reporting accepts plugging new methods in (like if you want a 90% time calculation and it's not implemented). Thanks to Groovy.

TODO: add a DSL for it, so plugging in is easier than writing a full blown java/groovy Class
TODO: add a DSL for report aggregation too

### Pluggable features

Thanks to Groovy, you can write your own filter to do the stuff you want. It can replace the originals too. Make it configurable from the grepp config, add new config entries or make it static and just add it to the chain just by a flag. Whatever, if you need something special - just put it there.

TODO: add a DSL for it, so plugging in is easier than writing a full blown java/groovy Class

### Good-old CLI interface

Sweet old school typing in the console to preserve the strong geek look and mesmerize non-IT people. Yeah.

TODO: actually a web-interface would be available at some point (i.e. a grepp-server). And a javaFX UI too may be.

# Download

Releases are available from SourceForge: https://sourceforge.net/projects/grepp/?source=directory

# Build

Uses a Gradle Wrapper, so just pull the repo, and _gradlew install_ which will build a version for your current Java version.

It would appear in the _greppInstall_ folder:
````
grepp/
 |-- greppInstall/
 |----- bin/ 
 |----- config/ 
 |----- lib/ 
 |----- grepp.readme
````

# Documentation

Run _grepp -h_ to display usage tips.

TODO: add a Wiki at the SourceForge probably