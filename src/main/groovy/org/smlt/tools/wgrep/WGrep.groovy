package org.smlt.tools.wgrep

/**
* CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.
* <p>
* Usage: java -cp wgrep.jar org.smlt.tools.wgrep.WGrep CONFIG_FILE [-[:option:]] [--:filter_option:] [-L LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME] <br>
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

@Slf4j
class WGrep 
{
	public static void main(String[] args)
	{
		log.debug(args.toString())
		if (args == null) return
		int argsToParseCount = args.size() - 1
		if ( argsToParseCount == 0) return
		
		def WGREP_CONFIG = args[0]
		def WGREP_CONFIG_XSD = WGREP_CONFIG.replace(".xml", ".xsd") //.xsd is assumed to be located near the config.xml
		WgrepConfig config = new WgrepConfig(WGREP_CONFIG, WGREP_CONFIG_XSD)
		WgrepFacade facade = new WgrepFacade(config)
		facade.doProcessing(args[1..argsToParseCount])
	}
}
