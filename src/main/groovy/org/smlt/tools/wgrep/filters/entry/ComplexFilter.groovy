package org.smlt.tools.wgrep.filters.entry

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.smlt.tools.wgrep.config.WgrepConfig;
import org.smlt.tools.wgrep.filters.enums.*;
import org.smlt.tools.wgrep.filters.FilterBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Class provides in-flight pattern building depending on thread start, thread end patterns. If non specified works in the same way as BasicFilter. <br>
 * Forces multiline regex matching. 
 * 
 * @author Alexander Semelit
 *
 */

class ComplexFilter extends FilterBase {

	//Complex pattern processing and stuff
	StringBuilder PATTERN = new StringBuilder("(?ms)"); //for multiline support
	List EXTNDD_PTTRNS = new ArrayList();
	Map EXTNDD_PTTRN_DICT = new HashMap();

	Map THRD_START_EXTRCTRS;
	List THRD_START_PTTRNS;
	List THRD_SKIP_END_PTTRNS;
	List THRD_END_PTTRNS;
	private final Logger log = LoggerFactory.getLogger(ComplexFilter.class);

	/**
	 * Initializes configInstance, filterPattern and thread coupling config from config.xml
	 * 
	 * @param nextFilter_ next filter in chain
	 * @param config WgrepConfig instance is needed to get supplied params.
	 */
	ComplexFilter(FilterBase nextFilter_, String filterPattern, Map preserveParams)
	{
		super(nextFilter_);
		THRD_START_EXTRCTRS = preserveParams.get("THRD_START_EXTRCTRS");
		THRD_START_PTTRNS = preserveParams.get("THRD_START_PTTRNS") != null ? preserveParams.get("THRD_START_PTTRNS") : new ArrayList();
		THRD_SKIP_END_PTTRNS= preserveParams.get("THRD_SKIP_END_PTTRNS") != null ? preserveParams.get("THRD_SKIP_END_PTTRNS") : new ArrayList();
		THRD_END_PTTRNS= preserveParams.get("THRD_END_PTTRNS") != null ? preserveParams.get("THRD_END_PTTRNS") : new ArrayList();
		if (this.log.isTraceEnabled()) {
			this.log.trace("Added on top of " + nextFilter.getClass().getCanonicalName());	
		}
		processExtendedPattern(filterPattern);
	}

	/**
	 * Appends to current pattern new part which is could be a thread coupling pattern or just a different thing to look up in the data.
	 * 
	 * @param val pattern to be added
	 * @param qualifier identifies how to conjunct it with previous patterns
	 */

	void addExtendedFilterPattern(String val, String qualifier)
	{
		if (this.log.isTraceEnabled()) this.log.trace("adding complex pattern: val=" + val + " qual=" + qualifier);

		if (qualifier != null) PATTERN = PATTERN.append(Qualifier.valueOf(qualifier).getPattern());
		PATTERN = PATTERN.append(val);

		EXTNDD_PTTRNS.add(val);
		EXTNDD_PTTRN_DICT.put(val, qualifier ? Qualifier.valueOf(qualifier) : null);

		if (this.log.isTraceEnabled()) {
			this.log.trace(EXTNDD_PTTRNS.toString());
			this.log.trace(EXTNDD_PTTRN_DICT.toString());
		}
	}

	/**
	 * Removes supplied pattern with it's qualifier if any.
	 * 
	 * @param val pattern for removal
	 */
	void removeExtendedFilterPattern(String val)
	{
		Qualifier qlfr = EXTNDD_PTTRN_DICT.get(val);
		String ptrn = (qlfr ? qlfr.getPattern() : "") + val;
		int ptrnIndex = PATTERN.indexOf(ptrn);
		if (this.log.isTraceEnabled()) this.log.trace("to delete:/" + ptrn +"/ index:" + ptrnIndex)
		if (ptrnIndex != -1)
		{
			PATTERN = PATTERN.delete(ptrnIndex, ptrnIndex + ptrn.length());
			EXTNDD_PTTRNS.remove(val);
			EXTNDD_PTTRN_DICT.remove(val);
		}
	}

	/**
	 * Parses supplied filterPattern. If it contains any qualifiers like %and&|%or% parses them into valid regex representation.
	 * 
	 * @param val pattern String
	 */
	void processExtendedPattern(String val)
	{
		String filterPattern = null;
		String qRegex = "";
		for (Qualifier it: Qualifier.values()) {
			qRegex += qRegex.size() > 0 ? "|%" + it + "%" : "%" + it + "%";
		}
		qRegex = qRegex.substring(0, qRegex.size()-2); //removing last |
		
		Matcher qualifierMatcher = Pattern.compile(qRegex).matcher(val); //matching any qualifiers with % signs
		if (qualifierMatcher.find())
		{
			if (this.log.isTraceEnabled()) this.log.trace("Processing complex pattern");
			String[] tokens = val.split("%");
			String nextQualifier = null;
			if (tokens != null)
			{
				qRegex = qRegex.replaceAll("%", ""); //matching only qualifier names
				for (String grp : tokens)
				{
					if (this.log.isTraceEnabled()) this.log.trace("Next group in match: " + grp);
					qualifierMatcher = Pattern.compile(qRegex).matcher(grp);
					if (qualifierMatcher.find())
					{
						nextQualifier = qualifierMatcher.group();
						continue;
					}

					addExtendedFilterPattern(grp, nextQualifier);
					nextQualifier = null;

				}
			}
			else throw new IllegalArgumentException("Check your complex pattern:/" + val + "/");
		}
		else
		{
			if (this.log.isTraceEnabled()) this.log.trace("No extended pattern supplied, might be a preserve thread");
			addExtendedFilterPattern(val, null);
		}
	}


