package org.smlt.tools.wgrep.exceptions

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