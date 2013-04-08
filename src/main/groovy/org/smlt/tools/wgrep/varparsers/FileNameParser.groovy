package org.smlt.tools.wgrep.varparsers

import groovy.util.logging.Slf4j;

@Slf4j
class FileNameParser extends DefaultVarParser
{
    def parseVar(def arg)
    {
        log.trace("Parsing var: " + arg)
        getFacade().addFileName(arg)
        //not unsubsrcibing since there could be more than one file
    }

}