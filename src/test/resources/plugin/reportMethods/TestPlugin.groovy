import java.util.regex.Matcher
import org.smltools.grepp.filters.ReportMethodParams
import org.smltools.grepp.filters.ReportMethodBase

@ReportMethodParams(id="test")
class TestMethod extends ReportMethodBase<String> {
    @Override
    public String processMatchResults(Matcher mtchResults, Integer groupIdx) {
        return "test"
    }    
}           
