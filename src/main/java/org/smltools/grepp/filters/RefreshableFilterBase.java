package org.smltools.grepp.filters;

import java.util.Map;
import org.smltools.grepp.exceptions.ConfigNotExistsRuntimeException;
import org.smltools.grepp.exceptions.PropertiesNotFoundRuntimeException;

/**
 * Super class for all filters. Provides filtering process template with hooking
 * methods.<br>
 * 
 * @author Alexander Semelit
 * @param <T>
 */
public abstract class RefreshableFilterBase<T> extends FilterBase<T> implements Refreshable {
    protected boolean isLocked = false;

    public RefreshableFilterBase(Class<?> subclazz) {
            super(subclazz);
    }

    
    public RefreshableFilterBase(Class<?> subclazz, Map<?, ?> config) {
            super(subclazz, config);
    }
    
    @Override
    public void lock() {
        isLocked = true;
    }

    @Override
    public boolean refreshByConfigId(String configId) {
        if (configId == null) {
            throw new IllegalArgumentException("configId shoudn't be null!");
        }

        if (this.config == null || isLocked) {
            LOGGER.debug("{} refresh is locked; config is null? {}", this.getClass().getName(), this.config == null);
            return false;
        }

        if (this.configId != null && this.configId.equals(configId)) {
            return false; //same configId, no need refreshing
        }

        try {
            if (fillParamsByConfigIdInternal(configId)) {
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