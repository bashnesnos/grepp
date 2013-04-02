package org.smlt.tools.wgrep

import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.varparsers.*

/**
* Facade-Singleton containing all the configuration and is linked with main modules.
* <p>
* As well is carrying out config parsing, incoming variable parsing, modules initialization.
* Pretty much holds most of the program params.
*/

class WgrepFacade {
    //internal
    def cfgDoc = null
    def root = null
    
    private static WgrepFacade facadeInstance
    
    /**
    * Creates the only facade instance.
    * @param args Params needed for the facade initialization. Currently only path to the config.xml is expected.
    * @return <code>facadeInstance</code>
    */

    static WgrepFacade getInstance(def args) { 
        if (facadeInstance == null) 
        {
            facadeInstance = new WgrepFacade(args)
            facadeInstance.initParsers()
        }
        return facadeInstance
    }

    /**
    * Nullifies facadeInstance. It allows to recreate the Facade. Used for test purposes.
    * @param args Params needed for the facade initialization. Currently only path to the config.xml is expected.
    * @return <code>facadeInstance</code>
    */

    static void reset()
    {
        facadeInstance = null        
    }

    //GLOBAL
    def FOLDER_SEPARATOR = null
    def CWD = null
    def HOME_DIR = null
    def RESULTS_DIR = 'results'
    def SPOOLING_EXT = 'log'

    
    //GENERAL
    def FILES = []

    //OPTIONS
    def VERBOSE = null
    def TRACE = null
    def varParsers = [] //organized as LIFO
    def params = [:] //all params as a Map

    LogEntryParser lentryParser = null 
    FilterParser filterParser =  null 
    FileNameParser fileNameParser =  null 

    PatternAutomationHelper paHelper
    FileProcessor fProcessor

    /**
    * Constructor
    * <p>
    * Initializes the instance. Parses config.xml and loads defaults from there.
    *
    * @param args Params needed for the facade initialization. Currently only path to the config.xml is expected. 
    */
    WgrepFacade(def args)
    {
        cfgDoc = DOMBuilder.parse(new FileReader(args[0]))     
        root = cfgDoc.documentElement
        CWD = System.getProperty("user.dir")
        this.loadDefaults()

    }

    /**
    *  Method loads default mode and spooling extension as configured in config.xml
    */
    def loadDefaults()
    {
        FOLDER_SEPARATOR = System.getProperty("file.separator")
        HOME_DIR = System.getProperty("wgrep.home") + FOLDER_SEPARATOR
        if (FOLDER_SEPARATOR == "\\") FOLDER_SEPARATOR += "\\"
        use(DOMCategory)
        {
            setSpoolingExt(root.global.spooling[0].text())
        }
    }
   
    def initParsers() {
        lentryParser = new LogEntryParser()
        filterParser = new FilterParser()
        fileNameParser = new FileNameParser()
        fileNameParser.subscribe()
        filterParser.subscribe()
        lentryParser.subscribe() 
    }

    // Getters
    
    /**
    * Getter for parsed <code>documentElement</code> of the parsed config.xml
    * @return Value of <code>root</code>
    */

    def getRoot()
    {
        return root
    }

    /**
    * Getter to extract CDATA element value from a node which is expected to be text.
    * @return <code>node.text()</code> if the node has text. Value of CDATA element i.e. <code>node.getFirstChild().getNodeValue()</code> otherwise.
    */

    def getCDATA(def node)
    {
        if (node == null) return
        use(DOMCategory)
        {
            def txt = node.text()
            return (txt)?txt:node.getFirstChild().getNodeValue()
        }
    }

    /**
    * Gets value of the {@link this.extraParams} by key.
    * @param field Key for <code>extraParams</code> which is needed to be get.
    * @return Value set to the key <code>field</code>
    */

    def getParam(def field)
    {
        if (isTraceEnabled()) trace("Accessing param: " + field)
        return hasField(field) ? this."$field" : this.params[field]
    }
   
    // Setters

    /**
    * Sets value of any field which exists in <code>WgrepFacade</code> via reflection. Is used to propagate value directly from config.xml
    * @param field Name of field of <code>WgrepFacade</code> which is needed to be set.
    * @param val Value to be set
    */

