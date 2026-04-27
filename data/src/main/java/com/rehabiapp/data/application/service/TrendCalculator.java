package com.rehabiapp.data.application.service;

import java.util.List;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public final class TrendCalculator {

    public record Trend(double slopeDegreesPerBucket, double intercept, double r2) {}

    private TrendCalculator() {}

    public static Trend compute(List<Double> romAvgByBucketOrder) {
        SimpleRegression r = new SimpleRegression(true);
        for (int i = 0; i < romAvgByBucketOrder.size(); i++) {
            r.addData(i, romAvgByBucketOrder.get(i));
        }
        return new Trend(r.getSlope(), r.getIntercept(), r.getRSquare());
    }
}
