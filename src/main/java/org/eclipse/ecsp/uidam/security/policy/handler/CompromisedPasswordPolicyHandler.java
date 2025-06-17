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

package org.eclipse.ecsp.uidam.security.policy.handler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * This class is used to check if a password has been compromised using the Pwned Passwords API. It hashes the password
 * and checks if the hash exists in the database of compromised passwords.
 */
public class CompromisedPasswordPolicyHandler extends PasswordPolicyHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompromisedPasswordPolicyHandler.class);
    private static final int INT_5 = 5;
    private MessageDigest messageDigest;
    private static final String PWNED_PASSWORD_URL = "https://api.pwnedpasswords.com/range/";
    private int passwordHashSubStringLength;

    public CompromisedPasswordPolicyHandler(Map<String, Object> rules) {
        this.passwordHashSubStringLength = toInt(rules.get("passwordHashSubStringLength"), INT_5);
    }

    /**
     * Validates the password against the compromised password database. It checks if the password hash exists in the
     * database of compromised passwords.
     *
     * @param input The password validation input containing password and username.
     * @return true if the password is valid, false otherwise.
     */
    @Override
    protected boolean doHandle(PasswordValidationInput input) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Validating password for compromised passwords, for user: {}", input.username());
        }
        try {
            if (isPasswordCompromised(input.password())) {
                LOGGER.warn("Password is compromised, try to use strong password");
                setErrorMessage("Password is compromised, try to use strong password");
                return false;
            }
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Error while hashing password: {}", e.getMessage(), e);
            return false;
        }
        LOGGER.info("Password is not compromised, go for next validator");
        return true;
    }

    /**
     * This method is used to hash the password using SHA-1 algorithm.
     *
     * @param password The password to hash.
     * @return The hashed password.
     * @throws NoSuchAlgorithmException exception.
     */
    protected String toHash(String password) throws NoSuchAlgorithmException {
        MessageDigest localMessageDigest = getMessageDigest();
        if (password == null) {
            return "";
        } else {
            localMessageDigest.update(password.getBytes(StandardCharsets.UTF_8));
            byte[] digest = localMessageDigest.digest();
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        }
    }

    /**
     * This method is used to check if the password is compromised using the Pwned Passwords API.
     *
     * @param password The password to check.
     * @return true if the password is compromised, false otherwise.
     * @throws NoSuchAlgorithmException exception.
     */
    protected boolean isPasswordCompromised(String password) throws NoSuchAlgorithmException {

        WebClient webClient = WebClient.builder().baseUrl(PWNED_PASSWORD_URL).filter(logRequest()).filter(logResponse())
                .build();
        String passwordHash = toHash(password);
        String response = null;
        String body = null;
        String url = (StringUtils.isNotEmpty(passwordHash) ? passwordHash.substring(0, passwordHashSubStringLength)
                : "");
        try {
            response = webClient.get().uri(url).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(String.class)
                    .block();
        } catch (Exception ex) {
            LOGGER.error("Error while calling Pwned Passwords API: {}", ex.getMessage(), ex);
        }
        if (response != null) {
            body = response;
        }
        return body != null && body.contains(passwordHash.substring(passwordHashSubStringLength));
    }

    /**
     * SonarQube S4790: SHA-1 is required for compatibility with the Pwned Passwords API (not used for security).
     * Suppress this warning as this is not a security-sensitive use.
     */
    @SuppressWarnings("java:S4790")
    protected MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        if (messageDigest == null) {
            messageDigest = MessageDigest.getInstance("SHA-1");
        }
        return messageDigest;
    }

    /**
     * This method is used to log the request.
     *
     * @return ExchangeFilterFunction
     */
    public static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            LOGGER.debug("Request {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    /**
     * This method is used to log the response.
     *
     * @return ExchangeFilterFunction
     */
    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            LOGGER.info("Response status code {} ", response.statusCode());
            return Mono.just(response);
        });
    }

}
