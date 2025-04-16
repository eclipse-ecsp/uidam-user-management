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

package org.eclipse.ecsp.uidam.usermanagement.validations.password.policy;



import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
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
 * This class is used to check compromised password.
 *
 */
public class CheckForCompromisedPassword extends AbstractPasswordValidationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckForCompromisedPassword.class);
    private MessageDigest messageDigest;
    private static final String PWNED_PASSWORD_URL = "https://api.pwnedpasswords.com/range/";
    private  static final int PASSWORD_HASH_SUBSTRING_LENGTH = 5;

    @Override
    public boolean validate(Map<String, String> userDetails) throws Exception {
        if (isPasswordCompromised(userDetails)) {
            LOGGER.warn("Password is compromised, try to use strong password");
            userDetails.put(ApiConstants.ERROR_MESSAGE, ApiConstants.WEAK_PASSWORD);
            return false;
        }
        LOGGER.info("Password is not compromised, go for next validator");
        return nextValidationHandler(userDetails);
    }

    private String toHash(String password) throws NoSuchAlgorithmException {
        MessageDigest localMessageDigest = getMessageDigest();
        if (password == null) {
            return ""; 
        } else {
            localMessageDigest.update(password.getBytes(StandardCharsets.UTF_8));
            byte[] digest = localMessageDigest.digest();
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        }
    }

    private boolean isPasswordCompromised(Map<String, String> userDetails) throws NoSuchAlgorithmException {
       
        WebClient webClient = WebClient.builder().baseUrl(PWNED_PASSWORD_URL)
                .filter(logRequest()).filter(logResponse()).build();

        String passwordHash = toHash(userDetails.get(ApiConstants.PASSWORD));
        String response = null;
        String body = null;
        String url =  (StringUtils.isNotEmpty(passwordHash) ? passwordHash.substring(0, PASSWORD_HASH_SUBSTRING_LENGTH)
                        : "");
        try {
            response =
            webClient.get().uri(url).accept(MediaType.APPLICATION_JSON).retrieve()
            .bodyToMono(String.class).block();
        } catch (Exception ex) {
            LOGGER.error("exception from third party service : ",  ex);
        }
        if (response != null) {
            body = response;
        }
        return body != null && body.contains(passwordHash.substring(PASSWORD_HASH_SUBSTRING_LENGTH));
    }

    private MessageDigest getMessageDigest() throws NoSuchAlgorithmException {
        if (messageDigest == null) {
            messageDigest = MessageDigest.getInstance("SHA-1");
        }
        return messageDigest;

    }
    
    /**
     * This method is used to log the request.

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

     * @return ExchangeFilterFunction
     */
    public static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            LOGGER.info("Response status code {} ", response.statusCode());
            return Mono.just(response);
        });
    }

}