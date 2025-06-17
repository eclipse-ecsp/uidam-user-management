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
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;

import java.util.List;
import java.util.Map;

/**
 * Represents the password policy response DTO. This class contains the details of the password policy to be enforced
 * while password creation.
 */
@Getter 
@Setter
public class PasswordPolicyResponse {
    private int minLength;
    private int maxLength;
    private int minConsecutiveLettersLength;
    private int minSpecialChars;
    private String allowedSpecialChars;
    private String excludedSpecialChars;
    private int minUppercase;
    private int minLowercase;
    private int minDigits;

    /**
     * Converts a list of PasswordPolicy entities to a PasswordPolicyResponse DTO.
     *
     * @param policies the list of PasswordPolicy entities
     * @return the PasswordPolicyResponse DTO
     */
    public static PasswordPolicyResponse fromEntities(List<PasswordPolicy> policies) {
        PasswordPolicyResponse response = new PasswordPolicyResponse();
        for (PasswordPolicy policy : policies) {
            switch (policy.getKey()) {
                case "complexity":
                    Map<String, Object> complexityRules = policy.getValidationRules();
                    response.setMinUppercase((Integer) complexityRules.getOrDefault("minUppercase", 0));
                    response.setMinLowercase((Integer) complexityRules.getOrDefault("minLowercase", 0));
                    response.setMinDigits((Integer) complexityRules.getOrDefault("minDigits", 0));
                    break;
                case "size":
                    Map<String, Object> sizeRules = policy.getValidationRules();
                    response.setMinLength((Integer) sizeRules.getOrDefault("minLength", 0));
                    response.setMaxLength((Integer) sizeRules.getOrDefault("maxLength", 0));
                    break;
                case "specialChars":
                    Map<String, Object> specialCharRules = policy.getValidationRules();
                    response.setMinSpecialChars((Integer) specialCharRules.getOrDefault("minSpecialChars", 0));
                    response.setAllowedSpecialChars((String) specialCharRules.getOrDefault("allowedSpecialChars", ""));
                    response.setExcludedSpecialChars(
                            (String) specialCharRules.getOrDefault("excludedSpecialChars", ""));
                    break;
                case "usernameSequenceExclusion":
                    response.setMinConsecutiveLettersLength(
                            (Integer) policy.getValidationRules().getOrDefault("noOfCharsSeqinUserField", 0));
                    break;
                default:
                    break;
            }
        }
        return response;
    }
}
