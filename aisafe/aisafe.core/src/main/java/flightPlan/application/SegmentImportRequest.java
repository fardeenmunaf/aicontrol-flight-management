package flightPlan.application;

public class SegmentImportRequest {

    public final double fromLatitude;
    public final double fromLongitude;
    public final double toLatitude;
    public final double toLongitude;

    public SegmentImportRequest(final double fromLatitude, final double fromLongitude,
                                final double toLatitude, final double toLongitude) {
        this.fromLatitude = fromLatitude;
        this.fromLongitude = fromLongitude;
        this.toLatitude = toLatitude;
        this.toLongitude = toLongitude;
    }
}