    def setParam(def field, def val)
    {
        if (isTraceEnabled()) trace("Setting param: " + field + " val: " + val)
        if (hasField(field))
        {
            this."$field" = val
        }
        else
        {
            this.params[field] = val
        }
    }

    /**
    * Sets value of <code>LOG_ENTRY_PATTERN</code> field which exists in <code>WgrepFacade</code> in a classical setter way.
    * @param val <code>String</code> value to be set
    */

    def setLogEntryPattern(def val)
    {
        setParam('LOG_ENTRY_PATTERN', val)
    }
    
    /**
    * Sets value of <code>FILTER_PATTERN</code> field which exists in <code>WgrepFacade</code> in a classical setter way. If extended pattern processing is enabled it will pre-processed to extract the left-most pattern first.
    * @param val <code>String</code> value to be set
    */

    def setFilterPattern(def val)
    {
        setParam('FILTER_PATTERN', val)
    }

    /**
    * Sets value of <code>SPOOLING_EXT</code> field which exists in <code>WgrepFacade</code> in a classical setter way.
    * @param val <code>String</code> value to be set
    */

    def setSpoolingExt(def val)
    {
        setParam('SPOOLING_EXT', val)
    }

    /**
    * Sets value of <code>FILES</code> field which exists in <code>WgrepFacade</code> in a classical setter way. Initializes {@link FileProcessor} at the same time.
    * @param val <code>String</code> value to be set
    */

    def setFileName(def val)
    {
        if (val == null) return
        FILES.add(val)
    }

    /**
    * Enables extended pattern processing.
    * @param field Field to be set. Either <code>EXTNDD_PATTERN</code> for just enabling or <code>PRESERVE_THREAD</code> if it should enable thread parsing as well.
    * @param val <code>String</code> value to be set. Either <code>e</code> if it just enabling, and a valid config preset tag from <code>thread_configs</code> section otherwise.
    */

    def setExtendedPattern(def field, def val)
    {
        setParam(field, val)
    }

    /**
    * Enables post processing.
    * @param field Field to be set
    * @param val <code>String</code> value to be set. Valid config preset tag from <code>pp_splitters</code> section is expected here.
    */

    def setPostProcessing(def field, def val)
    {
        setParam(field, val)
    }

    /**
    * Enables post processing. Initializes {@link DateTimeVarParser}.
    * @param field Field to be set
    * @param val <code>String</code> value to be set. Valid config preset tag from <code>date_time_config</code> section is expected here.
    */

    def setDateTimeFilter(def field, def val)
    {
        setParam(field, val)
        new DateTimeParser().subscribe()
    }

    /**
    * Enables <code>LOG_ENTRY_PATTERN</code>, <code>FILTER_PATTERN</code>, <code>PRESERVE_THREAD</code> auto-identification based on supplied <code>level</code>. Initializes {@link PatternAutomationHelper}.
    * @param field Field to be set
    * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
    */

    def setAutomation(def field, def val)
    {
        lentryParser.unsubscribe()   
        setParam(field, val)
    }

    /**
    * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. If pattern automation.
    * @param field Field to be set
    * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
    */
    def setPredefined(def field, def val)
    {
        filterParser.unsubscribe()   
        setParam(field, val)
    }

    /**
    * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. If pattern automation.
    * @param field Field to be set
    * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
    */
    def setPredefinedConfig(def field, def val)
    {
        lentryParser.unsubscribe() 
        if (getParam('ATMTN_LEVEL') == null) setParam('ATMTN_LEVEL', 'a')   
        setParam(field, val)
    }

    // INITIALIZATION    

    /**
    * Main method for the command-line arguments processing.
    * <p>
    * It processes arguments in the following way:
    *   <li>1. Flags starting with - and options starting with --</li>
    *   <li>2. {@link this.LOG_ENTRY_PATTERN} if not overrided by any flag/option</li>
    *   <li>3. {@link this.FILTER_PATTERN} if not overrided by any flag/option</li>
    *   <li>4. Additional modules, like {@link DateTimeChecker} variables</li>
    *   <li>5. All other are treated as consequential filenames to be checked</li>
    * <p>
    * As soon as they are processed, it starts module initialization.
    * @param args Array of strings containing arguments for parsing.
    */

