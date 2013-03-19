import groovy.xml.dom.DOMCategory
import org.smlt.tools.wgrep.*

def BASE_HOME = System.getProperty("wgrep.home")
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def HOME = BASE_HOME + "\\build\\resources\\test"
def args = ["-it","test", HOME+"\\test_*"]
WgrepFacade facade = WgrepFacade.getInstance([WGREP_CONFIG])
//facade.processInVars(args)
        def level = 'entry_pattern'
        def tag = ''
        def data = 'test_'
        use(DOMCategory)
        {
            def configs = facade.root.custom.config.findAll { it.pattern[0].'@alevel' ==~ level }
            def config = configs.find { config ->
                def mtchr = facade.getCDATA(config.pattern[0])
                println("ptrn=/" + mtchr + "/ data='" + data + "'")
                data =~ mtchr
            }
            if (config) tag = config.'@id'
            println("Tag found="+tag)
        }

//println facade.LOG_ENTRY_PATTERN
//println facade.FILTER_PATTERN 
//println facade.FILES 
//facade.startProcessing()