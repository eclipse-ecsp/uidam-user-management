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

package org.eclipse.ecsp.uidam.usermanagement.service.impl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.config.ApplicationProperties;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.entity.EmailVerificationEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.mapper.UserMapper;
import org.eclipse.ecsp.uidam.usermanagement.repository.EmailVerificationRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailNotificationService;
import org.eclipse.ecsp.uidam.usermanagement.service.EmailVerificationService;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.EmailVerificationResponse;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.eclipse.ecsp.uidam.usermanagement.utilities.ObjectConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.EMAIL_VERIFY_ERROR;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.EMAIL_VERIFY_FAILED;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.EMAIL_VERIFY_SUCCESS;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ID_VARIABLE;

/**
 *  User Email Verification Service CRUD APIs to send, get and update email verification for unregistered user in UIDAM.
 */
@Service
@AllArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private EmailVerificationRepository emailVerificationRepository;
    private UsersRepository usersRepository;
    private ApplicationProperties applicationProperties;
    private EmailNotificationService emailNotificationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);

    /**
     * Method to fetch email verification response for specific user id.
     *
     * @param userId unique user identifier.
     * @return List of EmailVerificationResponse object.
     * @throws ResourceNotFoundException ResourceNotFoundException if user id does not exist in system.
     */
    @Override
    public List<EmailVerificationResponse> getEmailVerificationByUserId(String userId)
        throws ResourceNotFoundException {
        UserEntity userEntity = usersRepository.findByIdAndStatusNot(new BigInteger(userId), UserStatus.DELETED);
        if (Objects.isNull(userEntity)) {
            throw new ResourceNotFoundException(USER, USER_ID_VARIABLE, userId);
        }
        Optional<EmailVerificationEntity> emailVerificationEntity = emailVerificationRepository
            .findByUserId(userEntity.getId());

        LOGGER.debug("Email verification entity present: {} for userId: {}",
                     emailVerificationEntity.isPresent(), userId);
        EmailVerificationResponse emailVerification = EmailVerificationResponse.builder()
            .isVerified(emailVerificationEntity.isPresent() && emailVerificationEntity.get().getIsVerified())
            .build();
        return List.of(emailVerification);
    }

    /**
     * Method to verify email with unique token.
     *
     * @param emailVerificationToken unique token generated at time of email verification.
     * @param httpServletResponse redirect response to auth server success = true/false/error page.
     * @throws IOException ioexception.
     */
    @Transactional
    @Override
    public void verifyEmail(String emailVerificationToken, HttpServletResponse httpServletResponse) throws IOException {
        validateTokenFormat(emailVerificationToken, httpServletResponse);
        try {
            String redirectUrl;
            Optional<EmailVerificationEntity> emailVerificationEntityOptional = emailVerificationRepository
                .findByToken(emailVerificationToken);
            if (emailVerificationEntityOptional.isEmpty()) {
                LOGGER.error("Email Verification Failed! Email data not found for token: {}", emailVerificationToken);
                redirectUrl = applicationProperties.getAuthServerEmailVerificationResponseUrl() + EMAIL_VERIFY_FAILED;
                httpServletResponse.sendRedirect(redirectUrl);
                return;
            }
            EmailVerificationEntity emailVerificationEntity = emailVerificationEntityOptional.get();
            LocalDateTime currentDateTime = LocalDateTime.now();
            LocalDateTime lastUpdatedTokenTime = emailVerificationEntity.getUpdateDate().toLocalDateTime();
            if (lastUpdatedTokenTime.plusDays(
                    applicationProperties.getEmailVerificationExpDays()).isBefore(currentDateTime)) {
                LOGGER.error("Email verification Failed, Token is expired! token: {} currentDateTime: {} "
                                 + "lastUpdateDateTime: {}", emailVerificationToken, currentDateTime,
                             lastUpdatedTokenTime);
                redirectUrl = applicationProperties.getAuthServerEmailVerificationResponseUrl() + EMAIL_VERIFY_FAILED;
                httpServletResponse.sendRedirect(redirectUrl);
                return;
            }
            emailVerificationEntity.setIsVerified(Boolean.TRUE);
            emailVerificationEntity.setUpdateDate(Timestamp.valueOf(currentDateTime));
            EmailVerificationEntity savedEmailVerification = emailVerificationRepository.save(emailVerificationEntity);
            LOGGER.debug("Email Verification updated with verified flag value as: {} updated on: {}",
                         savedEmailVerification.getIsVerified(), Timestamp.valueOf(currentDateTime));
            redirectUrl = applicationProperties.getAuthServerEmailVerificationResponseUrl() + EMAIL_VERIFY_SUCCESS;
            httpServletResponse.sendRedirect(redirectUrl);
        } catch (Exception exception) {
            String redirectUrl = applicationProperties.getAuthServerEmailVerificationResponseUrl()
                + EMAIL_VERIFY_ERROR;
            httpServletResponse.sendRedirect(redirectUrl);
            throw exception;
        }
    }

    /**
     * Method to validate incoming token format for email verification.
     *
     * @param emailVerificationToken incoming token as part of verify email request.
     */
    public void validateTokenFormat(String emailVerificationToken, HttpServletResponse httpServletResponse)
        throws IOException {
        try {
            ObjectConverter.stringToUuid(emailVerificationToken);
        } catch (IllegalArgumentException illegalArgumentException) {
            String redirectUrl = applicationProperties.getAuthServerEmailVerificationResponseUrl()
                + EMAIL_VERIFY_FAILED;
            httpServletResponse.sendRedirect(redirectUrl);
            throw new DataIntegrityViolationException("Token format is incorrect expecting UUID format: "
                                                          + emailVerificationToken);
        }
    }

    /**
     * Main method to validate if user exists in system and send email verification to user.
     *
     * @param userId user id for the user email verification needs to be sent.
     * @throws ResourceNotFoundException exception is thrown when user is not found.
     */
    @Override
    public void resendEmailVerification(String userId) throws ResourceNotFoundException {
        if (BooleanUtils.isFalse(applicationProperties.getIsEmailVerificationEnabled())) {
            LOGGER.info("Email verification is disabled, skipping user {} verification", userId);
            return;
        }

        UserEntity userEntity = usersRepository.findByIdAndStatusNot(new BigInteger(userId), UserStatus.DELETED);
        if (Objects.isNull(userEntity)) {
            throw new ResourceNotFoundException(USER, USER_ID_VARIABLE, userId);
        }
        UserResponseBase userResponse = UserMapper.USER_MAPPER.mapToUserResponse(userEntity);
        resendEmailVerification(userResponse);
    }

    /**
     * Method to send email verification to user via notification service.
     *
     * @param userResponse mapped userResponse
     * @return Boolean where null means email verification is not enabled, false means email is not sent and true means
     *          email sent successfully
     */
    @Override
    public Boolean resendEmailVerification(UserResponseBase userResponse) {
        if (BooleanUtils.isFalse(applicationProperties.getIsEmailVerificationEnabled())) {
            LOGGER.info("Email verification is disabled, skip sending email verification..");
            return null;
        }

        List<String> emailPatternList = applicationProperties.getEmailRegexPatternExclude();
        boolean excludeSendEmail = emailPatternList.stream().anyMatch((e -> Pattern.compile(e).matcher(
            userResponse.getEmail()).matches()));
        if (excludeSendEmail) {
            LOGGER.info("Email not sent for email id {} based on excluded email regex pattern",
                userResponse.getEmail());
            return false;
        }

        Timestamp currentTimestamp = Timestamp.valueOf(LocalDateTime.now());
        EmailVerificationEntity emailVerificationEntity;
        Optional<EmailVerificationEntity> emailVerificationEntityOptional = emailVerificationRepository
            .findByUserId(userResponse.getId());
        if (emailVerificationEntityOptional.isPresent()) {
            emailVerificationEntity = emailVerificationEntityOptional.get();
        } else {
            emailVerificationEntity = new EmailVerificationEntity();
            emailVerificationEntity.setUserId(userResponse.getId());
            emailVerificationEntity.setCreateDate(currentTimestamp);
        }
        String token = UUID.randomUUID().toString();
        emailVerificationEntity.setToken(token);
        emailVerificationEntity.setIsVerified(Boolean.FALSE);
        emailVerificationEntity.setUpdateDate(currentTimestamp);
        emailVerificationRepository.save(emailVerificationEntity);
        sendVerifyEmailNotification(userResponse, token);
        return true;
    }

    @Override
    public boolean isEmailVerificationEntityExists(BigInteger userId) {
        return emailVerificationRepository.existsByUserId(userId);
    }

    /**
     * Method to send email notification to user.
     *
     * @param userResponse userResponse.
     * @param token unique token generated to verify user.
     */
    private void sendVerifyEmailNotification(UserResponseBase userResponse, String token) {
        Map<String, String> map = new HashMap<>();
        String name = "";
        if (StringUtils.isNotEmpty(userResponse.getFirstName())) {
            name = userResponse.getFirstName();
        }
        if (StringUtils.isNotEmpty(userResponse.getLastName())) {
            name = name + " " + userResponse.getLastName();
        }
        map.put(ApiConstants.EMAIL_TO_NAME, name.trim());
        map.put(ApiConstants.EMAIL_ADDRESS, userResponse.getEmail());
        String verifyEmailUrl = String.format(applicationProperties.getEmailVerificationUrl(), token);
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("emailLink", verifyEmailUrl);
        if (!StringUtils.isEmpty(name)) {
            notificationData.put(ApiConstants.EMAIL_TO_NAME, name.trim());
        }
        emailNotificationService.sendNotification(map,
                applicationProperties.getEmailVerificationNotificationId(),
                notificationData);
    }

}
