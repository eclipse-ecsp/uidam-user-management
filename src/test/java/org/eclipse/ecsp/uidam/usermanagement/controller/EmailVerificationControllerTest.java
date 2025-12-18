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

import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailVerificationService;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.EmailVerificationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_IS_EMAIL_VERIFIED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.PATH_RESEND_EMAIL_VERIFY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({EmailVerificationController.class})
@MockBean(JpaMetamodelMappingContext.class)
class EmailVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailVerificationController emailVerificationController;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean
    private TenantConfigurationService tenantConfigurationService;
    
    @MockBean
    org.eclipse.ecsp.uidam.usermanagement.utilities.UserAuditHelper userAuditHelper;

    @Autowired
    private ApplicationContext applicationContext;
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationControllerTest.class);
    
    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }
    
    @Test
    @Order(1)
    void contextLoad() {
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
            .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping
            .getHandlerMethods();
        map.forEach((key, value) -> LOGGER.info("{} {}", key, value));
        Assert.notEmpty(map, "Handler mappings map is empty. Controller APIs are not exposed.");
        assertNotNull(emailVerificationController, "EmailVerificationController not created");
        assertNotNull(emailVerificationService, "emailVerificationService not created");
    }

    private EmailVerificationResponse getEmailVerificationResponse() {
        return EmailVerificationResponse.builder()
            .isVerified(true)
            .build();
    }

    @Test
    void testGetIsEmailVerified() throws Exception {
        when(emailVerificationService.getEmailVerificationByUserId(anyString()))
            .thenReturn(List.of(getEmailVerificationResponse()));
        UUID userId = UUID.randomUUID();
        String tenantId = "ecsp";
        mockMvc.perform(get("/" + tenantId + "/v1/emailVerification/" + userId + "/" + PATH_IS_EMAIL_VERIFIED)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .header(CORRELATION_ID, "12345"))
            .andExpect(status().isOk())
            .andExpectAll(
                MockMvcResultMatchers.jsonPath("$[0].isVerified").value(true));
    }

    @Test
    void testGetIsEmailVerifiedCorrelationIdMissing() throws Exception {
        when(emailVerificationService.getEmailVerificationByUserId(anyString()))
            .thenReturn(List.of(getEmailVerificationResponse()));
        UUID userId = UUID.randomUUID();
        String tenantId = "ecsp";
        mockMvc.perform(get("/" + tenantId + "/v1/emailVerification/" + userId + "/" + PATH_IS_EMAIL_VERIFIED)
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());
    }
    
    @Test
    void testVerifyEmail() throws Exception {
        UUID token = UUID.randomUUID();
        String tenantId = "ecsp";
        mockMvc.perform(get("/" + tenantId + "/v1/emailVerification/" + token)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .header(CORRELATION_ID, "12345"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testVerifyEmailCorrelationIdMissing() throws Exception {
        UUID token = UUID.randomUUID();
        String tenantId = "ecsp";
        mockMvc.perform(get("/" + tenantId + "/v1/emailVerification/" + token)
                            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());
    }

    @Test
    void testResendEmailVerification() throws Exception {
        doNothing().when(emailVerificationService).resendEmailVerification(anyString());
        UUID userId = UUID.randomUUID();
        String tenantId = "ecsp";
        mockMvc.perform(put("/" + tenantId + "/v1/emailVerification/" + userId + "/" + PATH_RESEND_EMAIL_VERIFY)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header(CORRELATION_ID, "12345"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testResendEmailVerificationCorrelationIdMissing() throws Exception {
        doNothing().when(emailVerificationService).resendEmailVerification(anyString());
        UUID userId = UUID.randomUUID();
        String tenantId = "ecsp";
        mockMvc.perform(put("/" + tenantId + "/v1/emailVerification/" + userId + "/" + PATH_RESEND_EMAIL_VERIFY)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());
    }

}
