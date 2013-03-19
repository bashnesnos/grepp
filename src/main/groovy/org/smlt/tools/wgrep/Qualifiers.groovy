enum Qualifiers {
    and {
    	boolean check(def previousMatched, def matcher) { 
    		previousMatched && matcher 
    	} 
    },
    or { 
    	boolean check(def previousMatched, def matcher) { 
    		previousMatched || matcher 
    	} 
    };
   	
   	boolean check(def previousMatched, def matcher) { 
   		throw new UnsupportedOperationException("Must be overrided")
   	};
}
