/*
 * Copyright (c) 2023 Harman International
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.ecsp.uidam.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.ecsp.sql.multitenancy.TenantContext;
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
                .tag(UidamMetricsConstants.TAG_NAME_APPLICATION, UidamMetricsConstants.TAG_VALUE_APPLICATION)
                .tag(UidamMetricsConstants.TAG_NAME_TENANT_ID, TenantContext.getCurrentTenant())
                .tag(UidamMetricsConstants.TAG_NAME_API_VERSION, UidamMetricsConstants.DEFAULT_API_VERSION)
                .tags(tags)
                .register(meterRegistry).increment();
    }
}
