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

package org.eclipse.ecsp.uidam.security.policy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicyRepository;
import org.eclipse.ecsp.uidam.security.policy.response.PasswordPolicyResponse;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyControllerTest {

    private static final int INT_10 = 10;
    private static final int INT_200 = 200;
    private static final int INT_16 = 16;
    
    @Mock
    private PasswordPolicyService passwordPolicyService;

    @InjectMocks
    private PasswordPolicyController passwordPolicyController;

    private static final String LOGGED_IN_USER = "test-user";
    private static final String POLICY_KEY = "passwordPolicy";
    
    @Mock
    private PasswordPolicyRepository passwordPolicyRepository;


    @Test
    void testGetAllPasswordPolicies() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setKey("size");
        policy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_10);
        validationRules.put("maxLength", INT_16);
        policy.setValidationRules(validationRules);
        when(passwordPolicyService.getAllPolicies()).thenReturn(List.of(policy));
        ResponseEntity<PasswordPolicyResponse> response = passwordPolicyController
                .getAllPasswordPolicies(LOGGED_IN_USER);
        assertNotNull(response);
        assertEquals(INT_200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        verify(passwordPolicyService, times(1)).getAllPolicies();
    }

    @Test
    void testGetPolicyByKey() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setKey("size");
        policy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_10);
        validationRules.put("maxLength", INT_16);
        policy.setValidationRules(validationRules);
        when(passwordPolicyService.getPolicyByKey(POLICY_KEY)).thenReturn(policy);
        ResponseEntity<PasswordPolicyResponse> response = 
                passwordPolicyController.getPolicyByKey(POLICY_KEY, LOGGED_IN_USER);
        assertNotNull(response);
        assertEquals(INT_200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        verify(passwordPolicyService, times(1)).getPolicyByKey(POLICY_KEY);
    }

    @Test
    void testUpdatePasswordPolicies() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setKey("size");
        policy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_10);
        validationRules.put("maxLength", INT_16);
        policy.setValidationRules(validationRules);
        JsonNode patchRequest = new ObjectMapper().createObjectNode();
        when(passwordPolicyService.updatePolicies(patchRequest, LOGGED_IN_USER)).thenReturn(List.of(policy));
        ResponseEntity<PasswordPolicyResponse> response = 
                passwordPolicyController.updatePasswordPolicies(patchRequest, LOGGED_IN_USER);        
        assertNotNull(response);
        assertEquals(INT_200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        verify(passwordPolicyService, times(1)).updatePolicies(patchRequest, LOGGED_IN_USER);
    }
}

