package org.smlt.tools.wgrep.filters.entry;

import java.util.*;
import java.util.regex.*;
import org.smlt.tools.wgrep.filters.enums.*;
import org.smlt.tools.wgrep.filters.FilterBase;
import org.smlt.tools.wgrep.util.WgrepUtil;



/**
 * 
 * Class provides in-flight pattern building depending on thread start, thread end patterns. If non specified works in the same way as BasicFilter. <br>
 * Forces multiline regex matching. 
 * 
 * @author Alexander Semelit
 *
 */

public class ComplexFilter extends FilterBase<String> {

	//Complex pattern processing and stuff
	Pattern currentPattern = null;
	StringBuilder PATTERN = new StringBuilder("(?ms)"); //for multiline support
	List<String> EXTNDD_PTTRNS = new ArrayList<String>();
	Map<String, Qualifier> EXTNDD_PTTRN_DICT = new HashMap<String, Qualifier>();

	List<String> THRD_START_EXTRCTRS;
	List<String> THRD_START_PTTRNS;
	List<String> THRD_SKIP_END_PTTRNS;
	List<String> THRD_END_PTTRNS;

	/**
	 * Initializes configInstance, filterPattern and thread coupling config from config.xml
	 * 
	 * @param nextFilter_ next filter in chain
	 * @param config WgrepConfig instance is needed to get supplied params.
	 */
	@SuppressWarnings("unchecked")
	ComplexFilter(FilterBase<String> nextFilter_, Map<String, Object> preserveParams)
	{
		super(nextFilter_, ComplexFilter.class);
		THRD_START_EXTRCTRS = (List<String>) preserveParams.get("THRD_START_EXTRCTRS"); //if null indicates that thread preserving is disabled
		if (isThreadPreserveEnabled())
		{
			THRD_START_PTTRNS = (List<String>) WgrepUtil.getNotNull(preserveParams, "THRD_START_PTTRNS", new ArrayList<String>()); //could be empty, as it will be extracted in runtime
			THRD_SKIP_END_PTTRNS = (List<String>) WgrepUtil.getNotNull(preserveParams, "THRD_SKIP_END_PTTRNS", new ArrayList<String>()); //could be empty, which will mean that there are no ends to skip
			THRD_END_PTTRNS = (List<String>) WgrepUtil.getNotNull(preserveParams, "THRD_END_PTTRNS", new ArrayList<String>()); //if empty, than it is probably an wrong configuration, since first thread will consume all output
			if (THRD_END_PTTRNS.isEmpty()) log.warn("No thread end patterns were specified! Output could be unrepresentative");
			if (log.isTraceEnabled()) {
				log.trace(THRD_START_EXTRCTRS.toString());
				log.trace(THRD_START_PTTRNS.toString());
				log.trace(THRD_SKIP_END_PTTRNS.toString());
				log.trace(THRD_END_PTTRNS.toString());
			}
		}
		processExtendedPattern((String) preserveParams.get("FILTER_PATTERN"));
	}

	

	/**
	 * Checks is data matches current pattern 
	 * @throws IllegalArgumentException if blockData is not String
	 */

	@Override
	public boolean check(Object blockData) {
		if (blockData instanceof String)
		{
			String filterPtrn = PATTERN.toString();
			if(log.isTraceEnabled()) {
				log.trace("Current pattern: " + filterPtrn);
			}
			
			if (currentPattern == null || currentPattern.toString() != filterPtrn) 
			{
				currentPattern = Pattern.compile(filterPtrn);
			}

			Matcher blockMtchr = currentPattern.matcher((String) blockData);
			return blockMtchr.find();
		} 
		else throw new IllegalArgumentException("blockData should be String");
	}

	/**
	 *  Extracts thread start patterns if thread preserve is enabled
	 */
	
	@Override
	public void beforePassing(Object blockData) {
		if (isThreadPreserveEnabled())
		{
			extractThreadPatterns((String) blockData);
		}
	}

	/**
	 * Checks is thread preserving is enabled.
	 * 
	 * @return true if it is
	 */
	private boolean isThreadPreserveEnabled()
	{
		return THRD_START_EXTRCTRS != null;
	}

