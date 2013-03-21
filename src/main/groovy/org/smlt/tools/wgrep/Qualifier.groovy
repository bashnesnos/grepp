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
