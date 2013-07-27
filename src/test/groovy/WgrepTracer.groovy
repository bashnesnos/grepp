import org.smlt.tools.wgrep.config.PredictingParamsHolderFactory
import org.smlt.tools.wgrep.config.ConfigHolder
import org.smlt.tools.wgrep.config.Param
import org.smlt.tools.wgrep.WGrep

def BASE_HOME = System.getProperty("wgrep.home")
def HOME = BASE_HOME + "\\build\\resources\\test"
def WGREP_CONFIG = BASE_HOME + "\\build\\resources\\test\\config.xml"
def WGREP_CONFIG_XSD = BASE_HOME + "\\build\\resources\\main\\config\\config.xsd"
def defalutOut = System.out


//def	config = new ConfigHolder(WGREP_CONFIG, WGREP_CONFIG_XSD) 

//config.withRoot{ root ->
//	println root.options.opt[0].text()
//}
//PredictingParamsHolderFactory paramFactory = new PredictingParamsHolderFactory(config)
//def params = paramFactory.getParamsHolder("-tsL stCommand queryTime --some_timings cmd_only_1.log".split(" "))


		def pipeOut = new PipedOutputStream()
		def pipeIn = new PipedInputStream(pipeOut)
		def passToIn = new PrintStream(pipeOut)
		def text = """\
#asda
asdas
#asdas
#sadas
fdsfd
"""
		passToIn.print(text)
		passToIn.close()
		def oldIn = System.in
		System.setIn(pipeIn)
		def expectedResult = """#asda
asdas
#asdas"""


		
		try {
				
			def oldStdout = System.out
			def nPipeOut = new PipedOutputStream()
			def nPipeIn = new PipedInputStream(nPipeOut)
			System.setOut(new PrintStream(nPipeOut))
	
			try {
				WGrep.main("-L # asd".split(" "))
			}
			catch (Exception e) {
				nPipeOut.close()
				System.setOut(oldStdout)
				throw e
			}
			finally {
				System.setOut(oldStdout)
				nPipeOut.close()
			}
	
			def outputReader = new BufferedReader(new InputStreamReader(nPipeIn))
	
			StringBuffer actualResult = new StringBuffer()
			if (outputReader.ready()) {
				def line = outputReader.readLine()
				while (line != null) {
					actualResult.size() > 0 ? actualResult.append('\n').append(line) : actualResult.append(line)
					line = outputReader.readLine()
				}
			}
			println actualResult.toString()
			println "Success!"
		}
		catch (Exception e) {
			pipeIn.close()
			System.setIn(oldIn)
			throw e
		}
		finally {
			pipeIn.close()
			System.setIn(oldIn)
		}
 