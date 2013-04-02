package org.smlt.tools.wgrep

/**
* CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.
* <p>
* Usage: java -cp wgrep.jar org.smlt.tools.wgrep.WGrep CONFIG_FILE [-[:option:]] [--:filter_option:] [LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME] <br>
* Usage via supplied .bat or .sh file: wgrep [-[:option:]] [--:filter_option:] [LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME]
* <p>
* Examples:
* <p>
* Using in Windows <br>
* wgrep -as "SomethingINeedToFind" "D:\myfolder\LOGS\myapp\node*.log*" <br>
* wgrep -as "SomethingINeedToFind" D:\myfolder\LOGS\myapp\node*.log*
* <p>
* Using on NIX <br>
* wgrep -a --my_predefined_config --dtime 2011-11-11T11:10 2011-11-11T11:11 myapp.log <br>
* wgrep -a --my_predefined_config myapp.log <br>
* wgrep -a 'SomethingINeedToFind' myanotherapp.log <br>
* wgrep -eas 'RecordShouldContainThis%and%ShouldContainThisAsWell' --dtime 2012-12-12T12 2012-12-12T12:12 thirdapp.log  <br>
* wgrep -ae 'RecordShouldContainThis%and%ShouldContainThisAsWell%or%ItCouldContainThis%and%This' --dtime 2009-09-09T09:00 + thirdapp.log <br>
* wgrep -as 'SimplyContainsThis' onemoreapp.log1 onemoreapp.log2 onemoreapp.log3
* 
* @author Alexander Semelit
*/

class WGrep 
{
	public static void main(String[] args)
	{
		def WGREP_CONFIG = args[0]
		WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])
		def shouldProcess = 1
		if (args.size() > 1) shouldProcess = facade.processInVars(args[1..args.size()-1])
		if (shouldProcess > 0 && facade.check()) facade.startProcessing()
	}
}
