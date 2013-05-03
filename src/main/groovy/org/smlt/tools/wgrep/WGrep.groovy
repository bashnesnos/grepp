package org.smlt.tools.wgrep

/**
* CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.
* <p>
* Usage: java -cp wgrep.jar org.smlt.tools.wgrep.WGrep [CONFIG_FILE] [-[:option:]] [--:filter_option:] [-L LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME] <br>
* Usage via supplied .bat or .sh file: wgrep [-[:option:]] [--:filter_option:] [-L LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME]
* <p>
* Examples:
* <p>
* Using in Windows <br>
* wgrep -s "SomethingINeedToFind" "D:\myfolder\LOGS\myapp\node*.log*" <br>
* wgrep -s "SomethingINeedToFind" D:\myfolder\LOGS\myapp\node*.log* <br>
* wgrep -L "RecordStart" "SomethingINeedToFind" D:\myfolder\LOGS\myapp\node*.log*
* <p>
* Using on NIX <br>
* wgrep --my_predefined_config --dtime 2011-11-11T11:10 2011-11-11T11:11 myapp.log <br>
* wgrep --my_predefined_config myapp.log <br>
* wgrep 'SomethingINeedToFind' myanotherapp.log <br>
* wgrep -s 'RecordShouldContainThis%and%ShouldContainThisAsWell' --dtime 2012-12-12T12 2012-12-12T12:12 thirdapp.log  <br>
* wgrep 'RecordShouldContainThis%and%ShouldContainThisAsWell%or%ItCouldContainThis%and%This' --dtime 2009-09-09T09:00 + thirdapp.log <br>
* wgrep -s 'SimplyContainsThis' onemoreapp.log1 onemoreapp.log2 onemoreapp.log3
* 
* @author Alexander Semelit
*/
import groovy.util.logging.Slf4j
import org.smlt.tools.wgrep.config.WgrepConfig
import org.smlt.tools.wgrep.util.WgrepUtil

@Slf4j
class WGrep 
{

	public static void main(String[] args)
	{
		Date startTime = new Date()
		log.info(args.toString())
		if (args == null) return
		int argsToParseCount = args.size() - 1
		if ( argsToParseCount == 0) return
		
		def argsToParse = args
		def WGREP_CONFIG = argsToParse[0]
		def WGREP_CONFIG_XSD = null
		
		if (WGREP_CONFIG =~ /config.xml$/) {
			argsToParse = args[1..argsToParseCount] //excluding config.xml file path from parameters
		}
		else {
			WGREP_CONFIG = WgrepUtil.getResourcePathOrNull("config.xml") //looking in the classpath
		}

		if (WGREP_CONFIG == null){ // it has not been found
			println "config.xml should be either in classpath or specified explicitly"
			return
		}

		WGREP_CONFIG_XSD = WgrepUtil.getResourcePathOrNull("config.xsd") //looking in the classpath
		if (WGREP_CONFIG_XSD == null){ //xsd has not been found 
			log.warn("config.xsd cannot be found in the classpath. Validation will be skipped")
		}

		WgrepConfig config = new WgrepConfig(WGREP_CONFIG, WGREP_CONFIG_XSD)
		WgrepFacade facade = new WgrepFacade(config)
		facade.doProcessing(argsToParse)
		log.info("Processing time = " + ((new Date().getTime() - startTime.getTime())/1000)) + " sec"
	}


}
