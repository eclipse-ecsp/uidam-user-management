package org.eclipse.ecsp.uidam.common.metrics;

import lombok.Builder;
import lombok.Getter;

import java.util.stream.Stream;

/**
 * Class representing metric information including the metric type and associated tags.
 */
@Getter
@Builder
public class MetricInfo {
    private UidamMetrics uidamMetrics;
    private Stream<String> tags;
}
