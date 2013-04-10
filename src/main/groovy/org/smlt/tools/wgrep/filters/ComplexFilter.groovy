package org.smlt.tools.wgrep.filters

import java.util.regex.Matcher
import org.smlt.tools.wgrep.WgrepConfig
import org.smlt.tools.wgrep.filters.enums.*

import groovy.util.logging.Slf4j;
import groovy.xml.dom.DOMCategory

/**
 * 
 * Class provides in-flight pattern building depending on thread start, thread end patterns. If non specified works in the same way as BasicFilter. <br>
 * Forces multiline regex matching. 
 * 
 * @author Alexander Semelit
 *
 */
@Slf4j
class ComplexFilter extends FilterBase {

	//Complex pattern processing and stuff
	StringBuilder PATTERN = new StringBuilder("(?ms)") //for multiline support
	List EXTNDD_PTTRNS = []
	Map EXTNDD_PTTRN_DICT = [:]

	String pt_tag = null
	Map THRD_START_EXTRCTRS =[:]
	List THRD_START_PTTRNS = []
	List THRD_SKIP_END_PTTRNS = []
	List THRD_END_PTTRNS =[]

	/**
	 * Initializes configInstance, filterPattern and thread coupling config from config.xml
	 * 
	 * @param nextFilter_ next filter in chain
	 * @param config WgrepConfig instance is needed to get supplied params.
	 */
	ComplexFilter(FilterBase nextFilter_, WgrepConfig config)
	{
		super(nextFilter_, config)
		setPattern(getFilterPattern())
		log.trace("Added on top of " + nextFilter.getClass().getCanonicalName())
		pt_tag = getParam('PRESERVE_THREAD')
		use(DOMCategory)
		{
			if (pt_tag != null)
			{
				def extrctrs = getRoot().custom.thread_configs.extractor.findAll { it.'@tags' =~ pt_tag }
				extrctrs.each { THRD_START_EXTRCTRS[it.text()] = it.'@qlfr' }
				def pttrns = getRoot().custom.thread_configs.pattern.findAll { it.'@tags' =~ pt_tag }
				pttrns.each { this."${it.'@clct'}".add(it.text()) }
			}
		}
		processExtendedPattern(filterPtrn)
	}

	@Override
	boolean isConfigValid() {
		boolean checkResult = super.isConfigValid()
		if (getParam('FILTER_PATTERN') == null)
		{
			log.warn('FILTER_PATTERN is not specified')
			checkResult = false
		}
		return checkResult
	}

	/**
	 * Appends to current pattern new part which is could be a thread coupling pattern or just a different thing to look up in the data.
	 * 
	 * @param val pattern to be added
	 * @param qualifier identifies how to conjunct it with previous patterns
	 */

	void addExtendedFilterPattern(String val, String qualifier)
	{
		log.trace("adding complex pattern: val=" + val + " qual=" + qualifier)

		if (qualifier != null) PATTERN = PATTERN.append(Qualifier.valueOf(qualifier).getPattern())
		PATTERN = PATTERN.append(val)

		EXTNDD_PTTRNS.add(val)
		EXTNDD_PTTRN_DICT[val] = qualifier ? Qualifier.valueOf(qualifier) : null

		log.trace(EXTNDD_PTTRNS.toString())
		log.trace(EXTNDD_PTTRN_DICT.toString())
	}

	/**
	 * Removes supplied pattern with it's qualifier if any.
	 * 
	 * @param val pattern for removal
	 */
	void removeExtendedFilterPattern(String val)
	{
		Qualifier qlfr = EXTNDD_PTTRN_DICT[val]
		String ptrn = (qlfr ? qlfr.getPattern() : '') + val
		int ptrnIndex = PATTERN.indexOf(ptrn)
		log.trace('to delete:/' + ptrn +'/ index:' + ptrnIndex)
		if (ptrnIndex != -1)
		{
			PATTERN = PATTERN.delete(ptrnIndex, ptrnIndex + ptrn.length())
			EXTNDD_PTTRNS.remove(val)
			EXTNDD_PTTRN_DICT.remove(val)
		}
	}