	/**
	 * Checks is data matches current pattern 
	 * @throws IllegalArgumentException if blockData is not String
	 */

	@Override
	boolean check(Object blockData) {
		if (!blockData instanceof String) throw new IllegalArgumentException("blockData should be String");
		String filterPtrn = PATTERN.toString();
		Matcher blockMtchr = blockData =~ filterPtrn;
		return blockMtchr.find();
	}

	/**
	 *  Extracts thread start patterns if thread preserve is enabled
	 */
	
	@Override
	public void beforePassing(Object blockData) {
		super.beforePassing(blockData);
		if (isThreadPreserveEnabled())
		{
			extractThreadPatterns(blockData);
		}
	}

	/**
	 * Checks is thread preserving is enabled.
	 * 
	 * @return true if it is
	 */
	boolean isThreadPreserveEnabled()
	{
		return THRD_START_EXTRCTRS != null;
	}

	/**
	 * Extracts thread patterns, and adds/keeps them if thread is not yet ended in the this.logs, or removes them is it has ended.
	 * 
	 * @param data String already matched by filter pattern.
	 */
	void extractThreadPatterns(String data)
	{
		if (searchThreadEnds(data))
		{
			for (Map.Entry<String, String> extractedStart: extractThreadStarts(data)) {
				removeThreadStart(extractedStart.getKey(), extractedStart.getValue());
			}
		}
		else
		{
			if (this.log.isTraceEnabled()) this.log.trace("Thread continues. Keeping starts");
			for (Map.Entry<String, String> extractedStart: extractThreadStarts(data)) {
				addThreadStart(extractedStart.getKey(), extractedStart.getValue());
			}

		}
	}

	/**
	 * Iterates through <extractor> patterns matched by PRESERVE_THREAD tag. <br>
	 * For each tries to match supplied data, and if it matches passes matched string and qualifier to supplied method. 
	 * 
	 * @param data String already matched by filter pattern.
	 * @return collection of pairs <pattern,qualifier>
	 */
	Map<String, String> extractThreadStarts(String data)
	{
		HashMap extractedStarts = new HashMap();
		for (Map.Entry extractorEntry : THRD_START_EXTRCTRS) {
			this.log.trace(extractorEntry.getKey());
			Matcher extractorMatcher = Pattern.compile(extractorEntry.getKey()).matcher(data);
			if (extractorMatcher.find())
			{
				def start = extractorMatcher.group();
				this.log.trace("extracted; " + start);
				extractedStarts.put(start, extractorEntry.getValue());
			}
		}
		return extractedStarts;
	}

	/**
	 * Applies <pattern> elements having THRD_END_PTTRNS collection as a parameter. <br>
	 * If any is matched current data is considered as end of current this.log thread if any.
	 * 
	 * @param data String already matched by filter pattern.
	 * @return true if thread end was found, false otherwise
	 */
	boolean searchThreadEnds(String data)
	{
		if (!shouldBeSkipped(data))
		{
			boolean decision = false;
			Iterator endIter = THRD_END_PTTRNS.iterator();
			while (!decision && endIter.hasNext()) {
				String thrend = endIter.next();
				this.log.trace("thrend ptrn: " + thrend);
				decision = Pattern.compile(thrend).matcher(data).find();
			}
			return decision;
		}
		return false;
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
		boolean decision = false;
		Iterator skipEndIter = THRD_SKIP_END_PTTRNS.iterator();
		while (!decision && skipEndIter.hasNext()) {
			String thrend = skipEndIter.next();
			this.log.trace("thrend ptrn: " + thrend);
			decision = Pattern.compile(thrend).matcher(data).find();
		}
		return decision;
	}

	/**
	 * Adds supplied thread start to THRD_START_PTTRNS and to a filterPtrn
	 * 
	 * @param start String representing thread start
	 * @param qlfr qualifier of this thread start
	 */
	void addThreadStart(String start, String qlfr)
	{
		this.log.trace("adding thread start: " + start);
		if (!THRD_START_PTTRNS.contains(start))
		{
			THRD_START_PTTRNS.add(start);
			addExtendedFilterPattern(start, qlfr);
		}
		else this.log.trace("Start exists");
	}

	/**
	 * Removes supplied thread start. Qualifier will be identified automatically.
	 *
	 * @param start String representing thread start
	 * @param qlfr not used. Needed for reflection call
	 */
	void removeThreadStart(String start, String qlfr)
	{
		this.log.trace("removing thread start: " + start);
		THRD_START_PTTRNS.remove(start);
		removeExtendedFilterPattern(start);
	}

}