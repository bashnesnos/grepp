import org.smltools.grepp.filters.ReportAggregatorBase
import org.smltools.grepp.filters.ReportAggregatorParams
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ReportAggregatorParams(id = "piped_text", spoolFileExtension = PipedTextAggregator.SPOOL_FILE_EXTENSION)
public class PipedTextAggregator extends ReportAggregatorBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipedTextAggregator.class);

    private List<String> columns = new ArrayList<String>();
    private StringBuilder aggregator = new StringBuilder();
    private Deque<String> curRowColumns = null;
    public static final String SPOOL_FILE_EXTENSION = "txt";
    public static final String COLUMN_SEPARATOR = "|";

    @Override
    public void addColumn(String columnName) {
        columns.add(columnName);
    }

    @Override
    public PipedTextAggregator addRow() {
        if (columns.isEmpty()) {
            throw new IllegalStateException("Columns should be supplied to determine number of cells in a row")
        }

        if (aggregator.length() > 0) {
            LOGGER.trace("Adding next row")
            aggregator.append('\n')
        }

        LOGGER.trace("Refreshing current row columns")
        curRowColumns = new ArrayDeque<String>(columns) //copying
        return this
    }

    @Override
    public PipedTextAggregator addCell(String value) {
        if (!curRowColumns.isEmpty()) {
            LOGGER.trace("Filled {}", curRowColumns.pop())
            if (columns.size() - curRowColumns.size() == 1) {
                aggregator.append(value ?: "")
            }
            else {
                aggregator.append(COLUMN_SEPARATOR).append(value ?: "")
            }
        }
        else {
            throw new IllegalStateException("Can't add cell, if all the row columns are filled")
        }
        return this
    }

    @Override
    public void flush() {
        aggregator.setLength(0)
        columns.clear()
        curRowColumns.clear()
    }

    @Override
    public PipedTextAggregator addHeader() {
        LOGGER.trace("Adding header row")
        aggregator.append(columns.join(COLUMN_SEPARATOR))
        return this
    }

    @Override
    public String buildRow() {
        if (aggregator.length() > 0) {
            LOGGER.trace("Building current row")
            if (curRowColumns.size() == columns.size()) { //i.e. no cells were filled in the last row
                aggregator.deleteCharAt(aggregator.length() - 1) //deleting carriage return
            }
            String result = aggregator.toString()
            aggregator.setLength(0)
            curRowColumns.clear()  //keeping other state
            return result 
        }
        else {
            return null
        }
    }

    @Override
    public String buildReport() {
        if (aggregator.length() > 0) {
            String result = aggregator.toString()
            flush() //clearing it for the next
            return result
        }
        else {
            flush()
            return null
        }
    }
}