	/**
	 * Extracts thread patterns, and adds/keeps them if thread is not yet ended in the logs, or removes them is it has ended.
	 * 
	 * @param data String already matched by filter pattern.
	 */
	private void extractThreadPatterns(String data)
	{
		if (searchThreadEnds(data))
		{
			for (Map.Entry<String, String> extractedStart: extractThreadStarts(data).entrySet()) {
				removeThreadStart(extractedStart.getKey(), extractedStart.getValue());
			}
		}
		else
		{
			if (log.isTraceEnabled()) log.trace("Thread continues. Keeping starts");
			for (Map.Entry<String, String> extractedStart: extractThreadStarts(data).entrySet()) {
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
	private Map<String, String> extractThreadStarts(String data)
	{
		HashMap<String, String> extractedStarts = new HashMap<String, String>();
		for (String extractorPattern : THRD_START_EXTRCTRS) {
			if (log.isTraceEnabled())
				log.trace(extractorPattern);
			Matcher extractorMatcher = Pattern.compile(extractorPattern).matcher(data);
			if (extractorMatcher.find())
			{
				String start = extractorMatcher.group();
				if (log.isTraceEnabled())
					log.trace("extracted; " + start);
				extractedStarts.put(start, Qualifier.or.toString()); //adding extractor as or, since any could be a thread start
			}
		}
		return extractedStarts;
	}

	/**
	 * Applies <pattern> elements having THRD_END_PTTRNS collection as a parameter. <br>
	 * If any is matched current data is considered as end of current log thread if any.
	 * 
	 * @param data String already matched by filter pattern.
	 * @return true if thread end was found, false otherwise
	 */
	private boolean searchThreadEnds(String data)
	{
		if (!shouldBeSkipped(data))
		{
			boolean decision = false;
			Iterator<String> endIter = THRD_END_PTTRNS.iterator();
			while (!decision && endIter.hasNext()) {
				String thrend = endIter.next();
				log.trace("thrend ptrn: " + thrend);
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
	private boolean shouldBeSkipped(String data)
	{
		boolean decision = false;
		Iterator<String> skipEndIter = THRD_SKIP_END_PTTRNS.iterator();
		while (!decision && skipEndIter.hasNext()) {
			String thrend = skipEndIter.next();
			log.trace("thrend ptrn: " + thrend);
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
	private void addThreadStart(String start, String qlfr)
	{
		log.trace("adding thread start: " + start);
		if (!THRD_START_PTTRNS.contains(start))
		{
			THRD_START_PTTRNS.add(start);
			addExtendedFilterPattern(start, qlfr);
		}
		else log.trace("Start exists");
	}

	/**
	 * Removes supplied thread start. Qualifier will be identified automatically.
	 *
	 * @param start String representing thread start
	 * @param qlfr not used. Needed for reflection call
	 */
	private void removeThreadStart(String start, String qlfr)
	{
		log.trace("removing thread start: " + start);
		THRD_START_PTTRNS.remove(start);
		removeExtendedFilterPattern(start);
	}

	/**
	 * Appends to current pattern new part which is could be a thread coupling pattern or just a different thing to look up in the data.
	 * 
	 * @param val pattern to be added
	 * @param qualifier identifies how to conjunct it with previous patterns
	 */

	private void addExtendedFilterPattern(String val, String qualifier)
	{
		if (log.isTraceEnabled()) log.trace("adding complex pattern: val=" + val + " qual=" + qualifier);

		if (qualifier != null) PATTERN = PATTERN.append(Qualifier.valueOf(qualifier).getPattern());
		PATTERN = PATTERN.append(val);

		EXTNDD_PTTRNS.add(val);
		EXTNDD_PTTRN_DICT.put(val, qualifier != null ? Qualifier.valueOf(qualifier) : null);

		if (log.isTraceEnabled()) {
			log.trace(EXTNDD_PTTRNS.toString());
			log.trace(EXTNDD_PTTRN_DICT.toString());
		}
	}

	/**
	 * Removes supplied pattern with it's qualifier if any.
	 * 
	 * @param val pattern for removal
	 */
	private void removeExtendedFilterPattern(String val)
	{
		Qualifier qlfr = EXTNDD_PTTRN_DICT.get(val);
		String ptrn = (qlfr != null ? qlfr.getPattern() : "") + val;
		int ptrnIndex = PATTERN.indexOf(ptrn);
		if (log.isTraceEnabled()) log.trace("to delete:/" + ptrn +"/ index:" + ptrnIndex);
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
	private void processExtendedPattern(String val)
	{
		String qRegex = "";
		for (Qualifier it: Qualifier.values()) {
			qRegex += qRegex.length() > 0 ? "|%" + it + "%" : "%" + it + "%";
		}

		if (log.isTraceEnabled()) log.trace("Trying to match supplied pattern /" + val + "/ if it contains /" + qRegex + "/");
		Matcher qualifierMatcher = Pattern.compile(qRegex).matcher(val); //matching any qualifiers with % signs
		if (qualifierMatcher.find())
		{
			if (log.isTraceEnabled()) log.trace("Processing complex pattern");
			String[] tokens = val.split("%");
			String nextQualifier = null;
			if (tokens != null)
			{
				qRegex = qRegex.replaceAll("%", ""); //matching only qualifier names
				for (String grp : tokens)
				{
					if (log.isTraceEnabled()) log.trace("Next group in match: " + grp);
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
			if (log.isTraceEnabled()) log.trace("No extended pattern supplied, might be a preserve thread");
			addExtendedFilterPattern(val, null);
		}
	}
}