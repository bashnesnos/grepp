import org.smltools.grepp.filters.entry.CsvAggregator
import org.smltools.grepp.filters.ReportAggregatorParams
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ReportAggregatorParams(id = "piped_text", spoolFileExtension = PipedTextAggregator.SPOOL_FILE_EXTENSION)
public class PipedTextAggregator extends CsvAggregator {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipedTextAggregator.class);
    public static final String SPOOL_FILE_EXTENSION = "txt";
    public static final String COLUMN_SEPARATOR = "|";

    @Override
    protected String getSeparator() {
        return PipedTextAggregator.COLUMN_SEPARATOR
    }
}