    def processInVars(def args)
    {
        for (arg in args)
        {
            if (isTraceEnabled()) trace("Next argument " + arg)

            switch (processOptions(arg))
            {
                case -1 : 
                    return -1
                    break
                case 1:
                    break
                default :
                    parseVar(arg)
            }
        }
        moduleInit()
    }

    /**
    * Method for flags and options parsing. It identifies if passed help flag, simple flag or option, and calls appropriate method. Also it removes special symbols - and -- before passing argument further.
    *
    * @param arg An argument to be parsed
    * @return <code>1</code> if <code>arg</code> was processed(i.e. it was a valid arg) <code>null</code> otherwise.
    */

    int processOptions(def arg)
    {
        if (arg ==~ /\?/) return printHelp()
        if (arg =~/^-(?!-)/)
        {
            processSimpleArg(arg.substring(1)) //skipping '-' itself
            return 1
        }
        else if (arg =~ /^--/)
        {
            processComlpexArg(arg.substring(2)) //skipping '--' itself
            return 1
        }
        return 0
    }
    
    /**
    * Method for simple flags. It tokenizes passed string by each symbol. 
    * I.e. for each character it will be looking for corresponding optiong in config.xml
    * Such behaviour was introduced to support multiple flags at once, like '-abcd'. 
    * For each tokenized character it calls {@link processOption} method.
    *
    * @param arg An argument to be parsed
    */
    def processSimpleArg(def arg)
    {
        (arg =~ /./).each{opt -> processOption(opt)}
    }

    /**
    * Method for complex flags/options. There is no complex logic at the moment.
    * It fetches every character from string and passes the result to {@link processOption} method.
    *
    * @param arg An argument to be parsed
    */

    def processComlpexArg(def arg)
    {
        (arg =~ /.*/).each{opt -> if(opt) processOption(opt)}
    }

    /**
    * Method which performs actual option lookup in the config.xml.
    * <p>
    * It fetches handler function (from <code>handler</code> attribute), and calls it from {@link WgrepFacade} class.
    * <p> It passes to the handler function <code>field</code> attribute and value of matching option from config.xml.
    *
    * @param arg An argument to be looked up
    * @throws IllegalArgumentException If the supplied <code>arg</code> is not configured, i.e. cannot be found in the config.xml.
    */

    def processOption(def opt)
    {
        use(DOMCategory)
        {
            def optElem = root.options.opt.find {it.text() == opt}
            if (optElem == null) optElem = root.custom.options.opt.find {it.text() == opt}
            if (optElem != null)
            {
                def handler = optElem['@handler']
                facadeInstance."$handler"(optElem['@field'], optElem.text())
            }
            else throw new IllegalArgumentException("Invalid option=" + opt)
        }
    }

    /**
    * Method for parsing variables.
    * <p>
    * It gets first parser from {@link varParsers} array and calls it <code>parseVar</code> function with supplied <code>arg</code>
    *
    * @param arg An argument to be parsed
    * @return <code>1</code> if <code>arg</code> was processed(i.e. if there was any {@link AdditionalVarParser} subscribed) <code>null</code> otherwise.
    */

    def parseVar(def arg)
    {
        if (isTraceEnabled()) trace("attempting to parse with parsers: " + varParsers)
        def nexParserIdx = varParsers.size() - 1
        if (nexParserIdx >= 0)
        {
            varParsers[nexParserIdx].parseVar(arg)
        }
    }

    /**
    * Method checks if all the main vars are not nulls.
    * @return <code>1</code> if check is passed. <code>null</code> otherwise.
    */

    def checkVars()
    {
        
        if (FILES.isEmpty())
        {
            println "No file to wgrep"
            return false
        }

        if (getParam('LOG_ENTRY_PATTERN') == null)
        {
            println "No log entry pattern. Can't split the log records."
            return false
        }

        if (getParam('FILTER_PATTERN') == null)
        {
            println "No filter pattern. To list all the file better use less"
            return false
        }

        return true
    }


    /**
    * Method for subscribing additional parsers.
    *
    * @param parsers Collection of {@link AdditionalVarParser} objects.
    */

    def subscribeVarParsers(def parsers)
    {
        varParsers.addAll(parsers)
    }

    /**
    * Method for unsubscribing additional parsers.
    *
    * @param parsers Collection of {@link AdditionalVarParser} objects.
    */

    def unsubscribeVarParsers(def parsers)
    {
        varParsers.removeAll(parsers)
    }


