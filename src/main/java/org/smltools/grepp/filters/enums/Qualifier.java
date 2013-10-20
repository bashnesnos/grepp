package org.smltools.grepp.filters.enums;

/**
 * Enum representing complex pattern qualifiers.
 * 
 * @author Alexander Semelit
 *
 */
public enum Qualifier {
    and(".*"), or("|");
   	
    private final String ptrn;
    
    Qualifier(String ptrn) {
       this.ptrn = ptrn;
    }
    
    public String getPattern() { 
      return ptrn;
    }
}
