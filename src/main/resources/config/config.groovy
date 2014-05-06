defaults {
    spoolFileExtension='.log'
    resultsDir='results'
    postProcessSeparator {
        value=','
        spoolFileExtension='.csv'
    }
}
logDateFormats {
    iso {
        value='yyyy-MM-dd HH:mm:ss'
        regex='(\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2})'
    }
}
savedConfigs {
    properties {
        starter='log4j.logger'
        pattern='.*\\.properties'
    }
    to_test {
        starter='####\\[\\D{1,}\\].*'
        pattern='fpTest_'
        dateFormat {
            value='yyyy-MM-dd HH:mm:ss'
            regex='(\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2})'
        }
        logThreshold=4
    }
    pr_test {
        pattern='processing_'
        dateFormat {
            value='yyyy-MM-dd HH:mm:ss'
            regex='(\\d{4}-\\d{1,2}-\\d{1,2} \\d{2}:\\d{2}:\\d{2})'
        }
        logThreshold=24
    }
}
processThreads {
    to_test {
        extractors=['ThreadStart: \'\\d{1,}\'']
        skipends=['SkipPattern']
        ends=['ThreadEnd1', 'ThreadEnd2']
    }
    pr_test {
        extractors=['ThreadStart: \'\\d{1,}\'']
        skipends=['SkipPattern']
        ends=['ThreadEnd1', 'ThreadEnd2']
    }
}
filterAliases {
    predef='Something::'
    foo='Foo'
    some_timings='oo'
    avg_timings='oo'
}
postProcessColumns {
    some_timings {
        filter {
            order=1
            colName='some_cmd'
            value='Command name="?(.*?)"'
        }
        counter {
            order=2
            colName='count_of_operands'
            value='(operand)'
        }
    }
    avg_timings {
        group {
            order=1
            colName='some_cmd'
            value='Command name="?(.*?)"'
        }
        avg {
            order=2
            colName='avg_processing'
            value='time="?(\\d*)"'
        }
    }
    avg_operands {
        group {
            order=1
            colName='some_cmd'
            value='Command name="?(.*?)"'
        }
        avg {
            order=2
            colName='avg_operands'
            value='(operand)'
        }
    }
}
postProcessSeparators {
    csv {
        value=','
        spoolFileExtension='.csv'
    }
    piped {
        value='|'
        spoolFileExtension='.txt'
    }
}

