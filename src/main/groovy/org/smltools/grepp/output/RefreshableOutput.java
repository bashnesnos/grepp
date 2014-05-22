/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.smltools.grepp.output;

/**
 *
 * @author asemelit
 * @param <T>
 */
public interface RefreshableOutput<T> {
    /**
     * Refreshes filters/filtering params by some criteria.
     * 
     * @param criteria Something that can be used for config refreshing. Filename for example
     */
    void refreshFilters(T criteria);

}
