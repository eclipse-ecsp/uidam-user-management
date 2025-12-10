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
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService.ValidationResult;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.authorization.dto.BaseResponseFromAuthorization;
import org.eclipse.ecsp.uidam.usermanagement.cache.CacheTokenService;
import org.eclipse.ecsp.uidam.usermanagement.config.tenantproperties.UserManagementTenantProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserRecoverySecret;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserRecoverySecretStatus;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.repository.CloudProfilesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.PasswordHistoryRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserRecoverySecretRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.AuthorizationServerClient;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailNotificationService;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.eclipse.ecsp.uidam.usermanagement.service.UidamAuthTokenGenerator;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserUpdatePasswordDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PASSWORD_ENCODER;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.RECOVERY_SECRET_EXPIRE_IN_MINUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit Test class for password recovery flow.
 *
 *
 */
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "3600000")
class UserPasswordRecoveryTest {
    private static final BigInteger USER_ID_1 = new BigInteger("145911385530649019822702644100150");
    private static final BigInteger USER_ID_2 = new BigInteger("145911385590649019822702644100150");
    private static final BigInteger USER_ID_3 = new BigInteger("145911385690649019822702644100150");
    private static final BigInteger ROLE_ID = new BigInteger("145911385590649014822702644100150");
    @MockBean
    UsersRepository usersRepository;

    @MockBean
    UserRecoverySecretRepository userRecoverySecretRepository;

    @MockBean
    EmailNotificationService emailNotificationService;

    @MockBean
    private PasswordHistoryRepository passwordHistoryRepository;
    
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    CacheTokenService tokenCache;

    @MockBean
    UidamAuthTokenGenerator uidamAuthTokenGenerator;
    @MockBean
    AuthorizationServerClient authorizationServerClient;
    @MockBean
    CloudProfilesRepository cloudProfilesRepository;
    @MockBean
    TenantConfigurationService tenantConfigurationService;
    @MockBean
    UserManagementTenantProperties tenantProperties;
    @MockBean
    AccountRepository accountRepository;

    @MockBean
    PasswordValidationService passwordValidationService;
    
    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    @MockBean
    org.eclipse.ecsp.uidam.usermanagement.utilities.UserAuditHelper userAuditHelper;
    
