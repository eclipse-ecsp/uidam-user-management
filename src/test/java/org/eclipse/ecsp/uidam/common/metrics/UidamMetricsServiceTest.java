package org.eclipse.ecsp.uidam.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test case for validating UidamMetrics Service.
 */
public class UidamMetricsServiceTest {

    private UidamMetricsService uidamMetricsService;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        uidamMetricsService = new UidamMetricsService(meterRegistry);
    }

    @Test
    public void incrementCounter_shouldRegisterAndIncrementCounter() {
        MetricInfo metricInfo = MetricInfo.builder().uidamMetrics(UidamMetrics.TOTAL_BLOCKED_USERS_EVENT).build();
        uidamMetricsService.incrementCounter(metricInfo);
    }

    @Test
    public void incrementCounter_validate_count() {
        MetricInfo metricInfo = MetricInfo.builder().uidamMetrics(UidamMetrics.TOTAL_BLOCKED_USERS_EVENT).build();
        uidamMetricsService.incrementCounter(metricInfo);
        double count = meterRegistry.counter(UidamMetrics.TOTAL_BLOCKED_USERS_EVENT.getMetricName(),
                "application", "Uidam User Management").count();
        assertEquals(1.0, count);
    }

    @Test
    public void incrementCounter_with_metric_info_as_null() {
        assertThrows(NullPointerException.class, () -> uidamMetricsService.incrementCounter(null));
    }

    @Test
    public void incrementCounter_with_uidam_metrics_as_null() {
        MetricInfo metricInfo = MetricInfo.builder().uidamMetrics(null).build();
        assertThrows(NullPointerException.class, () -> uidamMetricsService.incrementCounter(metricInfo));
    }

    @Test
    public void incrementCounter_shouldThrowExceptionWhenMetricInfoIsNull() {
        MetricInfo metricInfo = mock(MetricInfo.class);
        when(metricInfo.getUidamMetrics()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> uidamMetricsService.incrementCounter(metricInfo));
    }
}
