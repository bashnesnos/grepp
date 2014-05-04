postProcessSeparators {
    csv {
        spoolFileExtension='.csv'
        value=','
    }
    piped {
        spoolFileExtension='.txt'
        value='|'
    }
}
logDateFormats {
    iso {
        value = "yyyy-MM-dd HH:mm:ss"
        regex = /(\d{4}-\d{1,2}-\d{1,2} \d{2}:\d{2}:\d{2})/
    }
}
savedConfigs {
    properties {
        starter = "log4j.logger"
        pattern = /.*\.properties/
    }
    to_test {
        dateFormat = logDateFormats.iso
        starter = /####\[\D{1,}\].*/
        logThreshold = 4
        pattern = /fpTest_/
    }
    pr_test {
        dateFormat = logDateFormats.iso
        logThreshold = 24
        pattern = /processing_/
    }
}
processThreads {
    to_test {
        extractors = [/ThreadStart: '\d{1,}'/]
        skipends = [/SkipPattern/]
        ends = [/ThreadEnd1/,/ThreadEnd2/]
    }
    pr_test=to_test
}
postProcessColumns {
    some_timings {
        filter {
            order=1
            colName="some_cmd"
            value=/Command name = "?(.*?)"/
        }
        counter {
            order=2
            colName="count_of_operands"
            value=/(operand)/
        }
    }
    avg_timings {
        group {
            order=1
            colName="some_cmd"
            value=/Command name = "?(.*?)"/
        }
        avg {
            order=2
            colName="avg_processing"
            value=/time="?(\d*)"/
        }
    }
    avg_operands {
        group {
            order = 1
            colName = "some_cmd"
            value=/Command name = "?(.*?)"/
        }
        avg {
            order=2
            colName="avg_operands"
            value=/(operand)/
        }
    }
}
filterAliases {
    predef=/Something::/
    foo=/Foo/
    some_timings=avg_timings=/oo/
}
defaults {
    spoolFileExtension = '.log'
    resultsDir = 'results'
    postProcessSeparator=postProcessSeparators.csv
}