	/**
	 * Parses supplied filterPattern. If it contains any qualifiers like %and&|%or% parses them into valid regex representation.
	 * 
	 * @param val pattern String
	 */
	void processExtendedPattern(String val)
	{
		String filterPattern = null
		String qRegex = ""
		Qualifier.each { qRegex += '%' + it + '%|' }
		qRegex = qRegex[0..qRegex.size()-2] //removing last |
		Matcher qualifierMatcher = (val =~ /$qRegex/) //matching any qualifiers with % signs
		if (qualifierMatcher.find())
		{
			log.trace('Processing complex pattern')
			List tokens = val.tokenize("%")
			String nextQualifier = null
			if (tokens != null)
			{
				qRegex = qRegex.replaceAll(/%/, "") //matching only qualifier names
				for (grp in tokens)
				{
					log.trace('Next group in match: ' + grp)
					qualifierMatcher = (grp =~ /$qRegex/)
					if (qualifierMatcher.find())
					{
						nextQualifier = qualifierMatcher[0]
						continue
					}

					addExtendedFilterPattern(grp, nextQualifier)
					nextQualifier = null

				}
			}
			else throw new IllegalArgumentException('Check your complex pattern:/' + val + '/')
		}
		else
		{
			log.trace('No extended pattern supplied, might be a preserve thread')
			addExtendedFilterPattern(val, null)
		}
	}


	/**
	 * Checks is data matches current pattern 
	 * @throws IllegalArgumentException if blockData is not String
	 */

	@Override
	boolean check(def blockData) {
		if (!blockData instanceof String) throw new IllegalArgumentException("blockData should be String")
		setPattern(PATTERN.toString())
		Matcher blockMtchr = blockData =~ filterPtrn
		return blockMtchr.find()
	}

	/**
	 *  Extracts thread start patterns if thread preserve is enabled
	 */
	
	@Override
	public void beforePassing(def blockData) {
		if (isThreadPreserveEnabled())
		{
			extractThreadPatterns(blockData)
		}
	}

	/**
	 * Checks is thread preserving is enabled.
	 * 
	 * @return true if it is
	 */
	boolean isThreadPreserveEnabled()
	{
		return pt_tag != null
	}

	/**
	 * Extracts thread patterns, and adds/keeps them if thread is not yet ended in the logs, or removes them is it has ended.
	 * 
	 * @param data String already matched by filter pattern.
	 */
	void extractThreadPatterns(String data)
	{
		if (searchThreadEnds(data))
		{
			extractThreadStarts(data, "removeThreadStart")
		}
		else
		{
			log.trace("Thread continues. Keeping starts")
			extractThreadStarts(data, "addThreadStart")
		}
	}

	/**
	 * Iterates through <extractor> patterns matched by PRESERVE_THREAD tag. <br>
	 * For each tries to match supplied data, and if it matches passes matched string and qualifier to supplied method. 
	 * 
	 * @param data String already matched by filter pattern.
	 * @param method which will be applied to extracted start and it's qualifier
	 */
	void extractThreadStarts(String data, String method)
	{
		THRD_START_EXTRCTRS.each
		{extrctr, qlfr ->
			log.trace(extrctr);
			Matcher extractorMatcher = (data =~ extrctr);
			if (extractorMatcher.find())
			{
				def start = extractorMatcher[0]
				log.trace("extracted; " + start)
				this."$method"(start, qlfr)
			}
		}
	}

	/**
	 * Applies <pattern> elements having THRD_END_PTTRNS collection as a parameter. <br>
	 * If any is matched current data is considered as end of current log thread if any.
	 * 
	 * @param data String already matched by filter pattern.
	 * @return true if thread end was found, false otherwise
	 */
	boolean searchThreadEnds(String data)
	{
		if (!shouldBeSkipped(data))
		{
			def decision = THRD_END_PTTRNS.find
			{ thrend ->
				log.trace("thrend ptrn: " + thrend);
				data =~ thrend
			}
			return decision != null
		}
		return false
	}

	/**
	 * Applies <pattern> elements having THRD_SKIP_END_PTTRNS collection as a parameter. <br>
	 * If any is matched current data won't be considered as end, it simply won't be checked for end patterns.
	 * 
	 * @param data String already matched by filter pattern.
	 * @return true if data should skip thead end check, false otherwise
	 */
	boolean shouldBeSkipped(String data)
	{
		def decision = THRD_SKIP_END_PTTRNS.find
		{skip->
			log.trace("skip ptrn: " + skip)
			data =~ skip
		}
		return decision != null
	}

	/**
	 * Adds supplied thread start to THRD_START_PTTRNS and to a filterPtrn
	 * 
	 * @param start String representing thread start
	 * @param qlfr qualifier of this thread start
	 */
	void addThreadStart(String start, String qlfr)
	{
		log.trace("adding thread start: " + start);
		if (!THRD_START_PTTRNS.contains(start))
		{
			THRD_START_PTTRNS.add(start)
			addExtendedFilterPattern(start, qlfr)
		}
		else log.trace("Start exists")
	}

	/**
	 * Removes supplied thread start. Qualifier will be identified automatically.
	 *
	 * @param start String representing thread start
	 * @param qlfr not used. Needed for reflection call
	 */
	void removeThreadStart(String start, String qlfr)
	{
		log.trace("removing thread start: " + start);
		THRD_START_PTTRNS.remove(start);
		removeExtendedFilterPattern(start);
	}

}