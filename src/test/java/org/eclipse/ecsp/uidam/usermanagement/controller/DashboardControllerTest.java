/*
 * Copyright (c) 2023 - 2024 Harman International
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

package org.eclipse.ecsp.uidam.usermanagement.controller;

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.uidam.usermanagement.dashboard.response.dto.DashboardStatsResponse;
import org.eclipse.ecsp.uidam.usermanagement.dashboard.response.dto.DashboardStatsResponse.RecentActivityItem;
import org.eclipse.ecsp.uidam.usermanagement.service.DashboardService;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.utilities.UserAuditHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for DashboardController — verifies REST endpoint responses
 * and JSON serialization of dashboard statistics.
 */
@WebMvcTest(DashboardController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
class DashboardControllerTest {

    private static final int TOTAL_USERS = 100;
    private static final int ACTIVE_USERS = 85;
    private static final int PENDING_USERS = 10;
    private static final int BLOCKED_USERS = 5;
    private static final int TOTAL_ACCOUNTS = 8;
    private static final int ACTIVE_ACCOUNTS = 7;
    private static final int PENDING_ACCOUNTS = 1;
    private static final int TOTAL_ROLES = 15;
    private static final int TOTAL_SCOPES = 45;
    private static final int EXTERNAL_USERS = 20;
    private static final int FEDERATED_USERS = 10;
    private static final int USER_ACCOUNT_MAPPINGS = 150;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private TenantConfigurationService tenantConfigurationService;

    @MockitoBean
    private UserAuditHelper userAuditHelper;

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void getDashboardStats_withData_returnsOkWithAllFields() throws Exception {
        // Given — service returns a fully populated response
        DashboardStatsResponse response = buildMockResponse();
        when(dashboardService.getDashboardStats()).thenReturn(response);

        // When & Then — GET returns 200 with expected JSON structure
        mockMvc.perform(get("/v1/dashboard/stats")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", is(TOTAL_USERS)))
                .andExpect(jsonPath("$.activeUsers", is(ACTIVE_USERS)))
                .andExpect(jsonPath("$.pendingUsers", is(PENDING_USERS)))
                .andExpect(jsonPath("$.blockedUsers", is(BLOCKED_USERS)))
                .andExpect(jsonPath("$.totalAccounts", is(TOTAL_ACCOUNTS)))
                .andExpect(jsonPath("$.activeAccounts", is(ACTIVE_ACCOUNTS)))
                .andExpect(jsonPath("$.pendingAccounts", is(PENDING_ACCOUNTS)))
                .andExpect(jsonPath("$.totalRoles", is(TOTAL_ROLES)))
                .andExpect(jsonPath("$.totalScopes", is(TOTAL_SCOPES)))
                .andExpect(jsonPath("$.externalUsers", is(EXTERNAL_USERS)))
                .andExpect(jsonPath("$.federatedUsers", is(FEDERATED_USERS)))
                .andExpect(jsonPath("$.userAccountMappings", is(USER_ACCOUNT_MAPPINGS)))
                .andExpect(jsonPath("$.recentActivity[0].id", is("1")))
                .andExpect(jsonPath("$.recentActivity[0].type", is("LOGIN")))
                .andExpect(jsonPath("$.recentActivity[0].description", is("User logged in")));
    }

    @Test
    void getDashboardStats_withEmptyData_returnsOkWithZeroes() throws Exception {
        // Given — service returns an empty/zero response
        DashboardStatsResponse response = DashboardStatsResponse.builder()
                .totalUsers(0)
                .activeUsers(0)
                .pendingUsers(0)
                .blockedUsers(0)
                .totalAccounts(0)
                .activeAccounts(0)
                .pendingAccounts(0)
                .totalRoles(0)
                .totalScopes(0)
                .externalUsers(0)
                .federatedUsers(0)
                .userAccountMappings(0)
                .recentActivity(List.of())
                .build();
        when(dashboardService.getDashboardStats()).thenReturn(response);

        // When & Then — GET returns 200 with zero values
        mockMvc.perform(get("/v1/dashboard/stats")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", is(0)))
                .andExpect(jsonPath("$.recentActivity").isEmpty());
    }

    private DashboardStatsResponse buildMockResponse() {
        return DashboardStatsResponse.builder()
                .totalUsers(TOTAL_USERS)
                .activeUsers(ACTIVE_USERS)
                .pendingUsers(PENDING_USERS)
                .blockedUsers(BLOCKED_USERS)
                .totalAccounts(TOTAL_ACCOUNTS)
                .activeAccounts(ACTIVE_ACCOUNTS)
                .pendingAccounts(PENDING_ACCOUNTS)
                .totalRoles(TOTAL_ROLES)
                .totalScopes(TOTAL_SCOPES)
                .externalUsers(EXTERNAL_USERS)
                .federatedUsers(FEDERATED_USERS)
                .userAccountMappings(USER_ACCOUNT_MAPPINGS)
                .recentActivity(List.of(
                        RecentActivityItem.builder()
                                .id("1")
                                .type("LOGIN")
                                .description("User logged in")
                                .user("12345")
                                .timestamp("2026-05-18T10:30:00Z")
                                .build()))
                .build();
    }
}
