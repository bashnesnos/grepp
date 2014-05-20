package  org.smltools.grepp.filters;

public abstract class ReportAggregatorBase implements ReportAggregator {
    @Override
    public String getId() {
        if (this.getClass().isAnnotationPresent(ReportAggregatorParams.class)) {
            ReportAggregatorParams aggParams = this.getClass().getAnnotation(ReportAggregatorParams.class);
            return aggParams.id();
        }
        else {
            throw new IllegalArgumentException(this.getClass() + " should have ReportAggregatorParams annotation or override getSpoolFileExtension method!");
        }        
    }

    @Override
    public String getSpoolFileExtension() {
        if (this.getClass().isAnnotationPresent(ReportAggregatorParams.class)) {
            ReportAggregatorParams aggParams = this.getClass().getAnnotation(ReportAggregatorParams.class);
            return aggParams.spoolFileExtension();
        }
        else {
            throw new IllegalArgumentException(this.getClass() + " should have ReportAggregatorParams annotation or override getSpoolFileExtension method!");
        }
    }

}
