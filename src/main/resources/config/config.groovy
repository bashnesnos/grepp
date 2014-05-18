defaults {
    spoolFileExtension='log'
    resultsDir='results'
    reportSeparator {
        value=','
        spoolFileExtension='csv'
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
    group_op_values='oo'
    group_ops='oo'
    count_ops='oo'
    avg_timings='oo'
}
reportColumns {
   test_ops {
        filter {
            colName='some_cmd'
            value='Command name="?(.*?)"'
        }
        test {
            colName='count_of_operands'
            value='(operand)'
        }
    }
    count_ops {
        filter {
            colName='some_cmd'
            value='Command name="?(.*?)"'
        }
        counter {
            colName='count_of_operands'
            value='(operand)'
        }
    }
    group_ops {
        group {
            colName='some_cmd'
            value='Command name="?(.*?)"'
        }
        counter {
            colName='count_of_operands'
            value='(operand)'
        }
    }    
    group_op_values {
        group {
            colName='some_cmd'
            value='Command name="?(.*?)"'
        }
        rfilter {
            colName='operands'
            value='operand=\'(.*?)\''
        }
    }    
    avg_timings {
        group {
            colName='some_cmd'
            value='Command name="?(.*?)"'
        }
        avg {
            colName='avg_processing'
            value='time="?(\\d*)"'
        }
    }
    avg_operands {
        group {
            colName='some_cmd'
            value='Command name="?(.*?)"'
        }
        avg {
            colName='avg_operands'
            value='(operand)'
        }
    }
}
reportSeparators {
    csv {
        value=','
        spoolFileExtension='.csv'
    }
    piped {
        value='|'
        spoolFileExtension='.txt'
    }
}
doodki {
    test_doodki="aga"
}