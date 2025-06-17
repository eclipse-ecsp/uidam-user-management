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

import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PasswordPolicyResponseTest {

    private static final int INT_16 = 16;
    private static final int INT_8 = 8;
    private static final int INT_4 = 4;
    private static final int INT_12 = 12;
    private static final int INT_6 = 6;
    private static final int INT_2 = 2;
    private static final int INT_1 = 1;
    private static final int INT_3 = 3;

    @Test
    void testFromEntities_withComplexityPolicy() {
        PasswordPolicy complexityPolicy = new PasswordPolicy();
        complexityPolicy.setKey("complexity");
        Map<String, Object> complexityRules = new HashMap<>();
        complexityRules.put("minUppercase", INT_2);
        complexityRules.put("minLowercase", INT_3);
        complexityRules.put("minDigits", INT_4);
        complexityPolicy.setValidationRules(complexityRules);
        List<PasswordPolicy> policies = new ArrayList<>();
        policies.add(complexityPolicy);

        PasswordPolicyResponse response = PasswordPolicyResponse.fromEntities(policies);

        assertEquals(INT_2, response.getMinUppercase());
        assertEquals(INT_3, response.getMinLowercase());
        assertEquals(INT_4, response.getMinDigits());
    }

    @Test
    void testFromEntities_withSizePolicy() {
        PasswordPolicy sizePolicy = new PasswordPolicy();
        sizePolicy.setKey("size");
        Map<String, Object> sizeRules = new HashMap<>();
        sizeRules.put("minLength", INT_8);
        sizeRules.put("maxLength", INT_16);
        sizePolicy.setValidationRules(sizeRules);
        List<PasswordPolicy> policies = new ArrayList<>();
        policies.add(sizePolicy);

        PasswordPolicyResponse response = PasswordPolicyResponse.fromEntities(policies);

        assertEquals(INT_8, response.getMinLength());
        assertEquals(INT_16, response.getMaxLength());
    }

    @Test
    void testFromEntities_withSpecialCharsPolicy() {
        PasswordPolicy specialCharsPolicy = new PasswordPolicy();
        specialCharsPolicy.setKey("specialChars");
        Map<String, Object> specialCharRules = new HashMap<>();
        specialCharRules.put("minSpecialChars", INT_2);
        specialCharRules.put("allowedSpecialChars", "!@#$%");
        specialCharRules.put("excludedSpecialChars", "^&*");
        specialCharsPolicy.setValidationRules(specialCharRules);
        List<PasswordPolicy> policies = new ArrayList<>();
        policies.add(specialCharsPolicy);

        PasswordPolicyResponse response = PasswordPolicyResponse.fromEntities(policies);

        assertEquals(INT_2, response.getMinSpecialChars());
        assertEquals("!@#$%", response.getAllowedSpecialChars());
        assertEquals("^&*", response.getExcludedSpecialChars());
    }

    @Test
    void testFromEntities_withUsernameSequenceExclusionPolicy() {
        PasswordPolicy usernameSequencePolicy = new PasswordPolicy();
        usernameSequencePolicy.setKey("usernameSequenceExclusion");
        Map<String, Object> usernameSequenceRules = new HashMap<>();
        usernameSequenceRules.put("noOfCharsSeqinUserField", INT_3);
        usernameSequencePolicy.setValidationRules(usernameSequenceRules);
        List<PasswordPolicy> policies = new ArrayList<>();
        policies.add(usernameSequencePolicy);

        PasswordPolicyResponse response = PasswordPolicyResponse.fromEntities(policies);

        assertEquals(INT_3, response.getMinConsecutiveLettersLength());
    }

    @Test
    void testFromEntities_withMultiplePolicies() {
        PasswordPolicy complexityPolicy = new PasswordPolicy();
        complexityPolicy.setKey("complexity");
        Map<String, Object> complexityRules = new HashMap<>();
        complexityRules.put("minUppercase", INT_1);
        complexityRules.put("minLowercase", INT_2);
        complexityRules.put("minDigits", INT_3);
        complexityPolicy.setValidationRules(complexityRules);
        List<PasswordPolicy> policies = new ArrayList<>();
        policies.add(complexityPolicy);

        PasswordPolicy sizePolicy = new PasswordPolicy();
        sizePolicy.setKey("size");
        Map<String, Object> sizeRules = new HashMap<>();
        sizeRules.put("minLength", INT_6);
        sizeRules.put("maxLength", INT_12);
        sizePolicy.setValidationRules(sizeRules);
        policies.add(sizePolicy);

        PasswordPolicyResponse response = PasswordPolicyResponse.fromEntities(policies);

        assertEquals(INT_1, response.getMinUppercase());
        assertEquals(INT_2, response.getMinLowercase());
        assertEquals(INT_3, response.getMinDigits());
        assertEquals(INT_6, response.getMinLength());
        assertEquals(INT_12, response.getMaxLength());
    }
}