    @BeforeEach 
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void updatePasswordRecoverySecretSuccess() {
        String secret = "6f452624-c4e3-40ff-ba29-fe9082705f50";
        UserUpdatePasswordDto userEventsDto = new UserUpdatePasswordDto();
        userEventsDto.setPassword("April@20q22");
        userEventsDto.setSecret(secret);
        UserRecoverySecret userRecoverySecret = new UserRecoverySecret();
        userRecoverySecret.setId(USER_ID_1);
        userRecoverySecret.setRecoverySecret(secret);
        userRecoverySecret.setRecoverySecretStatus("GENERATED");
        userRecoverySecret.setSecretGeneratedAt(Instant.now());
        when(userRecoverySecretRepository.findUserRecoverySecretDetailsByRecoverySecret(secret))
                .thenReturn(userRecoverySecret);
        when(usersRepository.findByIdAndStatusNot(userRecoverySecret.getUserId(), UserStatus.DELETED))
                .thenReturn(getUserEntity());
        when(usersRepository.save(any(UserEntity.class))).thenReturn(getUserEntity());
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getRecoverySecretExpiresInMinutes()).thenReturn(RECOVERY_SECRET_EXPIRE_IN_MINUTES);
        when(tenantProperties.getPasswordEncoder()).thenReturn(PASSWORD_ENCODER);
        when(tokenCache.getAccessToken()).thenReturn("testtoken");
        BaseResponseFromAuthorization authResponse = new BaseResponseFromAuthorization();
        authResponse.setHttpStatus(HttpStatus.OK);
        when(authorizationServerClient.revokeTokenByAdmin(anyString(), anyString())).thenReturn(authResponse);
        when(passwordValidationService.validatePassword(anyString(), anyString(), any(Timestamp.class)))
                .thenReturn(new ValidationResult(true, null));
        webTestClient.post().uri("/v1/users/recovery/set-password").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(userEventsDto).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void updatePasswordRecoverySecretResourceNotFound() {
        String secret = "6f452624-c4e3-40ff-ba29-fe9082705f50";
        UserUpdatePasswordDto userEventsDto = new UserUpdatePasswordDto();
        userEventsDto.setPassword("April@20q22");
        userEventsDto.setSecret(secret);
        UserRecoverySecret userRecoverySecret = new UserRecoverySecret();
        userRecoverySecret.setId(USER_ID_2);
        userRecoverySecret.setRecoverySecret(secret);
        userRecoverySecret.setRecoverySecretStatus("GENERATED");
        userRecoverySecret.setSecretGeneratedAt(Instant.now());
        when(userRecoverySecretRepository.findUserRecoverySecretDetailsByRecoverySecret(secret))
                .thenReturn(userRecoverySecret);
        when(usersRepository.findByIdAndStatusNot(userRecoverySecret.getUserId(), UserStatus.DELETED)).thenReturn(null);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getRecoverySecretExpiresInMinutes()).thenReturn(RECOVERY_SECRET_EXPIRE_IN_MINUTES);
        when(tenantProperties.getPasswordEncoder()).thenReturn(PASSWORD_ENCODER);
        webTestClient.post().uri("/v1/users/recovery/set-password").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(userEventsDto).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updatePasswordRecoverySecretExpires() {
        String secret = "6f452624-c4e3-40ff-ba29-fe9082705f50";
        UserUpdatePasswordDto userEventsDto = new UserUpdatePasswordDto();
        userEventsDto.setPassword("April@20q22");
        userEventsDto.setSecret(secret);
        UserRecoverySecret userRecoverySecret = new UserRecoverySecret();
        userRecoverySecret.setId(USER_ID_3);
        userRecoverySecret.setRecoverySecret(secret);
        userRecoverySecret.setRecoverySecretStatus(UserRecoverySecretStatus.EXPIRED.name());
        userRecoverySecret.setSecretGeneratedAt(Instant.now());
        when(userRecoverySecretRepository.findUserRecoverySecretDetailsByRecoverySecret(secret))
                .thenReturn(userRecoverySecret);
        when(usersRepository.findByIdAndStatusNot(userRecoverySecret.getUserId(), UserStatus.DELETED)).thenReturn(null);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getRecoverySecretExpiresInMinutes()).thenReturn(RECOVERY_SECRET_EXPIRE_IN_MINUTES);
        when(tenantProperties.getPasswordEncoder()).thenReturn(PASSWORD_ENCODER);
        webTestClient.post().uri("/v1/users/recovery/set-password").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(userEventsDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }
    
