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

package org.eclipse.ecsp.uidam.usermanagement.service;

import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.ClientFilterDto;
import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ClientFilterResponse;
import java.util.Optional;

/**
 * Interface containing methods for client registration flow.
 */
public interface ClientRegistration {

    RegisteredClientDetails addRegisteredClient(RegisteredClientDetails request);

    Optional<RegisteredClientDetails> getRegisteredClient(String clientId, String status);

    Optional<String> deleteRegisteredClient(String clientId);

    Optional<RegisteredClientDetails> updateRegisteredClient(String clientId, RegisteredClientDetails request);

    /**
     * Retrieve the clients matching the given filter criteria, page by page.
     *
     * @param clientFilterDto criteria each client attribute must match.
     * @param pageNumber zero based index of the page to retrieve.
     * @param pageSize number of clients per page.
     * @param sortBy client entity attribute used to sort the result.
     * @param sortOrder asc or desc sorting order.
     * @param ignoreCase perform a case-insensitive match on string attributes.
     * @param searchType match the value as PREFIX, SUFFIX, CONTAINS or EQUAL.
     * @return paginated clients matching the criteria, without their client secret.
     */
    ClientFilterResponse filterClients(ClientFilterDto clientFilterDto, Integer pageNumber, Integer pageSize,
            String sortBy, String sortOrder, boolean ignoreCase, SearchType searchType);

}