    // Handlers implementation
    
    def isVerboseEnabled()
    {
        return VERBOSE != null || TRACE != null
    }

    /**
    * Prints supplied string if {@link VERBOSE} is <code>true</code>
    *
    * @param text A String to be verbosed
    */
    def verbose(def text)
    {
        if (isVerboseEnabled()) println text
    }
    
    def isTraceEnabled()
    {
        return TRACE != null
    }

    /**
    * Prints supplied string if {@link TRACE} is <code>true</code>
    *
    * @param text A String to be traced
    */

    def trace(def text)
    {
        if (isTraceEnabled()) println '###TRACE### ' + text
    }

   
    //General

    boolean hasField(def field)
    {
        try {
            this.getClass().getDeclaredField(field)
        }
        catch (NoSuchFieldException e) {
            return false
        }
        return true
    }

    def moduleInit()
    {
        paHelper = PatternAutomationHelper.getInstance()
        fProcessor = FileProcessor.getInstance()
        return 1
    }

/**
    * Initializes spooling, i.e. redirects System.out to a file. 
    * <p>
    * File is created in the {@link this.HOME_DIR} folder with name compiled from {@link this.FILTER_PATTERN} and extension as {@link this.SPOOLING_EXT}
    */

    def spool()
    {
        if (getParam('SPOOLING') != null)
        {
            def resultsDir = new File(HOME_DIR + FOLDER_SEPARATOR + RESULTS_DIR)
            if (!resultsDir.exists()) resultsDir.mkdir()
            def out_file = new File(resultsDir.getAbsolutePath() + FOLDER_SEPARATOR + getParam('FILTER_PATTERN').replaceAll("[^\\p{L}\\p{N}]", {""}) + getParams('SPOOLING_EXT'))
            if (isTraceEnabled()) trace("Creating new file: " + out_file.getAbsolutePath())
            out_file.createNewFile()
            System.setOut(new PrintStream(new FileOutputStream(out_file)))
        }
    }

    /**
    * Method to trigger processing of supplied files. Contains hook to start spooling.
    */

    def startProcessing()
    {
        spool()
        fProcessor.processAll()
    }

    def checkEntryPattern(def fileName)
    {
        if (paHelper != null)
        {
            paHelper.automateByFile(fileName)
        }
    }

    /**
    * Method prints out some help
    * <p> 
    * Actually it has the same date as in groovydoc.
    */
    int printHelp() 
    {
        def help = """\
        CLI program to analyze text files in a regex manner. Adding a feature of a log record splitting, thread-coupling and reporting.

        Usage: 
        java -cp wgrep.jar org.smlt.tools.wgrep.WGrep CONFIG_FILE [-[:option:]] [--:filter_option:] [LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME]
        Usage via supplied .bat or .sh file: 
        wgrep [-[:option:]] [--:filter_option:] [LOG_ENTRY_PATTERN] [FILTER_PATTERN] [--dtime FROM_DATE TO_DATE] FILENAME [FILENAME]

        Examples:

        Using in Windows
        wgrep -as \"SomethingINeedToFind\" \"D:\\myfolder\\LOGS\\myapp\\node*.log*\"
        wgrep -as \"SomethingINeedToFind\" D:\\myfolder\\LOGS\\myapp\\node*.log

        Using on NIX 
        wgrep -a --my_predefined_config --dtime 2011-11-11T11:10 2011-11-11T11:11 myapp.log 
        wgrep -a --my_predefined_config myapp.log 
        wgrep -a 'SomethingINeedToFind' myanotherapp.log 
        wgrep -eas 'RecordShouldContainThis%and%ShouldContainThisAsWell' --dtime 2012-12-12T12 2012-12-12T12:12 thirdapp.log 
        wgrep -ae 'RecordShouldContainThis%and%ShouldContainThisAsWell%or%ItCouldContainThis%and%This' --dtime 2009-09-09T09:00 + thirdapp.log 
        wgrep -as 'SimplyContainsThis' onemoreapp.log1 onemoreapp.log2 onemoreapp.log3 
        """
        println help
        return -1
    }

    /**
    * Main method for printing a block. Checks if it is not null and prints to System.out.
    * @param block Block to be printed
    */

    def printBlock(def block)
    {
        println block
    }


}