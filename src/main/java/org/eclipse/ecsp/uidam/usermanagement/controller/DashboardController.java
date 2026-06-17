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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.eclipse.ecsp.uidam.usermanagement.dashboard.response.dto.DashboardStatsResponse;
import org.eclipse.ecsp.uidam.usermanagement.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.VERSION_V1;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Dashboard API controller providing aggregated platform statistics.
 */
@RestController
@RequestMapping(value = VERSION_V1 + "/dashboard", produces = APPLICATION_JSON_VALUE)
@AllArgsConstructor
public class DashboardController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardService dashboardService;

    /**
     * Retrieves aggregated dashboard statistics.
     *
     * @return dashboard statistics including user counts, account counts,
     *         role/scope counts, status distribution, and recent activity
     */
    @GetMapping("/stats")
    @SecurityRequirement(name = "JwtAuthValidator", scopes = {"ManageAccounts", "TenantAdmin"})
    @Operation(
            summary = "Get dashboard statistics",
            description = "Retrieves aggregated statistics for the admin dashboard including user counts, "
                    + "account information, roles, scopes, and recent activity.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully",
                        content = @Content(schema = @Schema(implementation = DashboardStatsResponse.class))),
                @ApiResponse(responseCode = "401", description = "Unauthorized"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        LOGGER.info("Request received to fetch dashboard statistics");
        DashboardStatsResponse stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
