package org.eclipse.ecsp.uidam.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.ecsp.uidam.usermanagement.config.TenantContext;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service for handling UIDAM metrics.
 */
@Component
@RequiredArgsConstructor
public class UidamMetricsService {

    private final MeterRegistry meterRegistry;

    public static final String DEFAULT_API_VERSION = "v1";

    /**
     * Increment the counter for the given metric information.
     *
     * @param metricInfo the metric information
     */
    public void incrementCounter(@NonNull MetricInfo metricInfo) {

        Objects.requireNonNull(metricInfo.getUidamMetrics(), "Uidam metric must not be null");
        String[] tags = Optional.ofNullable(metricInfo.getTags()).orElse(Stream.empty()).toArray(String[]::new);
        Counter.builder(metricInfo.getUidamMetrics().getMetricName())
                .description(metricInfo.getUidamMetrics().getDescription())
                .tag("application", "uidam-user-management")
                .tag("tenantId", TenantContext.getCurrentTenant())
                .tag("apiVersion", DEFAULT_API_VERSION)
                .tags(tags)
                .register(meterRegistry).increment();
    }
}
