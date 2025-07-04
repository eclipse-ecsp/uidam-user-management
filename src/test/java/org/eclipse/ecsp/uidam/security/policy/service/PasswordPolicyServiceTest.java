package org.eclipse.ecsp.uidam.security.policy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.eclipse.ecsp.uidam.security.policy.exception.PasswordPolicyException;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicy;
import org.eclipse.ecsp.uidam.security.policy.repo.PasswordPolicyRepository;
import org.eclipse.ecsp.uidam.usermanagement.service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.math.BigInteger;
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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordPolicyServiceTest {

    private static final int INT_2 = 2;
    private static final int INT_5 = 5;
    private static final int INT_6 = 6;
    private static final int INT_8 = 8;
    private static final int INT_10 = 10;
    private static final int INT_12 = 12;
    private static final int INT_16 = 16;

    @Mock
    private PasswordPolicyRepository passwordPolicyRepository;

    @Mock
    private UsersService usersService;
    
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PasswordPolicyService passwordPolicyService;

    private PasswordPolicy commonPolicy;

    private String policyKey = "size";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setting up a basic PasswordPolicy object for testing
        commonPolicy = new PasswordPolicy();
        commonPolicy.setKey(policyKey);
        commonPolicy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_8);
        validationRules.put("maxLength", INT_16);
        commonPolicy.setValidationRules(validationRules);
        commonPolicy.setName("Test Policy");
        commonPolicy.setDescription("Test Description");
        commonPolicy.setPriority(1);
        commonPolicy.setCreatedBy("admin");
        commonPolicy.setCreateDate(new Timestamp(System.currentTimeMillis()));
    }

    // Test for getting all policies
    @Test
    void testGetAllPolicies() {
        List<PasswordPolicy> policies = Arrays.asList(commonPolicy);

        when(passwordPolicyRepository.findAll()).thenReturn(policies);
        when(usersService.hasUserPermissionForScope(any(BigInteger.class), anySet())).thenReturn(true);
        List<PasswordPolicy> result = passwordPolicyService.getAllPolicies(new BigInteger("12345678901234567890"));
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(policyKey, result.get(0).getKey());
    }

    @Test
    void testGetAllPoliciesNoPermission() {
        when(usersService.hasUserPermissionForScope(any(BigInteger.class), anySet())).thenReturn(false);
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.getAllPolicies(new BigInteger("12345678901234567890"));
        });
        assertTrue(exception.getMessage().contains("User does not have permission"));
    }

    // Test for getting a policy by key
    @Test
    void testGetPolicyByKey() {
        when(passwordPolicyRepository.findByKey("passwordPolicy1")).thenReturn(Optional.of(commonPolicy));
        when(usersService.hasUserPermissionForScope(any(BigInteger.class), anySet())).thenReturn(true);
        PasswordPolicy result = passwordPolicyService.getPolicyByKey("passwordPolicy1",
                new BigInteger("12345678901234567890"));
        assertNotNull(result);
        assertEquals(policyKey, result.getKey());
    }

    @Test
    void testGetPolicyByKeyNotFound() {
        when(passwordPolicyRepository.findByKey("passwordPolicy1")).thenReturn(Optional.empty());
        when(usersService.hasUserPermissionForScope(any(BigInteger.class), anySet())).thenReturn(true);
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.getPolicyByKey("passwordPolicy1", new BigInteger("12345678901234567890"));
        });
        assertTrue(exception.getMessage().contains("The policy does not exist"));
    }

    // Test for applying JSON patch to update policies
    @Test
    void testUpdatePolicies() throws IOException {
        String patchJson = "[{\"op\":\"replace\", \"path\":\"/size/validationRules/minLength\", \"value\":10}]";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode patchNode = objectMapper.readTree(patchJson);
        when(passwordPolicyRepository.findAll()).thenReturn(Arrays.asList(commonPolicy));
        // Simulating successful patch application
        Map<String, JsonPatch> patchMap = new HashMap<>();
        patchMap.put(policyKey, JsonPatch.fromJson((ArrayNode) patchNode));
        String modifiedPatchJson = "[{\"op\":\"replace\", \"path\":\"/validationRules/minLength\", \"value\":10}]";
        JsonNode modifiedPatchJsonNode = objectMapper.readTree(modifiedPatchJson);
        when(this.objectMapper.valueToTree(any(Object.class)))
                .thenReturn(objectMapper.valueToTree(modifiedPatchJsonNode))
                .thenReturn(objectMapper.valueToTree(objectMapper.valueToTree(commonPolicy)));
        // Create a mock patched Policy
        PasswordPolicy patchedPolicy = new PasswordPolicy();
        patchedPolicy.setKey("size");
        patchedPolicy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_10);
        validationRules.put("maxLength", INT_16);
        patchedPolicy.setValidationRules(validationRules);
        // Mock the behavior of objectReader.readValue(patchedNode) to return the patched Policy
        when(this.objectMapper.writeValueAsString(commonPolicy))
            .thenReturn(objectMapper.writeValueAsString(commonPolicy));
        when(this.objectMapper.readValue(objectMapper.writeValueAsString(commonPolicy), PasswordPolicy.class))
                .thenReturn(commonPolicy);
        // Mock the behavior of objectMapper.readerForUpdating(policy) to return a mocked ObjectReader
        ObjectReader objectReader = mock(ObjectReader.class);
        when(this.objectMapper.readerForUpdating(commonPolicy)).thenReturn(objectReader);
        when(objectReader.readValue(any(JsonNode.class))).thenReturn(patchedPolicy);
        when(usersService.hasUserPermissionForScope(any(BigInteger.class), anySet())).thenReturn(true);
        List<PasswordPolicy> updatedPolicies = passwordPolicyService.updatePolicies(patchNode,
                new BigInteger("12345678901234567890"));
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
        when(usersService.hasUserPermissionForScope(any(BigInteger.class), anySet())).thenReturn(true);
        // Test patch application
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.updatePolicies(patchNode, new BigInteger("12345678901234567890"));
        });
        assertTrue(exception.getMessage().contains("Apply patch failed,policy not exit policy key:" + policyKey));
    }

    // Test for validating a policy update
    @Test
    void testValidatePolicyUpdateValid() {
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_8);
        validationRules.put("maxLength", INT_16);
        commonPolicy.setValidationRules(validationRules);

        PasswordPolicy updatedPolicy = new PasswordPolicy();
        updatedPolicy.setKey("passwordPolicy1");
        updatedPolicy.setRequired(true);
        updatedPolicy.setValidationRules(validationRules);

        // Test validation for valid update
        try {
            passwordPolicyService.validatePolicyUpdate(commonPolicy, updatedPolicy);
        } catch (PasswordPolicyException e) {
            fail("Validation failed when it should have passed");
        }
    }

    @Test
    void testValidatePolicyUpdateInvalidMinLength() {
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_5);
        validationRules.put("maxLength", INT_16);
        commonPolicy.setValidationRules(validationRules);

        PasswordPolicy updatedPolicy = new PasswordPolicy();
        updatedPolicy.setKey(policyKey);
        updatedPolicy.setRequired(true);
        updatedPolicy.setValidationRules(validationRules);

        // Test validation for invalid minLength
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.validatePolicyUpdate(commonPolicy, updatedPolicy);
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
    void testProcessJsonPatchEmptyArray() throws Exception {
        PasswordPolicyService service = new PasswordPolicyService(passwordPolicyRepository, new ObjectMapper(),
                usersService);
        JsonNode emptyArray = new ObjectMapper().readTree("[]");
        Map<String, JsonPatch> result = service.processJsonPatch(emptyArray);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testProcessJsonPatchUnsupportedOp() throws IOException {
        PasswordPolicyService service = 
                new PasswordPolicyService(passwordPolicyRepository, new ObjectMapper(), usersService);
        String patchJson = "[{\"op\":\"add\", \"path\":\"/size/minLength\", \"value\":10}]";
        JsonNode patchNode = new ObjectMapper().readTree(patchJson);
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            service.processJsonPatch(patchNode);
        });
        assertTrue(exception.getMessage().contains("invalid patch opeation"));
    }

    @Test
    void testValidatePolicyUpdate_InvalidPasswordExpiryDays() {
        PasswordPolicy originalPolicy = new PasswordPolicy();
        originalPolicy.setRequired(true);
        originalPolicy.setKey("expiry");
        Map<String, Object> rules = new HashMap<>();
        rules.put("passwordExpiryDays", 0);
        PasswordPolicy updatedPolicy = new PasswordPolicy();
        updatedPolicy.setRequired(true);
        updatedPolicy.setKey("expiry");
        updatedPolicy.setValidationRules(rules);
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            passwordPolicyService.validatePolicyUpdate(originalPolicy, updatedPolicy);
        });
        assertTrue(exception.getMessage().contains("passwordExpiryDays must be greater than 0"));
    }

    @Test
    void testApplyPatchToPolicyError() throws JsonPatchException  {
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
            passwordPolicyService.applyPatchToPolicy(patch, policy, new BigInteger("12345678901234567890"));
        });

        // Verify that the exception message contains the expected error
        assertTrue(exception.getMessage().contains("Apply patch failed,with loginuser:12345678901234567890"));
    }

    @Test
    void testApplyPatchToPolicyHappyPath() throws Exception {
        PasswordPolicy passpolicy = new PasswordPolicy();
        passpolicy.setKey("size");
        passpolicy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_8);
        validationRules.put("maxLength", INT_16);
        passpolicy.setValidationRules(validationRules);
        PasswordPolicyService service = new PasswordPolicyService(passwordPolicyRepository, new ObjectMapper(),
                usersService);
        // Patch both minLength and maxLength using the correct nested path
        JsonPatch patch = JsonPatch.fromJson(new ObjectMapper()
                .readTree("[\n" + "  {\"op\":\"replace\",\"path\":\"/validationRules/minLength\",\"value\":10},\n"
                        + "  {\"op\":\"replace\",\"path\":\"/validationRules/maxLength\",\"value\":16}\n" + "]"));
        PasswordPolicy updated = service.applyPatchToPolicy(patch, passpolicy, new BigInteger("12345678901234567890"));
        assertNotNull(updated);
        assertEquals(INT_10, updated.getValidationRules().get("minLength"));
        assertEquals(INT_16, updated.getValidationRules().get("maxLength"));
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
    void testGetValidationRules_ValidJson() {
        // Arrange
        PasswordPolicy passwordPolicy = new PasswordPolicy();
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

    @Test
    void testConstructorWithNulls() {
        PasswordPolicyService service = new PasswordPolicyService(null, null, null);
        assertNotNull(service);
    }

    @Test
    void testGetAllPoliciesNoArg() {
        PasswordPolicyService service = new PasswordPolicyService(passwordPolicyRepository, new ObjectMapper(),
                usersService);
        List<PasswordPolicy> policies = Arrays.asList(new PasswordPolicy());
        when(passwordPolicyRepository.findAll()).thenReturn(policies);
        List<PasswordPolicy> result = service.getAllPolicies();
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testFormatErrorMessageMultipleArgs() {
        PasswordPolicyService service = new PasswordPolicyService(null, null, null);
        String msg = service.getClass().getDeclaredMethods()[service.getClass().getDeclaredMethods().length - 1]
                .getName(); // just to avoid unused warning
        String formatted = service.getClass().getDeclaredMethods()[service.getClass().getDeclaredMethods().length
                - INT_2].getName(); // just to avoid unused warning
        String result = service.getClass().getSimpleName(); // just to avoid unused warning
        // Actually test formatErrorMessage via throwException
        Exception exception = assertThrows(PasswordPolicyException.class, () -> {
            service.throwException("Error {0} {1}", "key", "p", "v", HttpStatus.BAD_REQUEST);
        });
        assertTrue(exception.getMessage().contains("Error v {1}"));
    }


    @Test
    void testApplyPatchToPolicy_JsonPatchException() throws Exception {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setKey("size");
        policy.setRequired(true);
        Map<String, Object> validationRules = new HashMap<>();
        validationRules.put("minLength", INT_8);
        validationRules.put("maxLength", INT_16);
        policy.setValidationRules(validationRules);
        JsonPatch patch = mock(JsonPatch.class);
        JsonNode mockJsonNode = mock(JsonNode.class);
        when(objectMapper.valueToTree(any(Object.class))).thenReturn(mockJsonNode);
        when(patch.apply(any(JsonNode.class))).thenThrow(new JsonPatchException("Simulated patch error"));
        assertThrows(PasswordPolicyException.class, () ->
            passwordPolicyService.applyPatchToPolicy(patch, policy, new BigInteger("12345678901234567890"))
        );
    }
}
