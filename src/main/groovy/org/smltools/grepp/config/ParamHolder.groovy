package org.smltools.grepp.config

import java.util.List;
import java.util.Map;
import groovy.util.logging.Slf4j;

@Slf4j
public class ParamHolder {
	
	private ParamHolderFactory createdByFactory
	private final Map<Param, ?> params = [:] //all params as a Map
	
	public ParamHolder(ParamHolderFactory factory) {
		createdByFactory = factory
	}
	
	/**
	 * Gets value of the {@link this.params} by key.
	 * @param field Key for <code>params</code> which is needed to be get.
	 * @return Value set to the key <code>field</code>
	 */

	public Object get(Param field)
	{
		if (Param.FILES.equals(field))
		{
			return Collections.unmodifiableList(params[field]);
		}
		return params[field]
	}
	
	public String getSpoolFileName() {
		return get(Param.FILTER_PATTERN).replaceAll("[^\\p{L}\\p{N}]", {""}) + get(Param.SPOOLING_EXT)
	}
	
	public Object getProcessingData() {
		List<Param> dataParams = Param.getDataParams()
		Param suppliedData = dataParams.find { param ->
			this.checkParamIsEmpty(param) != true
		}
		if (suppliedData != null) {
			return this.get(suppliedData)
		}
		else {
			return Param.Type.DATA.getDefaultValue()
		}
	}
	
	/**
	 * Sets value of any field which exists in <code>WgrepFacade</code> via reflection. Is used to propagate value directly from config.xml
	 * @param field Name of field of <code>WgrepFacade</code> which is needed to be set.
	 * @param val Value to be set
	 */

	void set(Param field, Object val)
	{
		params[field] = val
	}
	
	Map<Param, ?> getModifiableParams() {
		return params
	}
	
	Object withModifiableParams(Closure closure) {
		return closure.call(params)
	}
	
	public Map<Param, ?> getParams() {
		return Collections.unmodifiableMap(params)
	}
	
	public Object withParams(Closure closure) {
		return closure.call(Collections.unmodifiableMap(params))
	}
	
	public boolean refresh(Object criteria) {
		return createdByFactory.refreshParams(this, criteria)
	}
	
	/**
	 * Method checks if mandatory and optional parameters are filled.
	 * @return <code>true</code> if check is passed. <code>false</code> otherwise.
	 */

	public boolean check(List<Param> mandatory, List<Param> optional)
	{
		return check(params, mandatory, optional)
	}

	public static boolean check(Map<Param, ?> params, List<Param> mandatory, List<Param> optional) {
		boolean checkResult = true
		
		def emptyMandatory = mandatory.findAll{ paramName -> checkParamIsEmpty(params, paramName)}
			.each{ paramName ->
					log.error("Mandatory param {} is empty", paramName)
			}
		
		if (emptyMandatory.size() > 0) return false

		optional.findAll{ paramName -> checkParamIsEmpty(params, paramName)}
			.each{ paramName ->
				log.warn("Optional param {} is empty", paramName)
			}

		return checkResult

	}
	
	/**
	 * Method checks if param is empty.
	 * @return <code>true</code> if it is empty. <code>false</code> otherwise.
	 */

	public boolean checkParamIsEmpty(Param paramName) {
		return checkParamIsEmpty(params, paramName);
	}
	
	public static boolean checkParamIsEmpty(Map<Param, ?> params, Param paramName) {
		def param = params[paramName]
		if (param != null) {
			if (param instanceof Collection) {
				return param.size() == 0
			}
			if (param instanceof Map) {
				return param.size() == 0
			}
			return false
		}
		else {
			return true
		}

	}

}
