package org.smlt.tools.wgrep.exceptions;

/**
 * Exception which is throwed when time to date is overdued due a file proccessing. <br>
 * Prevents unnecessary lines filtering.
 * 
 * @author Alexander Semelit
 *
 */
public class TimeToIsOverduedException extends Exception {
	
	private String timeToString;
    public TimeToIsOverduedException(String timeTo) {
        super();
        timeToString = timeTo;
    }
    
	public String getTimeTo() {
        return timeToString;
    }
}