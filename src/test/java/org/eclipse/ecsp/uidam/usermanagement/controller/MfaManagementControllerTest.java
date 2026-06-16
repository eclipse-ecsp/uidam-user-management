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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.service.MfaManagementService;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodeVerifyRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodeVerifyResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaBackupCodesResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaEnrollInitiateResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaRecoveryKeyRequest;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.MfaStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for MfaManagementController.
 */
@WebMvcTest(MfaManagementController.class)
@MockitoBean(types = JpaMetamodelMappingContext.class)
@DisplayName("MfaManagementController Test Suite")
class MfaManagementControllerTest {

    private static final String BASE_PATH = "/v1/users/{username}/mfa";
    private static final String ADMIN_BASE_PATH = "/v1/admin/users/{userId}/mfa";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_SECRET = "JBSWY3DPEHPK3PXP";
    private static final String TEST_QR_URI = "otpauth://totp/TestApp:testuser?secret=JBSWY3DPEHPK3PXP&issuer=TestApp";
    private static final BigInteger TEST_USER_ID = BigInteger.valueOf(12345);
    private static final String TEST_ADMIN_ID = "admin123";
    private static final int FIVE = 5;
    private static final int FOUR = 4;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MfaManagementService mfaManagementService;

    @MockitoBean
    private TenantConfigurationService tenantConfigurationService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    org.eclipse.ecsp.uidam.usermanagement.utilities.UserAuditHelper userAuditHelper;

