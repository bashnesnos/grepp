package org.smltools.grepp.exceptions;

/**
 * Exception which is throwed when time to date is overdued due a file proccessing. <br>
 * Prevents unnecessary lines filtering.
 * 
 * @author Alexander Semelit
 *
 */
@SuppressWarnings("serial")
public class TimeToIsOverduedException extends FilteringIsInterruptedException {
	
	private String timeToString;
    public TimeToIsOverduedException(String timeTo) {
        super(timeTo);
        timeToString = timeTo;
    }
    
	public String getTimeTo() {
        return timeToString;
    }
}