package org.smlt.tools.wgrep

import groovy.xml.DOMBuilder
import groovy.xml.dom.DOMCategory

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
        if (!facadeInstance) 
        {
            facadeInstance = new WgrepFacade(args)
        }
        return facadeInstance
    }

    //GLOBAL
    def FOLDER_SEPARATOR = null
    def CWD = null
    def HOME_DIR = null
    def RESULTS_DIR = 'results'
    
    //GENERAL
    def LOG_ENTRY_PATTERN = null
    def LEP_OVERRIDED = null
    def FILTER_PATTERN = null
    def FP_OVERRIDED = null
    def FILES = []

    //OPTIONS
    def VERBOSE = null
    def TRACE = null
    def SPOOLING = null
        def SPOOLING_EXT = null
    def FILE_MERGING = null
    def ATMTN_LEVEL = null
        PatternAutomationHelper paHelper
    def PREDEF_TAG = null
    def EXTNDD_PATTERN = null
        def PRESERVE_THREAD = null
        ExtendedPatternProcessor epProcessor
    def POST_PROCESSING = null
        PostProcessor pProcessor 
    def DATE_TIME_FILTER = null
        DateTimeChecker dtChecker
    def additionalVarParsers = []
    def extraParams = [:]

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
    //General
    
    /**
    * Main method for printing a block. Checks if it is not null and prints to System.out.
    * @param block Block to be printed
    */

    def printBlock(def block)
    {
        if (block) println block
    }

    /**
    * Method to trigger processing of supplied files. Contains hook to start spooling.
    */

    def startProcessing()
    {
        spool()
        fProcessor.processAll()
    }

    def moduleInit()
    {
        if (ATMTN_LEVEL) paHelper = new PatternAutomationHelper()
        if (EXTNDD_PATTERN || PRESERVE_THREAD) epProcessor = new ExtendedPatternProcessor()
        if (POST_PROCESSING) pProcessor = new PostProcessor()
        if (DATE_TIME_FILTER) dtChecker = new DateTimeChecker()
        fProcessor = new FileProcessor()
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
            return
        }

        if (!LOG_ENTRY_PATTERN)
        {
            println "No log entry pattern. Can't split the log records."
            return
        }

        if (!FILTER_PATTERN)
        {
            println "No filter pattern. To list all the file better use less"
            return
        }

        return 1
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
        if (!node) return
        use(DOMCategory)
        {
            def txt = node.text()
            return (txt)?txt:node.getFirstChild().getNodeValue()
        }
    }

    /**
    * Returns current {@code @DataTimeChecker} instance. Is deprecated, used only by unit-tests.
    * @return Value of <code>dtChecker</code>
    */

    @Deprecated
    def getDTChecker()
    {
        return dtChecker
    }

    /**
    * Gets value of any field which exists in <code>WgrepFacade</code> via reflection.
    * @param field Name of field of <code>WgrepFacade</code> which is needed to be get.
    * @return Value of <code>WgrepFacade.'field'</code>
    */

    def getParam(def field)
    {
        trace("Accessing main param: " + field)
        return this."$field"
    }

    /**
    * Gets value of the {@link this.extraParams} by key.
    * @param field Key for <code>extraParams</code> which is needed to be get.
    * @return Value set to the key <code>field</code>
    */

    def getExtraParam(def field)
    {
        trace("Accessing extra param: " + field)
        return this.extraParams[field]
    }

    // Setters

    /**
    * Sets value of any field which exists in <code>WgrepFacade</code> via reflection. Is used to propagate value directly from config.xml
    * @param field Name of field of <code>WgrepFacade</code> which is needed to be set.
    * @param val Value to be set
    */

    def setParam(def field, def val)
    {
        trace("Setting main param: " + field + " val: " + val)
        this."$field" = val
    }

    /**
    * Adds/updates value of the {@link this.extraParams} by key.
    * @param field Key for <code>extraParams</code> which is needed to be get.
    * @param val Value to be set
    */

    def setExtraParam(def field, def val)
    {
        this.extraParams[field] = val
    }

    /**
    * Sets value of <code>LOG_ENTRY_PATTERN</code> field which exists in <code>WgrepFacade</code> in a classical setter way.
    * @param val <code>String</code> value to be set
    */

    def setLogEntryPattern(def val)
    {
        LOG_ENTRY_PATTERN = val
        trace("Set LOG_ENTRY_PATTERN=/"+LOG_ENTRY_PATTERN+"/")
    }
    
    /**
    * Sets value of <code>FILTER_PATTERN</code> field which exists in <code>WgrepFacade</code> in a classical setter way. If extended pattern processing is enabled it will pre-processed to extract the left-most pattern first.
    * @param val <code>String</code> value to be set
    */

    def setFilterPattern(def val)
    {
        if (val)
        {
            FILTER_PATTERN = val
            trace("Set FILTER_PATTERN=/"+FILTER_PATTERN+"/")
        }
    }

    /**
    * Sets value of <code>SPOOLING_EXT</code> field which exists in <code>WgrepFacade</code> in a classical setter way.
    * @param val <code>String</code> value to be set
    */

    def setSpoolingExt(def val)
    {
        SPOOLING_EXT = val
    }

    /**
    * Sets value of <code>FILES</code> field which exists in <code>WgrepFacade</code> in a classical setter way. Initializes {@link FileProcessor} at the same time.
    * @param val <code>String</code> value to be set
    */

    def setFileName(def val)
    {
        if (!val) return
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
        new DateTimeVarParser().subscribe()
    }

    /**
    * Enables <code>LOG_ENTRY_PATTERN</code>, <code>FILTER_PATTERN</code>, <code>PRESERVE_THREAD</code> auto-identification based on supplied <code>level</code>. Initializes {@link PatternAutomationHelper}.
    * @param field Field to be set
    * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
    */

    def setAutomation(def field, def val)
    {
        setParam('LEP_OVERRIDED', '1')   
        setParam(field, val)
    }

    /**
    * Sets <code>FILTER_PATTERN</code> according to on supplied <code>tag</code> from <code>filters</code> section of config.xml. If pattern automation.
    * @param field Field to be set
    * @param val <code>String</code> value to be set. Valid config preset tag from <code>automation</code> section is expected here.
    */
    def setPredefined(def field, def val)
    {
        setParam('FP_OVERRIDED', '1')   
        setParam(field, val)
    }

    /**
    * Initializes spooling, i.e. redirects System.out to a file. 
    * <p>
    * File is created in the {@link this.HOME_DIR} folder with name compiled from {@link this.FILTER_PATTERN} and extension as {@link this.SPOOLING_EXT}
    */

    def spool()
    {
        if (SPOOLING)
        {
            def out_file = new File(HOME_DIR + FOLDER_SEPARATOR + RESULTS_DIR + FOLDER_SEPARATOR + FILTER_PATTERN.replaceAll("[^\\p{L}\\p{N}]", {""}) + SPOOLING_EXT)
            trace("Creating new file: " + out_file.getAbsolutePath())
            out_file.createNewFile()
            System.setOut(new PrintStream(new FileOutputStream(out_file)))
        }
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
            trace("Next argument " + arg)

            if(processOptions(arg)) continue
            
            if (!LEP_OVERRIDED && !LOG_ENTRY_PATTERN)
            {
                setLogEntryPattern(arg)
                continue
            }

            if (!FP_OVERRIDED && !FILTER_PATTERN) 
            {
                setFilterPattern(arg)
                continue
            }

            if (parseAdditionalVar(arg)) continue
            setFileName(arg)
        }
        moduleInit()
    }

    /**
    * Method for flags and options parsing. It identifies if passed help flag, simple flag or option, and calls appropriate method. Also it removes special symbols - and -- before passing argument further.
    *
    * @param arg An argument to be parsed
    * @return <code>1</code> if <code>arg</code> was processed(i.e. it was a valid arg) <code>null</code> otherwise.
    */

    def processOptions(def arg)
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
        return null
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
            if (!optElem) optElem = root.custom.options.opt.find {it.text() == opt}
            if (optElem)
            {
                def handler = optElem['@handler']
                facadeInstance."$handler"(optElem['@field'], optElem.text())
            }
            else throw new IllegalArgumentException("Invalid option=" + opt)
        }
    }

    /**
    * Method for parsing 'additional' variables.
    * <p>
    * It gets first parser from {@link additionalVarParsers} array and calls it <code>parseVar</code> function with supplied <code>arg</code>
    *
    * @param arg An argument to be parsed
    * @return <code>1</code> if <code>arg</code> was processed(i.e. if there was any {@link AdditionalVarParser} subscribed) <code>null</code> otherwise.
    */

    def parseAdditionalVar(def arg)
    {
        trace("Parsing additional var: " + arg)
        if (additionalVarParsers.size() > 0)
        {
            additionalVarParsers[0].parseVar(arg)
            return 1
        }
        return null
    }

    /**
    * Method for subscribing additional parsers.
    *
    * @param parsers Collection of {@link AdditionalVarParser} objects.
    */

    def subscribeVarParsers(def parsers)
    {
        additionalVarParsers.addAll(parsers)
    }

    /**
    * Method for unsubscribing additional parsers.
    *
    * @param parsers Collection of {@link AdditionalVarParser} objects.
    */

    def unsubscribeVarParsers(def parsers)
    {
        additionalVarParsers.removeAll(parsers)
    }


    // Handlers implementation
    /**
    * Prints supplied string if {@link VERBOSE} is <code>true</code>
    *
    * @param text A String to be verbosed
    */
    def verbose(def text)
    {
        if (VERBOSE) println text
    }
    
    /**
    * Prints supplied string if {@link TRACE} is <code>true</code>
    *
    * @param text A String to be traced
    */

    def trace(def text)
    {
        if (TRACE) println '###TRACE### ' + text
    }

    //FILTERING AND STUFF

    /**
    * Facade method to check if any presets exists for supplied filename. Simply calls {@link PatternAutomationHelper.automateByFile()} method.
    *
    * @param fName A String with filename
    */
    def checkEntryPattern(def fName)
    {
        if (paHelper) paHelper.automateByFile(fName)
    }

    /**
    * Facade method to check if supplied filename, and corresponding {@link File} object suits desired date and time. 
    * Calls {@link dtChecker.check()} method if {@link DATE_TIME_FILTER} is not null.
    *
    * @param fName A String with filename
    */
    def checkFileTime(def fName)
    {
        def file = new File(fName)
        if (!DATE_TIME_FILTER) return file
        else return dtChecker.check(file)
    }
    
    /**
    * Facade method to check if supplied entry suits desired date and time. 
    * Calls {@link dtChecker.check()} method if {@link DATE_TIME_FILTER} and <code>entry</code> are not null.
    *
    * @param entry A String to be checked
    */
    def checkEntryTime(def entry)
    {
        if (entry && DATE_TIME_FILTER)
        {
            return dtChecker.check(entry)
        }
        return true
    }

    /**
    * Combined filter method.
    * <p> 
    * Is called against each block. Current sequence is following:
    * <li>1. Checks if block contains {@link FILTER_PATTERN}</li>
    * <li>2. Passes block and matching result to {@link processComplexBlock} method</li>
    * <li>3. Passes block to {@link postProcessBlockData} method</li>
    * <li>4. Passes the result of step 3 to {@link printBlock} method</li>
    *
    * @param blockData A String to be filtered.
    */

    def filter(def blockData)
    {
        trace("Filtering with /"+FILTER_PATTERN+"/")
        blockData = (blockData =~ FILTER_PATTERN)? processComplexBlock(blockData, 1) : processComplexBlock(blockData, null)
        printBlock(postProcessBlockData(blockData))        
    }

    /**
    * Method for complex pattern processing.
    * <p> 
    * Is called against each block. If {@link EXTNDD_PATTERN} and {@link PRESERVE_THREAD} both are null, return supplied block back.
    * Otherwise passes supplied params to {@link ExtendedPatternProcessor.process()} method.
    *
    * @param blockData A String to be filtered.
    * @param matched An <code>Object</code> or <code>null</code> if the supplied <code>blockData</code> passed <code>FILTER_PATTERN</code> check.
    * @return result of {@link ExtendedPatternProcessor.process()} method.
    */

    def processComplexBlock(def blockData, def matched)
    {
        if (!EXTNDD_PATTERN && !PRESERVE_THREAD) return matched ? blockData : null
        trace("Processing extended pattern")
        return epProcessor.process(blockData, matched)
    }

    /**
    * Method for post processing.
    * <p> 
    * Is called against each block. If {@link POST_PROCESSING} is not null, passes blockData to {@link PostProcessor.process()} method.
    * Otherwise returns <code>blockData</code> back.
    *
    * @param blockData A String to be post processed.
    * @return result of {@link PostProcessor.process()} method or the <code>blockData</code>
    */
    def postProcessBlockData(def blockData)
    {
        if (POST_PROCESSING)
        {
            return pProcessor.process(blockData)
        }
        else return blockData
    }
}