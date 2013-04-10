package org.smlt.tools.wgrep.exceptions

/**
 * Exception which is throwed when time to date is overdued due a file proccessing. <br>
 * Prevents unnecessary lines filtering.
 * 
 * @author Alexander Semelit
 *
 */
class TimeToIsOverduedException extends Exception {
    private timeToString
    TimeToIsOverduedException(String timeTo) {
        super()
        timeToString = timeTo
    }
    def getTimeTo() {
        return timeToString
    }
}