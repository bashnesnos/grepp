import org.smltools.grepp.cli.Grepp
import org.smltools.grepp.filters.FilterParams
import org.smltools.grepp.util.GreppUtil
import groovy.util.GroovyTestCase
import org.smltools.grepp.filters.FilterChain
import org.smltools.grepp.filters.Filter

class GreppDslTest extends GroovyTestCase {
	def HOME = System.getProperty('grepp.home')
	def filterPluginDir = new File(HOME, FilterChain.GREPP_FILTER_PLUGIN_DIR)
	def SEPARATOR = System.getProperty('file.separator')

	void setUp() {
		if (!filterPluginDir.exists() && !filterPluginDir.isDirectory()) {
			throw new RuntimeException('Filter plugin dir not found')
		}
	}

	void testFilterDsl() {
		File dslFilterFile = new File(filterPluginDir, 'Doodki1DslFilter.groovy')
		assertTrue("File not found", dslFilterFile.exists())
		def clazz = GreppUtil.loadFilterClass(dslFilterFile)
		assertTrue("Filter class loading failed", clazz != null)
		assertTrue("Is not a Filter implementation", Filter.class.isAssignableFrom(clazz))
		FilterParams paramsAnnotation = clazz.getAnnotation(FilterParams.class)
		assertTrue("Should have a params annotation", paramsAnnotation != null)
		assertTrue("Should have configIdPath specified", paramsAnnotation.configIdPath() != "")
		assertTrue("Should have order specified", paramsAnnotation.order() != "")
	}

	void testFilterDslInAction() {
		def expectedResult = """\
doodki!
doodki!
doodki!
doodki!"""

		GreppTest.assertGreppOutput(expectedResult) {
			Grepp.main("--doodki1 oo $HOME${SEPARATOR}processing_report_test.log".split(" "))
		}
	}


}