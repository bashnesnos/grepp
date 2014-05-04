package org.smltools.grepp.config;

import groovy.util.ConfigObject;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** 
 * 
 * As much immutable version of ConfigObject as possible to represent Grepp config file
 * 
 * @author asemelit
 */

public abstract class ConfigHolder extends ConfigObject {

    public abstract void addAndSave(ConfigObject newSubConfig);

    /**
     * Builds the following structure:
     *  {
     *      "-" : {
     *              "L":"Flag to use a following pattern as current entry pattern"
     *              ...
     *            } 
     *      "--" : {
     *              "dtime":"Turn on date time filtering and accept datetime boundaries"
     *              }
     *  }
     * @return Map containing all options with description including filters etc. grouped by corresponding prefix ('-' or '--')
     */
    public Map<String, ?> getOptions() {
        Map<String, ?> result = [:]
        result["-"] = [:]
        result["--"] = [:]
        def addFlag = { name, descr ->
            result["-"][name] = [descr]
        }
        
        def addOpt = { name, descr ->
            def descrList = result["--"][name]
            if (descrList == null) {
                result["--"][name] = [descr]
            }
            else {
                descrList.add(descr)
            }
        }
        
//        withRoot { root ->
//            root.options.opt.each {
//                String name = it.text()
//                String descr = it.'@descr'
//                if (descr != null) {
//                    if (name.length() > 1) {
//                        addOpt(name, descr)
//                    }
//                    else {
//                        addFlag(name, descr)
//                    }
//                }
//            }
            //
//            root.custom.filters.filter.each { filter ->
//                String tags = filter.'@tags'
//                if (tags != null) {
//                    tags.split(", ?").each { tag ->  
//                        addOpt(tag, "Filter. /${filter.text()}/")   
//                    }
//                }
//            }
            //
//            root.custom.pp_splitters.splitter.each{ splitter ->
//                String tags = splitter.'@tags'
//                if (tags != null) {
//                    tags.split(", ?").each { tag ->
//                        addOpt(tag, "Post filter. Type: ${splitter.'@type'} Ptrn: /${splitter.text()}/")
//                    }
//                }
//            }
            //
//            root.custom.thread_configs.extractors.pattern.each { extrctr -> 
//                String tags = extrctr.'@tags'
//                if (tags != null) {
//                    tags.split(", ?").each { tag ->
//                        addOpt(tag, "Thread. Start extractor. /${extrctr.text()}/")
//                    }
//                }
//            }
//
//            root.custom.thread_configs.skipends.pattern.each { skipend ->
//                String tags = skipend.'@tags'
//                if (tags != null) {
//                    tags.split(", ?").each { tag ->
//                        addOpt(tag, "Thread. Skip ends. /${skipend.text()}/")
//                    }
//                }
//            }
//
//            root.custom.thread_configs.ends.pattern.each { end ->
//                String tags = end.'@tags'
//                if (tags != null) {
//                    tags.split(", ?").each { tag ->
//                        addOpt(tag, "Thread. End. /${end.text()}/")
//                    }
//                }
//            }
//        }
        
        return result;
    }

}
