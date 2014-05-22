
import groovy.util.logging.Slf4j
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.smltools.grepp.util.PropertiesParserProcessorBase
import org.smltools.grepp.util.PropertiesParserParams
import groovy.xml.dom.DOMCategory
import groovy.xml.DOMBuilder
import org.smltools.grepp.util.SavedConfigBuilder
import static org.smltools.grepp.util.Log4jLogbackPropUtil.*

@Slf4j("LOGGER")
@PropertiesParserParams(id = "logback")
public class LogbackPropertiesParser extends PropertiesParserProcessorBase {
	/**
	 * Method which does processing of one File, which contains lines. <br>
	 * 
	 * @param data a File which needs to be processed
	 */
    @Override
    protected void processSingleFile(File data) {
		def cfgDoc = DOMBuilder.parse(new FileReader(data))
		def root = cfgDoc.documentElement
		use(DOMCategory) {
			root.appender.each { appender ->
				if (appender.'@class' =~ /FileAppender/) {
					String[] configIdAndfilePattern = findIdAndFileNamePattern(appender.file.text())
					if (configIdAndfilePattern != null) {
						SavedConfigBuilder builder = new SavedConfigBuilder()
						builder.setId(configIdAndfilePattern[0])				
						builder.setPattern(configIdAndfilePattern[1])
						def encoderPattern = appender.encoder.pattern.text()
						builder.setStarter(findStarter(encoderPattern))
						String[] dateRegexAndDateFormat = findDateRegexAndDateFormat(encoderPattern)
						if (dateRegexAndDateFormat != null) {
							builder.setDateRegex(dateRegexAndDateFormat[0])
							builder.setDateFormat(dateRegexAndDateFormat[1])
						}
						else { // no log entry pattern
							LOGGER.info("dateRegex was not identified")
						}
						output.print(builder.buildSavedConfig())
					}
					else {
						LOGGER.info("File pattern not parsed: {}", appender.file[0])
					}
				}
			}
		}		
    }
}