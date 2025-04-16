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

import org.eclipse.ecsp.uidam.usermanagement.auth.request.dto.RegisteredClientDetails;
import java.util.Optional;

/**
 * Interface containing methods for client registration flow.
 */
public interface ClientRegistration {

    Optional<RegisteredClientDetails> addRegisteredClient(RegisteredClientDetails request);

    Optional<RegisteredClientDetails> getRegisteredClient(String clientId, String status);

    Optional<String> deleteRegisteredClient(String clientId);

    Optional<RegisteredClientDetails> updateRegisteredClient(String clientId, RegisteredClientDetails request);

}
