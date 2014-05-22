import org.smltools.grepp.filters.Filter;
import org.smltools.grepp.filters.FilterParams;

@FilterParams(configIdPath = "doodki", isStatic = true, order = 99)
public class DoodkiFilter implements Filter<String> {

    @Override
    public String filter(String blockData) {
        return "doodki!"
    }

}
