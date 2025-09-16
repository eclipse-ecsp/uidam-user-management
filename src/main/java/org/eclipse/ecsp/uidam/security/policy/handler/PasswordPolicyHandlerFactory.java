/*
 * Copyright (c) 2023 - 2024 Harman International
 * Licensed under the Apache Lice        COMPLEXITY("complexity") {
            @Override
            PasswordPolicyHandler createHandler(Map<String, Object> rules, PasswordHistoryRepository repository,
                    TenantConfigurationService tenantConfigurationService) {
                return new PasswordComplexityPolicyHandler(rules);
            }
        },rsion 2.0 (the "License");
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

package org.eclipse.ecsp.uidam.security.policy.handler;

import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.eclipse.ecsp.uidam.usermanagement.repository.PasswordHistoryRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Factory class for creating password policy handlers. This class is responsible for creating instances of
 * PasswordPolicyHandler based on the provided PasswordPolicy.
 */
@Service
public class PasswordPolicyHandlerFactory {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final TenantConfigurationService tenantConfigurationService;

    /**
     * Constructor for PasswordPolicyHandlerFactory.
     *
     * @param passwordHistoryRepository Repository for password history.
     * @param tenantConfigurationService Tenant configuration service.
     */
    public PasswordPolicyHandlerFactory(PasswordHistoryRepository passwordHistoryRepository,
            TenantConfigurationService tenantConfigurationService) {
        this.passwordHistoryRepository = Objects.requireNonNull(passwordHistoryRepository,
                "passwordHistoryRepository must not be null");
        this.tenantConfigurationService = Objects.requireNonNull(tenantConfigurationService,
                "tenantConfigurationService must not be null");
    }
    
    /**
     * Creates a PasswordPolicyHandler based on the provided PasswordPolicy.
     *
     * @param policy The PasswordPolicy to create a handler for.
     * @return A PasswordPolicyHandler instance.
     * @throws IllegalArgumentException if the policy is null or if the policy key is unknown.
     */
    public PasswordPolicyHandler createHandler(PasswordPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        Map<String, Object> rules = policy.getValidationRules();

        return Stream.of(PolicyType.values()).filter(type -> type.key.equals(policy.getKey())).findFirst()
                .map(type -> type.createHandler(rules, passwordHistoryRepository, tenantConfigurationService))
                .orElseThrow(() -> new IllegalArgumentException("Unknown policy key: " + policy.getKey()));
    }

    /**
     * Enum representing the different types of password policies. Each enum constant is associated with a key and
     * provides a method to create the corresponding PasswordPolicyHandler.
     */
    public enum PolicyType {
        SIZE("size") {
            @Override
            PasswordPolicyHandler createHandler(Map<String, Object> rules, PasswordHistoryRepository repository,
                    TenantConfigurationService tenantConfigurationService) {
                return new SizePolicyHandler(rules);
            }
        },
        SPECIAL_CHARS("specialChars") {
            @Override
            PasswordPolicyHandler createHandler(Map<String, Object> rules, PasswordHistoryRepository repository,
                    TenantConfigurationService tenantConfigurationService) {
                return new SpecialCharacterPolicyHandler(rules);
            }
        },
        COMPLEXITY("complexity") {
            @Override
            PasswordPolicyHandler createHandler(Map<String, Object> rules, PasswordHistoryRepository repository,
                    TenantConfigurationService tenantConfigurationService) {
                return new ComplexityPolicyHandler(rules);
            }
        },
        EXPIRATION("expiration") {
            @Override
            PasswordPolicyHandler createHandler(Map<String, Object> rules, PasswordHistoryRepository repository,
                    TenantConfigurationService tenantConfigurationService) {
                return new ExpirationPolicyHandler(rules, repository,
                        tenantConfigurationService.getTenantProperties().getPasswordEncoder());
            }
        },
        USERNAME_SEQUENCE_EXCLUSION("usernameSequenceExclusion") {
            @Override
            PasswordPolicyHandler createHandler(Map<String, Object> rules, PasswordHistoryRepository repository,
                    TenantConfigurationService tenantConfigurationService) {
                return new UsernameExclusionPolicyHandler(rules);
            }
        },
        PASSWORD_LAST_UPDATE_VALIDATION("passwordLastUpdateValidation") {
            @Override
            PasswordPolicyHandler createHandler(Map<String, Object> rules, PasswordHistoryRepository repository,
                    TenantConfigurationService tenantConfigurationService) {
                return new LastUpdateValidationPolicyHandler(rules);
            }
        },
        COMPROMISED_PASSWORD("CompromisedPassword") {
            @Override
            PasswordPolicyHandler createHandler(Map<String, Object> rules, PasswordHistoryRepository repository,
                    TenantConfigurationService tenantConfigurationService) {
                return new CompromisedPasswordPolicyHandler(rules);
            }
        };

        private final String key;

        PolicyType(String key) {
            this.key = key;
        }

        abstract PasswordPolicyHandler createHandler(Map<String, Object> rules, PasswordHistoryRepository repository,
                TenantConfigurationService tenantConfigurationService);
    }
}
