package org.smltools.grepp.exceptions;

/**
 * 
 * Such an exception will show that filtering was interrupted by some valid reason. <br>
 * I.e. that further filtering is not necessary and there will be no data matching given criteria.
 * 
 * @author Alexander Semelit
 *
 */
@SuppressWarnings("serial")
public class ConfigNotExistsRuntimeException extends RuntimeException {
    public ConfigNotExistsRuntimeException(String configId) {
        super(configId);
    }
}
