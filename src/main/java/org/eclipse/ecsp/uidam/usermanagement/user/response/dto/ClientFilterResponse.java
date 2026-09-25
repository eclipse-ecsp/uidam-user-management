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

package org.eclipse.ecsp.uidam.usermanagement.user.response.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import java.util.List;

/**
 * Paginated response of the client filter api.
 */
@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ClientFilterResponse {

    @Schema(description = "Clients matching the filter criteria, client secret is never returned")
    private List<RegisteredClientDetails> items;

    @Schema(description = "Zero based index of the returned page")
    private int page;

    @Schema(description = "Number of items per page")
    private int pageSize;

    @Schema(description = "Total number of clients matching the filter criteria")
    private long totalItems;

    @Schema(description = "Total number of pages available for the filter criteria")
    private int totalPages;
}