    @Test
    void updatePasswordRecoverySecretInvalidated() {
        String secret = "6f452624-c4e3-40ff-ba29-fe9082705f50";
        UserUpdatePasswordDto userEventsDto = new UserUpdatePasswordDto();
        userEventsDto.setPassword("April@20q22");
        userEventsDto.setSecret(secret);
        UserRecoverySecret userRecoverySecret = new UserRecoverySecret();
        userRecoverySecret.setId(USER_ID_3);
        userRecoverySecret.setRecoverySecret(secret);
        userRecoverySecret.setRecoverySecretStatus(UserRecoverySecretStatus.INVALIDATED.name());
        userRecoverySecret.setSecretGeneratedAt(Instant.now());
        when(userRecoverySecretRepository.findUserRecoverySecretDetailsByRecoverySecret(secret))
                .thenReturn(userRecoverySecret);
        when(usersRepository.findByIdAndStatusNot(userRecoverySecret.getUserId(), UserStatus.DELETED)).thenReturn(null);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getRecoverySecretExpiresInMinutes()).thenReturn(RECOVERY_SECRET_EXPIRE_IN_MINUTES);
        when(tenantProperties.getPasswordEncoder()).thenReturn(PASSWORD_ENCODER);
        webTestClient.post().uri("/v1/users/recovery/set-password").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(userEventsDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updatePasswordRecoverySecretExpiresException() {
        String secret = "6f452624-c4e3-40ff-ba29-fe9082705f50";
        UserUpdatePasswordDto userEventsDto = new UserUpdatePasswordDto();
        userEventsDto.setPassword("April@20q22");
        userEventsDto.setSecret(secret);
        UserRecoverySecret userRecoverySecret = new UserRecoverySecret();
        userRecoverySecret.setId(USER_ID_1);
        userRecoverySecret.setRecoverySecret(secret);
        userRecoverySecret.setRecoverySecretStatus(UserRecoverySecretStatus.GENERATED.name());
        userRecoverySecret.setSecretGeneratedAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        when(userRecoverySecretRepository.findUserRecoverySecretDetailsByRecoverySecret(secret))
                .thenReturn(userRecoverySecret);
        when(usersRepository.findByIdAndStatusNot(userRecoverySecret.getUserId(), UserStatus.DELETED))
                .thenReturn(getUserEntity());
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getRecoverySecretExpiresInMinutes()).thenReturn(RECOVERY_SECRET_EXPIRE_IN_MINUTES);
        when(tenantProperties.getPasswordEncoder()).thenReturn(PASSWORD_ENCODER);
        webTestClient.post().uri("/v1/users/recovery/set-password").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).bodyValue(userEventsDto).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void testSendPasswordRecoveryEmailNotification() {
        String userName = "username";
        when(usersRepository.findByUserNameIgnoreCaseAndStatusNot(userName, UserStatus.DELETED))
                .thenReturn(getUserEntity());
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getRecoverySecretExpiresInMinutes()).thenReturn(RECOVERY_SECRET_EXPIRE_IN_MINUTES);
        when(tenantProperties.getPasswordEncoder()).thenReturn(PASSWORD_ENCODER);
        when(tenantProperties.getAuthServerResetResponseUrl()).thenReturn("https://localhost:9443/recovery/reset");
        webTestClient.post().uri("/v1/users/{userName}/recovery/forgotpassword", userName).headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }
    
    
    @Test
    void testSendSelfPasswordRecoveryEmailNotification() {
        when(usersRepository.findByIdAndStatusNot(USER_ID_1, UserStatus.DELETED)).thenReturn(getUserEntity());
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getRecoverySecretExpiresInMinutes()).thenReturn(RECOVERY_SECRET_EXPIRE_IN_MINUTES);
        when(tenantProperties.getPasswordEncoder()).thenReturn(PASSWORD_ENCODER);
        when(tenantProperties.getAuthServerResetResponseUrl()).thenReturn("https://localhost:9443/recovery/reset");
        webTestClient.post().uri("/v1/users/self/recovery/resetpassword").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add("user-id", USER_ID_1.toString());
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).exchange().expectStatus().isEqualTo(HttpStatus.OK);
    }

    @Test
    void testSendPasswordRecoveryEmailNotificationResourceNotFound() {
        String userName = "username";
        when(usersRepository.findByUserNameIgnoreCaseAndStatusNot(userName, UserStatus.DELETED)).thenReturn(null);
        when(tenantConfigurationService.getTenantProperties()).thenReturn(tenantProperties);
        when(tenantProperties.getRecoverySecretExpiresInMinutes()).thenReturn(RECOVERY_SECRET_EXPIRE_IN_MINUTES);
        when(tenantProperties.getPasswordEncoder()).thenReturn(PASSWORD_ENCODER);
        webTestClient.post().uri("/v1/users/{userName}/recovery/forgotpassword", userName).headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
        }).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

    private UserEntity getUserEntity() {
        UserEntity userEntity = new UserEntity();
        userEntity.setUserName("johnd");
        userEntity.setPwdChangedtime(Timestamp.valueOf(LocalDateTime.now().minus(1, ChronoUnit.MINUTES)));
        return userEntity;
    }
}
