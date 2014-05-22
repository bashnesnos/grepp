package org.smltools.grepp.filters;

import java.util.Map;
import org.smltools.grepp.exceptions.ConfigNotExistsRuntimeException;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Super class for all filters. Provides filtering process template with hooking
 * methods.<br>
 * 
 * @author Alexander Semelit
 * @param <T>
 */
public abstract class RefreshableFilterBase<T> extends FilterBase<T> implements Refreshable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshableFilterBase.class);           
    protected boolean isLocked = false;

    @Override
    public void lock() {
        isLocked = true;
    }

    @Override
    public boolean refreshByConfigId(String configId) {
        if (configId == null) {
            throw new IllegalArgumentException("configId shoudn't be null!");
        }

        if (isLocked) {
            LOGGER.debug("{} refresh is locked;", this.getClass().getName());
            return false;
        }

        if (this.config == null) {
            throw new IllegalStateException("Can't refresh by configId if the config itself wasn't supplied explicitly!");
        }

        if (this.configId != null && this.configId.equals(configId)) {
            return false; //same configId, no need refreshing
        }

        try {
            if (fillParamsByConfigId(configId)) {
                this.configId = configId;
                return true;
            }
            else {
                return false;
            }
        }
        catch(ConfigNotExistsRuntimeException cnere) {
            LOGGER.debug("Not refreshing due to: ", cnere);
        }
        catch(PropertiesNotFoundRuntimeException pnfre) {
            LOGGER.debug("Not refreshing due to: ", pnfre);
        }
        return false;
    }

}