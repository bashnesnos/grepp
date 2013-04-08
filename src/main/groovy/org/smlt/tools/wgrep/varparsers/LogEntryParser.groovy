package org.smlt.tools.wgrep.varparsers

import groovy.util.logging.Slf4j;

@Slf4j
class LogEntryParser extends DefaultVarParser
{
    def parseVar(def arg)
    {
        log.trace("Parsing var: " + arg)
        getFacade().setLogEntryPattern(arg)
        unsubscribe()
    }

}