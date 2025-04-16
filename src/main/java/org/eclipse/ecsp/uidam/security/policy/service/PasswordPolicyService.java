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

package org.eclipse.ecsp.uidam.security.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.security.policy.exception.PasswordPolicyException;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicyRepository;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.exception.handler.ErrorProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;


/**
 * Service class for managing password policies.
 */
@Service 
@Slf4j
public class PasswordPolicyService {

    private static final int INT_8 = 8;
    private static final int INT_2 = 2;
    public static final String POLICY_DOES_NOT_EXIST_MSG = "The policy does not exist: {0}";
    public static final String POLICY_DOES_NOT_EXIST = "um.policy.does.not.exist";
    public static final String PATCH_POLICY_DOES_NOT_EXIST_MSG = "Apply patch failed,policy not exit policy key:{0}";
    public static final String PATCH_POLICY_DOES_NOT_EXIST = "um.patch.policy.not.exist";
    public static final String INVALID_PATCH_POLICY_OPERATION_MSG = "Apply patch failed,invalid patch opeation:{0}";
    public static final String INVALID_PATCH_POLICY_OPERATION = "um.patch.invalid.policy.operation";
    public static final String PATCH_POLICY_ERROR_MSG = "Apply patch failed,with loginuser:{0}";
    public static final String PATCH_POLICY_ERROR = "um.patch.policy.failed";
    public static final String PATCH_POLICY_VALIDATION_ERROR_MSG = 
            "Apply patch failed,validation failed with error:{0}";
    public static final String PATCH_POLICY_VALIDATION_ERROR = "um.patch.policy.validation.failed";
    public static final String POLICY_ID = "policyKey";
    private static final String POLICY_ERROR = "policyError";
    
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public PasswordPolicyService(PasswordPolicyRepository passwordPolicyRepository, ObjectMapper objectMapper) {
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Retrieves all password policies.
     *
     * @return List of password policies.
     */
    public List<PasswordPolicy> getAllPolicies() {
        return passwordPolicyRepository.findAll();
    }

    /**
     * Retrieves a password policy by its key.
     *
     * @param key The key of the policy.
     * @return The found password policy.
     */
    public PasswordPolicy getPolicyByKey(String key) {
        Optional<PasswordPolicy> policy = passwordPolicyRepository.findByKey(key);
        if (policy.isEmpty()) {
            throwException(POLICY_DOES_NOT_EXIST_MSG, POLICY_DOES_NOT_EXIST, POLICY_ID, key, NOT_FOUND);
            return null;
        }
        return policy.get();
    }

    /**
     * Applies a JSON Patch operation to update password policies.
     *
     * @param patch The JSON Patch containing update operations.
     * @param loginUserId The ID of the user performing the update.
     * @return Updated list of password policies.
     */
    @Transactional
    public List<PasswordPolicy> updatePolicies(JsonNode patch, String loginUserId) {
        log.info("Applying patch to password policies. User: {}", loginUserId);
        Map<String, JsonPatch> policiesPatchMap = processJsonPatch(patch);
        // Get all policies and apply the patch to each
        List<PasswordPolicy> policies = passwordPolicyRepository.findAll();
        Map<String, PasswordPolicy> dbPolicyMap = policies.stream()
                .collect(Collectors.toMap(PasswordPolicy::getKey, policy -> policy));
        log.debug("Fetched all password policies. Applying patch to each policy.");
        List<PasswordPolicy> updatedPolicies = new ArrayList<>();
        policiesPatchMap.entrySet().stream().forEach(entry -> {
            PasswordPolicy policy = dbPolicyMap.get(entry.getKey());
            if (policy != null) {
                updatedPolicies.add(applyPatchToPolicy(entry.getValue(), policy, loginUserId));
            } else {
                throwException(PATCH_POLICY_DOES_NOT_EXIST_MSG, PATCH_POLICY_DOES_NOT_EXIST, POLICY_ID, entry.getKey(),
                        BAD_REQUEST);
            }
        });
        passwordPolicyRepository.saveAll(updatedPolicies);
        log.info("Updated password policies successfully.");
        return updatedPolicies;
    }

    /**
     * Processes a JSON Patch to group operations by policy key.
     *
     * @param jsonPatch The JSON Patch to process.
     * @return A map of policy keys to JSON Patches.
     */
    public Map<String, JsonPatch> processJsonPatch(JsonNode jsonPatch) {
        Map<String, List<JsonNode>> groupedPatch = new HashMap<>();
        if (jsonPatch.isArray()) {
            for (JsonNode patch : jsonPatch) {
                String op = patch.get("op").asText();
                if (!op.equals("replace")) {
                    throwException(INVALID_PATCH_POLICY_OPERATION_MSG, INVALID_PATCH_POLICY_OPERATION, "operation", op,
                            BAD_REQUEST);
                }
                String path = patch.get("path").asText();
                String[] pathSegments = path.split(ApiConstants.SLASH);

                if (pathSegments.length > 1) {
                    // Extract the key from the path
                    String key = pathSegments[1]; // e.g., "size" or "specialChars"
                    String modifiedPath = ApiConstants.SLASH + String.join(ApiConstants.SLASH,
                            Arrays.copyOfRange(pathSegments, INT_2, pathSegments.length));

                    // Create a modified patch by updating the path
                    ObjectNode modifiedPatch = (ObjectNode) patch.deepCopy();
                    modifiedPatch.put("path", modifiedPath);

                    // Group patches by key
                    groupedPatch.computeIfAbsent(key, k -> new ArrayList<>()).add(modifiedPatch);
                }
            }
        }

        return groupedPatch.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, // Keep the key as is
                entry -> {
                    try {
                        ArrayNode patchArray = objectMapper.valueToTree(entry.getValue());
                        return JsonPatch.fromJson(patchArray);
                    } catch (IOException e) {
                        return null;
                    }
                }));

    }

    /**
     * Applies a JSON Patch operation to update a password policy.
     *
     * @param patch The JSON Patch containing update operations.
     * @param key The key of the policy to update.
     * @param loginUserId The ID of the user performing the update.
     * @return The updated password policy.
     */
    protected PasswordPolicy applyPatchToPolicy(JsonPatch patch, PasswordPolicy policy, String loginUserId) {
        try {
            log.debug("Applying patch to policy: {}", policy.getKey());
            // Step 1: Clone the original policy object
            PasswordPolicy originalPolicy = objectMapper.readValue(objectMapper.writeValueAsString(policy),
                    PasswordPolicy.class);
            // Convert policy to JsonNode (ObjectNode to mutate it)
            JsonNode policyNode = objectMapper.valueToTree(policy);
            // Apply the patch
            JsonNode patchedNode = patch.apply(policyNode);
            // Convert the patched JsonNode back to a PasswordPolicy object
            PasswordPolicy updatedPolicy = objectMapper.readerForUpdating(policy).readValue(patchedNode);
            log.debug("Applied patch to policy: {}", policy.getKey());
            // Validate the updated policy
            validatePolicyUpdate(originalPolicy, updatedPolicy);
            updatedPolicy.setUpdateDate(new Timestamp(System.currentTimeMillis()));
            updatedPolicy.setUpdatedBy(String.valueOf(loginUserId));
            log.debug("Updated policy: {}", updatedPolicy.getKey());
            return updatedPolicy;
        } catch (IOException | JsonPatchException e) {
            log.error("Failed to apply patch to policy.", e);
            throwException(PATCH_POLICY_ERROR_MSG, PATCH_POLICY_ERROR, "user", loginUserId, BAD_REQUEST);
            return null;
        }
    }

    

    /**
     * Validates a password policy update.
     *
     * @param originalPolicy The original password policy.
     * @param updatedPolicy The updated password policy.
     */
    protected void validatePolicyUpdate(PasswordPolicy originalPolicy, PasswordPolicy updatedPolicy) {
        log.debug("Validating policy update: {}", updatedPolicy.getKey());
        // Custom validation logic for policy updates
        if (!originalPolicy.isRequired() && !updatedPolicy.isRequired()) {
            String errorMsg = String.format("Key: %s, Cannot update a disabled password policy.",
                    originalPolicy.getKey());
            throwException(PATCH_POLICY_VALIDATION_ERROR_MSG, PATCH_POLICY_VALIDATION_ERROR, POLICY_ERROR, errorMsg,
                    BAD_REQUEST);
        }
        if (updatedPolicy.getValidationRules() == null) {
            String errorMsg = String.format("Key: %s, Validation rules cannot be null.", originalPolicy.getKey());
            throwException(PATCH_POLICY_VALIDATION_ERROR_MSG, PATCH_POLICY_VALIDATION_ERROR, POLICY_ERROR, errorMsg,
                    BAD_REQUEST);
        }
        if (originalPolicy.getKey().equals("size")) {
            Integer minLength = (Integer) updatedPolicy.getValidationRules().get("minLength");
            Integer maxLength = (Integer) updatedPolicy.getValidationRules().get("maxLength");
            if (minLength < INT_8 || maxLength <= minLength) {
                String errorMsg = String.format(
                        "Key %s, Invalid length constraints: minLength must be >= 8 and maxLength > minLength.",
                        originalPolicy.getKey());
                throwException(PATCH_POLICY_VALIDATION_ERROR_MSG, PATCH_POLICY_VALIDATION_ERROR, POLICY_ERROR,
                        errorMsg, BAD_REQUEST);
            }
        }
        if (originalPolicy.getKey().equals("specialChars")) {
            String allowed = (String) updatedPolicy.getValidationRules().get("allowedSpecialChars");
            String excluded = (String) updatedPolicy.getValidationRules().get("excludedSpecialChars");
            if (allowed != null && excluded != null && allowed.chars().anyMatch(ch -> excluded.indexOf(ch) >= 0)) {
                String errorMsg = String.format("Key %s, Excluded characters must not be in allowed set.",
                        originalPolicy.getKey());
                throwException(PATCH_POLICY_VALIDATION_ERROR_MSG, PATCH_POLICY_VALIDATION_ERROR, POLICY_ERROR,
                        errorMsg, BAD_REQUEST);
            }
        }
    }

    /**
     * Throws a PasswordPolicyException with the given message pattern and property value.
     *
     * @param msgPattern The message pattern.
     * @param msgKey The message key.
     * @param propertyKey The property key.
     * @param propertyValue The property value.
     * @param httpStatus The HTTP status.
     */
    protected void throwException(String msgPattern, String msgKey, String propertyKey, String propertyValue,
            HttpStatus httpStatus) {
        String errorMessage = formatErrorMessage(msgPattern, propertyValue);
        log.error(errorMessage);
        ErrorProperty property = new ErrorProperty();
        property.setKey(propertyKey);
        property.setValues(Arrays.asList(propertyValue));
        throw new PasswordPolicyException(msgKey, errorMessage, Arrays.asList(property), httpStatus);
    }

    /**
     * Formats an error message using the given message pattern and arguments.
     *
     * @param msgPattern The message pattern.
     * @param arguments The arguments to format the message.
     * @return The formatted error message.
     */
    private String formatErrorMessage(String msgPattern, Object... arguments) {
        MessageFormat msgFormat = new MessageFormat(msgPattern);
        StringBuffer errorMessage = new StringBuffer();
        msgFormat.format(arguments, errorMessage, new FieldPosition(0));
        return errorMessage.toString();
    }
}
