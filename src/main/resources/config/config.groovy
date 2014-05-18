defaults {
    spoolFileExtension='txt'
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
reportSeparators {
    csv {
        value=','
        spoolFileExtension='csv'
    }
    piped {
        value='|'
        spoolFileExtension='txt'
    }
}
savedConfigs {
    properties {
        starter='log4j.logger'
        pattern='.*\\.properties'
    }
}
