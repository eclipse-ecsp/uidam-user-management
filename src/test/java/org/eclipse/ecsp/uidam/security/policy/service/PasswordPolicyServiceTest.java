package org.eclipse.ecsp.uidam.security.policy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.eclipse.ecsp.uidam.security.policy.exception.PasswordPolicyException;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordPolicyServiceTest {

    private static final int INT_5 = 5;
    private static final int INT_6 = 6;
    private static final int INT_8 = 8;
    private static final int INT_10 = 10;
    private static final int INT_12 = 12;
    private static final int INT_16 = 16;


    @Mock
    private PasswordPolicyRepository passwordPolicyRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PasswordPolicyService passwordPolicyService;

    private PasswordPolicy policy;

    private String policyKey = "size";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setting up a basic PasswordPolicy object for testing
        policy = new PasswordPolicy();
        policy.setKey(policyKey);
        policy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_8);
        validationRules.put("maxLength", INT_16);
        policy.setValidationRules(validationRules);
        policy.setName("Test Policy");
        policy.setDescription("Test Description");
        policy.setPriority(1);
        policy.setCreatedBy("admin");
        policy.setCreateDate(new Timestamp(System.currentTimeMillis()));
    }

    // Test for getting all policies
    @Test
    void testGetAllPolicies() {
        List<PasswordPolicy> policies = Arrays.asList(policy);

        when(passwordPolicyRepository.findAll()).thenReturn(policies);

        List<PasswordPolicy> result = passwordPolicyService.getAllPolicies();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(policyKey, result.get(0).getKey());
    }

    // Test for getting a policy by key
    @Test
    void testGetPolicyByKey() {
        when(passwordPolicyRepository.findByKey("passwordPolicy1")).thenReturn(Optional.of(policy));

        PasswordPolicy result = passwordPolicyService.getPolicyByKey("passwordPolicy1");
        assertNotNull(result);
        assertEquals(policyKey, result.getKey());
    }

    @Test
    void testGetPolicyByKeyNotFound() {
        when(passwordPolicyRepository.findByKey("passwordPolicy1")).thenReturn(Optional.empty());

        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.getPolicyByKey("passwordPolicy1");
        });
        assertTrue(exception.getMessage().contains("The policy does not exist"));
    }

    // Test for applying JSON patch to update policies
    @Test
    void testUpdatePolicies() throws IOException {
        String patchJson = "[{\"op\":\"replace\", \"path\":\"/size/validationRules/minLength\", \"value\":10}]";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode patchNode = objectMapper.readTree(patchJson);
        when(passwordPolicyRepository.findAll()).thenReturn(Arrays.asList(policy));
        // Simulating successful patch application
        Map<String, JsonPatch> patchMap = new HashMap<>();
        patchMap.put(policyKey, JsonPatch.fromJson((ArrayNode) patchNode));
        String modifiedPatchJson = "[{\"op\":\"replace\", \"path\":\"/validationRules/minLength\", \"value\":10}]";
        JsonNode modifiedPatchJsonNode = objectMapper.readTree(modifiedPatchJson);
        when(this.objectMapper.valueToTree(any(Object.class)))
                .thenReturn(objectMapper.valueToTree(modifiedPatchJsonNode))
                .thenReturn(objectMapper.valueToTree(objectMapper.valueToTree(policy)));
        // Create a mock patched Policy
        PasswordPolicy patchedPolicy = new PasswordPolicy();
        patchedPolicy.setKey("size");
        patchedPolicy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_10);
        validationRules.put("maxLength", INT_16);
        patchedPolicy.setValidationRules(validationRules);
        // Mock the behavior of objectReader.readValue(patchedNode) to return the patched Policy
        when(this.objectMapper.writeValueAsString(policy)).thenReturn(objectMapper.writeValueAsString(policy));
        when(this.objectMapper.readValue(objectMapper.writeValueAsString(policy), PasswordPolicy.class))
                .thenReturn(policy);
        // Mock the behavior of objectMapper.readerForUpdating(policy) to return a mocked ObjectReader
        ObjectReader objectReader = mock(ObjectReader.class);
        when(this.objectMapper.readerForUpdating(policy)).thenReturn(objectReader);
        when(objectReader.readValue(any(JsonNode.class))).thenReturn(patchedPolicy);
        List<PasswordPolicy> updatedPolicies = passwordPolicyService.updatePolicies(patchNode, "user1");
        assertNotNull(updatedPolicies);
        assertEquals(1, updatedPolicies.size());
        assertEquals(INT_10, updatedPolicies.get(0).getValidationRules().get("minLength"));
    }

    @Test
    void testUpdatePoliciesWhenPolicyNotFound() throws IOException {
        String patchJson = "[{\"op\":\"replace\", \"path\":\"/size/minLength\", \"value\":10}]";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode patchNode = objectMapper.readTree(patchJson);
        String modifiedPatchJson = "[{\"op\":\"replace\", \"path\":\"/minLength\", \"value\":10}]";
        JsonNode modifiedPatchJsonNode = objectMapper.readTree(modifiedPatchJson);
        when(this.objectMapper.valueToTree(any(Object.class)))
                .thenReturn(objectMapper.valueToTree(modifiedPatchJsonNode));
        // Simulate empty result from the repository for the given patch
        when(passwordPolicyRepository.findAll()).thenReturn(Collections.emptyList());

        // Test patch application
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.updatePolicies(patchNode, "user1");
        });
        assertTrue(exception.getMessage().contains("Apply patch failed,policy not exit policy key:" + policyKey));
    }

    // Test for validating a policy update
    @Test
    void testValidatePolicyUpdateValid() {
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_8);
        validationRules.put("maxLength", INT_16);
        policy.setValidationRules(validationRules);

        PasswordPolicy updatedPolicy = new PasswordPolicy();
        updatedPolicy.setKey("passwordPolicy1");
        updatedPolicy.setRequired(true);
        updatedPolicy.setValidationRules(validationRules);

        // Test validation for valid update
        try {
            passwordPolicyService.validatePolicyUpdate(policy, updatedPolicy);
        } catch (PasswordPolicyException e) {
            fail("Validation failed when it should have passed");
        }
    }

    @Test
    void testValidatePolicyUpdateInvalidMinLength() {
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_5);
        validationRules.put("maxLength", INT_16);
        policy.setValidationRules(validationRules);

        PasswordPolicy updatedPolicy = new PasswordPolicy();
        updatedPolicy.setKey(policyKey);
        updatedPolicy.setRequired(true);
        updatedPolicy.setValidationRules(validationRules);

        // Test validation for invalid minLength
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.validatePolicyUpdate(policy, updatedPolicy);
        });
        assertTrue(exception.getMessage().contains("Invalid length constraints"));
    }

    // Test for processing JSON Patch
    @Test
    void testProcessJsonPatch() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String patchJson = "[{\"op\":\"replace\", \"path\":\"/size/minLength\", \"value\":10}]";
        JsonNode patchNode = objectMapper.readTree(patchJson);
        String modifiedPatchJson = "[{\"op\":\"replace\", \"path\":\"/minLength\", \"value\":10}]";
        JsonNode modifiedPatchJsonNode = objectMapper.readTree(modifiedPatchJson);
        when(this.objectMapper.valueToTree(any(Object.class)))
                .thenReturn(objectMapper.valueToTree(modifiedPatchJsonNode));
        // Test the patch processing logic
        Map<String, JsonPatch> processedPatch = passwordPolicyService.processJsonPatch(patchNode);
        assertNotNull(processedPatch);
        assertTrue(processedPatch.containsKey(policyKey));
    }

    @Test
    void testApplyPatchToPolicyError() throws IOException, JsonPatchException {
        // Ensure the policy object is properly initialized
        PasswordPolicy policy = new PasswordPolicy();
        policy.setKey(policyKey);
        policy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_8);
        validationRules.put("maxLength", INT_12);
        policy.setValidationRules(validationRules);

        // Create a mock patch object
        JsonPatch patch = mock(JsonPatch.class);

        // Create a mock JsonNode for testing purposes
        JsonNode mockJsonNode = mock(JsonNode.class);

        // Mock the behavior of objectMapper.valueToTree() to return a JsonNode
        when(objectMapper.valueToTree(any(Object.class))).thenReturn(mockJsonNode);

        // Simulate an invalid patch operation by throwing an exception when applying it
        when(patch.apply(any(JsonNode.class))).thenThrow(new JsonPatchException("invalid patch operation"));

        // Expecting an exception when calling applyPatchToPolicy
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.applyPatchToPolicy(patch, policy, "user1");
        });

        // Verify that the exception message contains the expected error
        assertTrue(exception.getMessage().contains("Apply patch failed,with loginuser:user1"));
    }

    // Test for throwing exceptions
    @Test
    void testThrowException() {
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.throwException("Test error message", "um.patch.policy.failed", "policyKey",
                    "testPolicyKey", HttpStatus.BAD_REQUEST);
        });
        assertTrue(exception.getMessage().contains("Test error message"));
    }

    @Test
    void testGetValidationRules_ValidJson() throws JsonProcessingException {
        // Arrange
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        String validJson = "{\"minLength\": 8, \"maxLength\": 16}";
        passwordPolicy.setValidationRules(Map.of("minLength", INT_8, "maxLength", INT_16));

        // Act
        Map<String, Object> validationRules = passwordPolicy.getValidationRules();

        // Assert
        assertNotNull(validationRules);
        assertEquals(INT_8, validationRules.get("minLength"));
        assertEquals(INT_16, validationRules.get("maxLength"));
    }

    @Test
    void testGetValidationRules_InvalidJson() {
        // Arrange
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        passwordPolicy.setValidationRules(Map.of("minLength", INT_8, "maxLength", INT_16));

        // Act
        Map<String, Object> validationRules = passwordPolicy.getValidationRules();

        // Assert
        assertNotNull(validationRules);
        assertFalse(validationRules.isEmpty());
    }

    @Test
    void testSetValidationRules_ValidMap() {
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        // Arrange
        Map<String, Object> validationRulesMap = Map.of("minLength", INT_8, "maxLength", INT_16);

        // Act
        passwordPolicy.setValidationRules(validationRulesMap);

        // Assert
        assertEquals(INT_8, passwordPolicy.getValidationRules().get("minLength"));
        assertEquals(INT_16, passwordPolicy.getValidationRules().get("maxLength"));
    }

    @Test
    void testSetValidationRules_InvalidMap() {
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        // Arrange
        Map<String, Object> invalidMap = Collections.singletonMap("invalidKey", new Object());

        // Act
        passwordPolicy.setValidationRules(invalidMap);

        // Assert
        assertTrue(passwordPolicy.getValidationRules().isEmpty());
    }

    @Test
    void testValidatePolicyUpdate_DisabledPolicy() {
        // Arrange
        PasswordPolicy originalPolicy = new PasswordPolicy();
        originalPolicy.setRequired(false);
        originalPolicy.setKey("exampleKey");

        PasswordPolicy updatedPolicy = new PasswordPolicy();
        updatedPolicy.setRequired(false);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            passwordPolicyService.validatePolicyUpdate(originalPolicy, updatedPolicy);
        });
        assertEquals("Apply patch failed,validation failed with error:Key:"
                + " exampleKey, Cannot update a disabled password policy.", exception.getMessage());
    }

    @Test
    void testValidatePolicyUpdate_NullValidationRules() {
        // Arrange
        PasswordPolicy originalPolicy = new PasswordPolicy();
        originalPolicy.setKey("exampleKey");

        PasswordPolicy updatedPolicy = new PasswordPolicy();
        updatedPolicy.setValidationRules(null);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            passwordPolicyService.validatePolicyUpdate(originalPolicy, updatedPolicy);
        });
        assertEquals("Apply patch failed,validation failed with error:Key"
                + ": exampleKey, "
                + "Cannot update a disabled password policy.", exception.getMessage());
    }

    @Test
    void testValidatePolicyUpdate_InvalidLengthConstraints() {
        // Arrange
        PasswordPolicy originalPolicy = new PasswordPolicy();
        originalPolicy.setRequired(false);
        originalPolicy.setKey("size");

        PasswordPolicy updatedPolicy = new PasswordPolicy();
        originalPolicy.setRequired(true);
        updatedPolicy.setValidationRules(Map.of("minLength", INT_6, "maxLength", INT_5));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            passwordPolicyService.validatePolicyUpdate(originalPolicy, updatedPolicy);
        });
        assertEquals("Apply patch failed,validation failed with error:Key size, "
                + "Invalid length constraints: minLength must be >= 8 and maxLength > minLength.",
                exception.getMessage());
    }

    @Test
    void testValidatePolicyUpdate_InvalidSpecialChars() {
        // Arrange
        PasswordPolicy originalPolicy = new PasswordPolicy();
        originalPolicy.setRequired(true);
        originalPolicy.setKey("specialChars");

        PasswordPolicy updatedPolicy = new PasswordPolicy();
        originalPolicy.setRequired(true);
        updatedPolicy.setValidationRules(Map.of("allowedSpecialChars", "!@#", "excludedSpecialChars", "@"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            passwordPolicyService.validatePolicyUpdate(originalPolicy, updatedPolicy);
        });
        assertEquals("Apply patch failed,validation failed with error:"
                + "Key specialChars, Excluded characters must not be in allowed set.", exception.getMessage());
    }
}
