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

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static lombok.AccessLevel.PRIVATE;

/**
 * UserDetailsResponse Pojo.
 */
@Getter
@Setter
@FieldDefaults(level = PRIVATE)
public class UserDetailsResponse {
    String id;
    String userName;
    String password;
    String passwordEncoder;
    String salt;
    String accountId;
    String status;
    String email;
    Map<String, Object> captcha = new HashMap<>();
    String lastSuccessfulLoginTime;
    Integer failureLoginAttempts;
    Set<String> scopes = new HashSet<>();
    Map<String, Object> additionalAttributes = new HashMap<>();
}
