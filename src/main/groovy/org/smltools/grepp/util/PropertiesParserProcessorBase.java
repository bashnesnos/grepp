package org.smltools.grepp.util;

import java.util.List;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smltools.grepp.filters.enums.Event;
import org.smltools.grepp.output.GreppOutput;
import org.smltools.grepp.output.RefreshableOutput;
import org.smltools.grepp.processors.DataProcessor;
import org.smltools.grepp.output.GreppOutput;

public abstract class PropertiesParserProcessorBase extends PropertiesParserBase implements DataProcessor<List<File>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesParserFactory.class);

	protected GreppOutput<String> output;

    public void setGreppOutput(GreppOutput<String> output) {
        this.output = output;
    }

    /**
     * Hook method which is called prior to file processing. Needed for check and configInstance refreshing if it is on. 
     * 
     * @param file_ a File instance which is needed to be initialized.
     * @return File instance if it was successfully initialized. null otherwise
     */
    @SuppressWarnings("unchecked")
    protected File initFile(File file) {
        LOGGER.info("Initializating {}", file.getName());
        if (output instanceof RefreshableOutput) {
            ((RefreshableOutput) output).refreshFilters(file.getName());
        }
        return file;
    }

    protected abstract void processSingleFile(File data);


    @Override
    public void process(List<File> data) {
        if (data != null) {
            for (File aFile: data) {
                processSingleFile(initFile(aFile));
                output.processEvent(Event.CHUNK_ENDED);
            }
            output.processEvent(Event.ALL_CHUNKS_PROCESSED);
            output.close();
        }
        else {
            LOGGER.trace("No files given; nothing to process");
        }
    }

}