    // ══════════════════════════════════════════════════════════
    //  Enrollment Tests
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should initiate MFA enrollment successfully")
    void testInitiateEnrollment() throws Exception {
        // Arrange
        MfaEnrollInitiateResponse response = new MfaEnrollInitiateResponse(
                TEST_SECRET, TEST_QR_URI, "JBSW Y3DP EHPK 3PXP");
        when(mfaManagementService.initiateEnrollment(TEST_USERNAME)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/enroll/initiate", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value(TEST_SECRET))
                .andExpect(jsonPath("$.qrUri").value(TEST_QR_URI));

        verify(mfaManagementService).initiateEnrollment(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return 404 when user not found during enrollment initiation")
    void testInitiateEnrollmentUserNotFound() throws Exception {
        // Arrange
        when(mfaManagementService.initiateEnrollment(TEST_USERNAME))
                .thenThrow(new ResourceNotFoundException("User", "username", TEST_USERNAME));

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/enroll/initiate", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should activate MFA enrollment successfully")
    void testActivateEnrollment() throws Exception {
        // Arrange
        doNothing().when(mfaManagementService).activateEnrollment(TEST_USERNAME);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/enroll/activate", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(mfaManagementService).activateEnrollment(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return 404 when no pending enrollment exists during activation")
    void testActivateEnrollmentNoPending() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("MfaEnrollment", "status", "PENDING"))
                .when(mfaManagementService).activateEnrollment(TEST_USERNAME);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/enroll/activate", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════════════════════
    //  Status and Secret Tests
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should get MFA status successfully")
    void testGetMfaStatus() throws Exception {
        // Arrange
        MfaStatusResponse statusResponse = new MfaStatusResponse(true, "ACTIVE", true);
        when(mfaManagementService.getStatus(TEST_USERNAME)).thenReturn(statusResponse);

        // Act & Assert
        mockMvc.perform(get(BASE_PATH + "/status", TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.backupCodesEnabled").value(true));

        verify(mfaManagementService).getStatus(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should get MFA secret successfully")
    void testGetMfaSecretSuccess() throws Exception {
        // Arrange
        when(mfaManagementService.getSecret(TEST_USERNAME)).thenReturn(Optional.of(TEST_SECRET));

        // Act & Assert
        mockMvc.perform(get(BASE_PATH + "/secret", TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(content().string("\"" + TEST_SECRET + "\""));

        verify(mfaManagementService).getSecret(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return 404 when MFA secret not found")
    void testGetMfaSecretNotFound() throws Exception {
        // Arrange
        when(mfaManagementService.getSecret(TEST_USERNAME)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get(BASE_PATH + "/secret", TEST_USERNAME))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should revoke MFA enrollment successfully")
    void testRevokeEnrollment() throws Exception {
        // Arrange
        doNothing().when(mfaManagementService).revokeEnrollment(TEST_USERNAME);

        // Act & Assert
        mockMvc.perform(delete(BASE_PATH + "/revoke", TEST_USERNAME))
                .andExpect(status().isNoContent());

        verify(mfaManagementService).revokeEnrollment(TEST_USERNAME);
    }

    // ══════════════════════════════════════════════════════════
    //  Recovery Tests
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should send recovery key successfully")
    void testSendRecoveryKey() throws Exception {
        // Arrange
        doNothing().when(mfaManagementService).sendRecoveryKey(TEST_USERNAME);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/recovery/send-key", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(mfaManagementService).sendRecoveryKey(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return 404 when user not found during recovery key send")
    void testSendRecoveryKeyUserNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User", "username", TEST_USERNAME))
                .when(mfaManagementService).sendRecoveryKey(TEST_USERNAME);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/recovery/send-key", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should verify recovery key successfully")
    void testVerifyRecoveryKeySuccess() throws Exception {
        // Arrange
        MfaRecoveryKeyRequest request = new MfaRecoveryKeyRequest("ABC123");
        when(mfaManagementService.verifyRecoveryKeyAndRevoke(TEST_USERNAME, "ABC123"))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/recovery/verify-key", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(mfaManagementService).verifyRecoveryKeyAndRevoke(TEST_USERNAME, "ABC123");
    }

    @Test
    @DisplayName("Should return false when recovery key verification fails")
    void testVerifyRecoveryKeyFailure() throws Exception {
        // Arrange
        MfaRecoveryKeyRequest request = new MfaRecoveryKeyRequest("WRONG");
        when(mfaManagementService.verifyRecoveryKeyAndRevoke(TEST_USERNAME, "WRONG"))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/recovery/verify-key", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ══════════════════════════════════════════════════════════
    //  Backup Codes Tests
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should generate backup codes successfully")
    void testGenerateBackupCodes() throws Exception {
        // Arrange
        MfaBackupCodesResponse response = new MfaBackupCodesResponse(
                Arrays.asList("CODE1", "CODE2", "CODE3", "CODE4", "CODE5"), FIVE);
        when(mfaManagementService.generateBackupCodes(TEST_USERNAME)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/backup-codes/generate", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codes").isArray())
                .andExpect(jsonPath("$.codes.length()").value(FIVE));

        verify(mfaManagementService).generateBackupCodes(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return 404 when user not found during backup code generation")
    void testGenerateBackupCodesUserNotFound() throws Exception {
        // Arrange
        when(mfaManagementService.generateBackupCodes(TEST_USERNAME))
                .thenThrow(new ResourceNotFoundException("User", "username", TEST_USERNAME));

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/backup-codes/generate", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should verify backup code successfully")
    void testVerifyBackupCodeSuccess() throws Exception {
        // Arrange
        MfaBackupCodeVerifyRequest request = new MfaBackupCodeVerifyRequest("BACKUP123");
        MfaBackupCodeVerifyResponse response = new MfaBackupCodeVerifyResponse(true, FOUR, false);
        when(mfaManagementService.verifyBackupCode(TEST_USERNAME, "BACKUP123"))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/backup-codes/verify", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.remainingBackupCodes").value(FOUR));

        verify(mfaManagementService).verifyBackupCode(TEST_USERNAME, "BACKUP123");
    }

    @Test
    @DisplayName("Should return invalid when backup code verification fails")
    void testVerifyBackupCodeInvalid() throws Exception {
        // Arrange
        MfaBackupCodeVerifyRequest request = new MfaBackupCodeVerifyRequest("INVALID");
        MfaBackupCodeVerifyResponse response = new MfaBackupCodeVerifyResponse(false, FIVE, false);
        when(mfaManagementService.verifyBackupCode(TEST_USERNAME, "INVALID"))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_PATH + "/backup-codes/verify", TEST_USERNAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.remainingBackupCodes").value(FIVE));
    }

    // ══════════════════════════════════════════════════════════
    //  Admin Endpoints Tests
    // ══════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should get MFA status for user by admin")
    void testAdminGetMfaStatus() throws Exception {
        // Arrange
        MfaStatusResponse statusResponse = new MfaStatusResponse(true, "ACTIVE", false);
        when(mfaManagementService.getStatusByUserId(TEST_USER_ID)).thenReturn(statusResponse);

        // Act & Assert
        mockMvc.perform(get(ADMIN_BASE_PATH + "/status", TEST_USER_ID)
                .header(LOGGED_IN_USER_ID, TEST_ADMIN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.backupCodesEnabled").value(false));

        verify(mfaManagementService).getStatusByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should revoke MFA enrollment by admin")
    void testAdminRevokeEnrollment() throws Exception {
        // Arrange
        doNothing().when(mfaManagementService).revokeEnrollmentByUserId(TEST_USER_ID);

        // Act & Assert
        mockMvc.perform(delete(ADMIN_BASE_PATH + "/revoke", TEST_USER_ID)
                .header(LOGGED_IN_USER_ID, TEST_ADMIN_ID))
                .andExpect(status().isNoContent());

        verify(mfaManagementService).revokeEnrollmentByUserId(TEST_USER_ID);
    }
}
