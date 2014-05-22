defaults {
    spoolFileExtension='txt'
    resultsDir='results'
    report {
        aggregator = 'csv'
        printHeader = true
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
}
