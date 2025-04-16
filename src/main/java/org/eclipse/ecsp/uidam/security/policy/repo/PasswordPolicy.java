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

package org.eclipse.ecsp.uidam.security.policy.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;

/**
 * Entity representing a password policy.
 */
@Entity 
@Table(name = "password_policies") 
@Data
@Slf4j
public class PasswordPolicy {

    // Reuse ObjectMapper instance to avoid unnecessary creation
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    @Column(nullable = false, updatable = false, columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "key", nullable = false, unique = true)
    private String key;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "validation_rules", nullable = false)
    private String validationRules;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "create_date")
    private Timestamp createDate;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "update_date")
    private Timestamp updateDate;

    /**
     * Get the validation rules as a Map.
     *
     * @return The validation rules as a Map.
     */
    public Map<String, Object> getValidationRules() {
        try {
            return MAPPER.readValue(validationRules, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            // Log the exception or handle as needed (e.g., log to a file, use a logger, etc.)
            log.error("Error parsing JSON: " + e.getMessage());
            // Return an empty map or a default value
            return Collections.emptyMap();
        }
    }

    /**
     * Set the validation rules as a Map.
     *
     * @param validationRulesMap The validation rules as a Map.
     */
    public void setValidationRules(Map<String, Object> validationRulesMap) {
        try {
            this.validationRules = MAPPER.writeValueAsString(validationRulesMap);
        } catch (JsonProcessingException e) {
            // Log the exception or handle as needed
            log.error("Error converting map to JSON: " + e.getMessage());
            // Set a default value for validationRules if conversion fails
            this.validationRules = "{}"; // or any default JSON string you prefer
        }
    }

}
