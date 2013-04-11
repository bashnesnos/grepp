package org.smlt.tools.wgrep.filters.enums

/**
 * Enum representing complex pattern qualifiers.
 * 
 * @author Alexander Semelit
 *
 */
enum Qualifier {
    and('.*'), or('|')
   	
    private final String ptrn
    Qualifier(String ptrn) {
       this.ptrn = ptrn
    }
    
    String getPattern() { 
      return ptrn
    };
}
