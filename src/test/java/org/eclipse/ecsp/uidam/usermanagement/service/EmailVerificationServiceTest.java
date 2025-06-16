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

import io.prometheus.client.CollectorRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.entity.EmailVerificationEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAddressEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.EmailVerificationRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.EmailVerificationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "3600000")
class EmailVerificationServiceTest {

    @MockBean
    private UsersRepository usersRepository;

    @MockBean
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailVerificationService emailVerificationService;
    @MockBean
    private EmailNotificationService emailNotificationService;
    @MockBean
    UidamAuthTokenGenerator uidamAuthTokenGenerator;
    @MockBean
    AuthorizationServerClient authorizationServerClient;

    @MockBean
    AccountRepository accountRepository;
    
    @MockBean
    PasswordValidationService passwordValidationService;

    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    private static final Long MINUS_DAYS = 8L;
    private static final BigInteger USER_ID = new BigInteger("157236105403847391232405464474353");

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void testGetEmailVerificationByUserIdUserNotFound() {
        String userId = USER_ID.toString();
        assertThrows(
            ResourceNotFoundException.class, 
            () -> emailVerificationService.getEmailVerificationByUserId(userId), 
            "User not found for userId: " + userId
        );
    }

    @Test
    void testGetEmailVerificationByUserIdEmailDataNotFound() throws ResourceNotFoundException {
        String userId = "157236105403847391232405464474353";
        when(usersRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
            .thenReturn(new UserEntity());
        when(emailVerificationRepository.findByUserId(any(BigInteger.class)))
                .thenReturn(Optional.empty());
        List<EmailVerificationResponse> emailVerificationResponses = emailVerificationService
            .getEmailVerificationByUserId(userId);
        assertEquals(false, emailVerificationResponses.get(0).getIsVerified());
    }

    @Test
    void testGetEmailVerificationByUserIdEmailVerified() throws ResourceNotFoundException {
        EmailVerificationEntity emailVerificationEntity = new EmailVerificationEntity();
        emailVerificationEntity.setIsVerified(true);
        UserEntity userEntity = new UserEntity();

        userEntity.setId(USER_ID);
        when(usersRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(userEntity);
        when(emailVerificationRepository.findByUserId(any(BigInteger.class)))
                .thenReturn(Optional.of(emailVerificationEntity));
        String userId = USER_ID.toString();
        List<EmailVerificationResponse> emailVerificationResponses = emailVerificationService
            .getEmailVerificationByUserId(userId);
        assertEquals(true, emailVerificationResponses.get(0).getIsVerified());
    }

    @Test
    void testVerifyEmailDataNotFound() throws IOException {
        String token = String.valueOf(UUID.randomUUID());
        when(emailVerificationRepository.findByToken(anyString())).thenReturn(Optional.empty());
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        emailVerificationService.verifyEmail(token, httpServletResponse);
        verify(httpServletResponse, times(1)).sendRedirect(anyString());
    }

    @Test
    void testVerifyEmailTokenExpired() throws IOException {
        EmailVerificationEntity emailVerificationEntity = new EmailVerificationEntity();
        emailVerificationEntity.setUpdateDate(Timestamp.valueOf(LocalDateTime.now().minusDays(MINUS_DAYS)));
        String token = String.valueOf(UUID.randomUUID());
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        when(emailVerificationRepository.findByToken(anyString()))
                .thenReturn(Optional.of(emailVerificationEntity));
        emailVerificationService.verifyEmail(token, httpServletResponse);
        verify(httpServletResponse, times(1)).sendRedirect(anyString());
    }

    @Test
    void testVerifyEmailTokenFormatIncorrect() {
        String token = "token";
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        assertThrows(DataIntegrityViolationException.class, 
                     () -> emailVerificationService.verifyEmail(token, httpServletResponse), 
                     "Token format is incorrect expecting UUID format: token");
    }

    @Test
    void testVerifyEmailUpdateSuccess() throws IOException {
        EmailVerificationEntity emailVerificationEntity = new EmailVerificationEntity();
        emailVerificationEntity.setUpdateDate(Timestamp.valueOf(LocalDateTime.now()));
        when(emailVerificationRepository.findByToken(anyString()))
                .thenReturn(Optional.of(emailVerificationEntity));
        emailVerificationEntity.setIsVerified(true);
        emailVerificationEntity.setUserId(USER_ID);
        when(emailVerificationRepository.save(any(EmailVerificationEntity.class)))
                .thenReturn(emailVerificationEntity);
        String token = String.valueOf(UUID.randomUUID());
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        emailVerificationService.verifyEmail(token, httpServletResponse);
        verify(httpServletResponse, times(1)).sendRedirect(anyString());
    }

    @Test
    void testResendEmailVerificationSuccess() {
        UserEntity userEntity = createUserEntity("test@gmail.com");
        when(usersRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(userEntity);
        when(emailVerificationRepository.findByUserId(any(BigInteger.class)))
            .thenReturn(Optional.of(new EmailVerificationEntity()));
        doNothing().when(emailNotificationService).sendNotification(anyMap(), anyString(), any(Map.class));
        assertDoesNotThrow(() -> emailVerificationService.resendEmailVerification(USER_ID.toString()));
        verify(emailNotificationService, times(1))
                .sendNotification(anyMap(), anyString(), any(Map.class));
    }

    @Test
    void testResendEmailVerificationSuccessUpdate() {
        UserEntity userEntity = createUserEntity("test@gmail.com");
        when(usersRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class)))
                .thenReturn(userEntity);
        when(emailVerificationRepository.findByUserId(any(BigInteger.class)))
                .thenReturn(Optional.empty());
        doNothing().when(emailNotificationService).sendNotification(anyMap(), anyString(), any(Map.class));
        assertDoesNotThrow(() -> emailVerificationService.resendEmailVerification(USER_ID.toString()));
        verify(emailNotificationService, times(1))
                .sendNotification(anyMap(), anyString(), any(Map.class));
    }

    @Test
    void testResendEmailVerificationUserNotFound() {
        when(usersRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class))).thenReturn(null);
        String userId = USER_ID.toString();
        assertThrows(ResourceNotFoundException.class, 
            () -> emailVerificationService.resendEmailVerification(userId));
        verify(emailNotificationService, times(0))
                .sendNotification(anyMap(), anyString(), any(Map.class));
    }

    @Test
    void testResendEmailVerificationEmailNotSent() {
        UserEntity userEntity = createUserEntity("test@guestuser.com");
        when(usersRepository.findByIdAndStatusNot(any(BigInteger.class), any(UserStatus.class))).thenReturn(userEntity);
        assertDoesNotThrow(() -> emailVerificationService.resendEmailVerification(USER_ID.toString()));
        verify(emailNotificationService, times(0)).sendNotification(anyMap(), anyString(), any(Map.class));
    }

    UserEntity createUserEntity(String email) {
        UserEntity userEntity = new UserEntity();
        BigInteger userIdUuid = USER_ID;
        userEntity.setId(userIdUuid);
        userEntity.setEmail(email);
        userEntity.setFirstName("first");
        userEntity.setLastName("last");
        UserAddressEntity userAddressEntity = new UserAddressEntity();
        userAddressEntity.setState("Delhi");
        userEntity.setUserAddresses(List.of(userAddressEntity));
        return userEntity;
    }

}
