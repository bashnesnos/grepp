import org.smltools.grepp.util.GreppUtil
import groovy.util.GroovyTestCase
import org.smltools.grepp.filters.FilterChain
import org.smltools.grepp.filters.Filter

class GreppDslTest extends GroovyTestCase {
	def HOME = System.getProperty('grepp.home')
	def filterPluginDir = new File(HOME, FilterChain.GREPP_FILTER_PLUGIN_DIR)

	void setUp() {
		if (!filterPluginDir.exists() && !filterPluginDir.isDirectory()) {
			throw new RuntimeException('Filter plugin dir not found')
		}
	}

	void testFilterDsl() {
		File dslFilterFile = new File(filterPluginDir, 'DoodkiDslFilter.groovy')
		assertTrue("File not found", dslFilterFile.exists())
		def clazz = GreppUtil.loadFilterClass(dslFilterFile)
		assertTrue("Filter class loading failed", clazz != null)
		assertTrue("Is not a Filter implementation", Filter.class.isAssignableFrom(clazz))
	}
}