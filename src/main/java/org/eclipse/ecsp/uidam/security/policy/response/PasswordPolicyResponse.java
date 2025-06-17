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

package org.eclipse.ecsp.uidam.security.policy.response;

import lombok.Data;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * DTO representing the response structure for password policy retrieval.
 */
@Data
public class PasswordPolicyResponse {
    private List<PolicyDetails> passwordPolicies;

    /**
     * Factory method to create PasswordPolicyResponse from a list of entities.
     */
    public static PasswordPolicyResponse fromEntities(List<PasswordPolicy> policies) {
        PasswordPolicyResponse response = new PasswordPolicyResponse();
        response.setPasswordPolicies(Optional.ofNullable(policies).filter(p -> !p.isEmpty())
                .map(p -> p.stream().map(PasswordPolicyMapper.INSTANCE::toPolicyDetails).toList())
                .orElseGet(ArrayList::new) // Return an empty list if policies is null or empty
        );

        return response;
    }

    /**
     * DTO representing details of a password policy.
     */
    @Data
    public static class PolicyDetails {
        private String name;
        private String key;
        private String description;
        private Map<String, Object> validationRules;
        private int priority;
        private boolean required;
    }

    /**
     * Mapper interface for mapping PasswordPolicy entities to PolicyDetails DTOs.
     */
    @Mapper
    public interface PasswordPolicyMapper {
        
        PasswordPolicyMapper INSTANCE = Mappers.getMapper(PasswordPolicyMapper.class);
        
        @Mapping(source = "validationRules", target = "validationRules")
        PolicyDetails toPolicyDetails(PasswordPolicy policy);
    }
}