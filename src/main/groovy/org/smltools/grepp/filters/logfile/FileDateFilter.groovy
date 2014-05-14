package org.smltools.grepp.filters.logfile

import java.text.SimpleDateFormat
import java.util.regex.Matcher

import groovy.util.logging.Slf4j
import groovy.xml.dom.DOMCategory
import org.smltools.grepp.filters.enums.Event
import org.smltools.grepp.filters.RefreshableFilterBase
import org.smltools.grepp.filters.FilterParams

/**
 * Provides filtering of supplied files by last modified date. <br>
 * If it does not comply to from date and to date + configured file threshold, a file would be skipped. 
 * 
 * @author Alexander Semelit
 *
 */

@FilterParams(order = 0)
public class FileDateFilter extends RefreshableFilterBase<List<File>> {
    //Checking dates everywhere

    protected SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    protected Date from = null
    protected Date to = null
    protected long logFileThreshold = -1L //means no threshold at all
    protected long logFileThresholdMltplr = 60L*60L*1000L

    public FileDateFilter(Date from, Date to, Long logFileThreshold)
    {
        super(FileDateFilter.class)

        if (from == null && to == null) {
            throw new IllegalArgumentException("Either 'from' or 'to' should be supplied")
        }

        this.from = from
        this.to = to

        if (logFileThreshold != null) {
            this.logFileThreshold = logFileThreshold
        }
    }
	
    public void setFileDateOutputFormat(String fileDateFormat) {
        this.fileDateFormat = new SimpleDateFormat(fileDateFormat)
    }


    public void setFrom(Date from) {
        this.from = from;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    /**
    * Creates FileDateFilter from config
    *
    */
    public FileDateFilter(Map<?, ?> config, String configId) {
        super(FileDateFilter.class, config);
        fillParamsByConfigIdInternal(configId);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean fillParamsByConfigIdInternal(String configId) {
        if (!EntryDateFilter.configIdExists(config, configId)) {
            return false
        }

        def customCfg = config.savedConfigs."$configId"

        if (customCfg.containsKey(LOG_THRESHOLD_KEY)) {
            logThreshold = customCfg.logThreshold
            return true;
        }
        else {
            LOGGER.debug("Log threshold is not configured for config: {}", configId)
            return false;
        }
    }

	/**
    * Checks supplied files if they exceed supplied time boundaries. <br>
    * 
    * @param files List to check
    * @return passed files List
    * @throws IllegalArgumentException if supplied argument is not instanceof List<Files>
    */
    @Override
    public List<File> filter(List<File> files) {
        if (from == null && to == null) {
            throw new IllegalStateException("Either 'from' or 'to' should be supplied to the filter");
        }

        LOGGER.trace("total files: {}", files.size())
        return files.findAll { file -> checkFileTime(file) }
    }

    /**
     * Checks if supplied files fits into configured time boundaries
     * 
     * @param file to be checked
     * @return true if it fits, false otherwise
     */
	boolean checkFileTime(File file)
    {
        if (file == null) return
        Date fileTime = new Date(file.lastModified())
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("fileTime: {}\nChecking if file suits FROM {}", fileDateFormat.format(fileTime), from == null ? null : fileDateFormat.format(from))
        }
        if (from == null || !fileTime.before(from))
        {
            if (to != null)
            {
                if (LOGGER.isTraceEnabled()) LOGGER.trace(" Checking if file suits TO {}", fileDateFormat.format(to))
                if (!fileTime.after(to)) {
                    return true
                }
                if (to.before(fileTime)) {
                    LOGGER.trace("Passed to")
                    if ((logFileThreshold == -1) || fileTime.before(new Date(to.getTime() + logFileThreshold*logFileThresholdMltplr))) return file
                    else
                    {
                        LOGGER.trace("File is too far")
                        return false
                    }
                }
            }
            LOGGER.trace("Passed from only")
            return true
        }
        else
        {
            LOGGER.trace("Not passed")
            return false
        }
    }

}