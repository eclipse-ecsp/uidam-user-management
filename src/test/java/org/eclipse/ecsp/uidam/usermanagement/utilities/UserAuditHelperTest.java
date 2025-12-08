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

package org.eclipse.ecsp.uidam.usermanagement.utilities;

import org.eclipse.ecsp.audit.logger.AuditLogger;
import org.eclipse.ecsp.uidam.audit.enums.AuditEventType;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.ecsp.audit.enums.AuditEventResult.SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAuditHelperTest {

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private UsersRepository userRepository;

    @InjectMocks
    private UserAuditHelper userAuditHelper;

    @Captor
    private ArgumentCaptor<String> eventTypeCaptor;

    @Captor
    private ArgumentCaptor<String> componentCaptor;

    @Captor
    private ArgumentCaptor<String> descriptionCaptor;

    private UserEntity testUser;
    private Map<BigInteger, String> accountIdToNameMapping;
    private Map<BigInteger, String> roleIdToNameMapping;
    private BigInteger loggedInUserId;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new UserEntity();
        testUser.setId(new BigInteger("12345"));
        testUser.setUserName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setEnabled(true);
        testUser.setIsExternalUser(false);
        testUser.setAccountNoLocked(true);
        testUser.setPhoneNumber("+1234567890");
        testUser.setCreatedBy("admin");
        testUser.setCreateDate(Timestamp.from(Instant.now()));

        // Setup account role mapping
        UserAccountRoleMappingEntity mapping = new UserAccountRoleMappingEntity();
        mapping.setAccountId(new BigInteger("100"));
        mapping.setRoleId(new BigInteger("200"));
        List<UserAccountRoleMappingEntity> mappings = new ArrayList<>();
        mappings.add(mapping);
        testUser.setAccountRoleMapping(mappings);

        // Setup mappings
        accountIdToNameMapping = new HashMap<>();
        accountIdToNameMapping.put(new BigInteger("100"), "Test Account");

        roleIdToNameMapping = new HashMap<>();
        roleIdToNameMapping.put(new BigInteger("200"), "Test Role");

        loggedInUserId = new BigInteger("67890");

        // Setup mock HTTP request
        mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("192.168.1.1");
        mockRequest.addHeader("User-Agent", "Test Agent");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }

    @Test
    void testLogUserCreatedAudit_SelfRegistration() {
        // When
        userAuditHelper.logUserCreatedAudit(testUser, null, true, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(
            eventTypeCaptor.capture(),
            componentCaptor.capture(),
            eq(SUCCESS),
            descriptionCaptor.capture(),
            any(),
            any(),
            any(),
            eq(null),
            eq(null),
            any(String.class),
            eq(null)
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo(AuditEventType.SELF_USER_REGISTERED.getType());
        assertThat(componentCaptor.getValue()).isEqualTo(ApiConstants.COMPONENT_NAME);
        assertThat(descriptionCaptor.getValue()).isEqualTo(AuditEventType.SELF_USER_REGISTERED.getDescription());
    }

    @Test
    void testLogUserCreatedAudit_AdminCreation() {
        // When
        userAuditHelper.logUserCreatedAudit(testUser, loggedInUserId, false, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(
            eventTypeCaptor.capture(),
            componentCaptor.capture(),
            eq(SUCCESS),
            descriptionCaptor.capture(),
            any(),
            any(),
            any(),
            eq(null),
            eq(null),
            any(String.class),
            eq(null)
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo(AuditEventType.ADMIN_USER_CREATED.getType());
        assertThat(componentCaptor.getValue()).isEqualTo(ApiConstants.COMPONENT_NAME);
        assertThat(descriptionCaptor.getValue()).isEqualTo(AuditEventType.ADMIN_USER_CREATED.getDescription());
    }

    @Test
    void testLogUserCreatedAudit_ExceptionHandling() {
        // Given
        doThrow(new RuntimeException("Test exception")).when(auditLogger).logWithStateChange(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );

        // When - should not throw exception
        userAuditHelper.logUserCreatedAudit(testUser, loggedInUserId, false, accountIdToNameMapping, roleIdToNameMapping);

        // Then - exception is logged, not thrown
        verify(auditLogger, times(1)).logWithStateChange(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testLogUserUpdatedAudit_SelfUpdate() {
        // Given
        String beforeValue = "{\"status\":\"ACTIVE\"}";

        // When
        userAuditHelper.logUserUpdatedAudit(testUser, testUser.getId(), beforeValue, true, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(
            eventTypeCaptor.capture(),
            componentCaptor.capture(),
            eq(SUCCESS),
            descriptionCaptor.capture(),
            any(),
            any(),
            any(),
            eq(null),
            eq(beforeValue),
            any(String.class),
            eq(null)
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo(AuditEventType.SELF_USER_UPDATED.getType());
        assertThat(componentCaptor.getValue()).isEqualTo(ApiConstants.COMPONENT_NAME);
    }

    @Test
    void testLogUserUpdatedAudit_AdminUpdate() {
        // Given
        String beforeValue = "{\"status\":\"ACTIVE\"}";

        // When
        userAuditHelper.logUserUpdatedAudit(testUser, loggedInUserId, beforeValue, false, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(
            eventTypeCaptor.capture(),
            componentCaptor.capture(),
            eq(SUCCESS),
            descriptionCaptor.capture(),
            any(),
            any(),
            any(),
            eq(null),
            eq(beforeValue),
            any(String.class),
            eq(null)
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo(AuditEventType.ADMIN_USER_UPDATED.getType());
    }

    @Test
    void testLogUserDeletedAudit_WithLoggedInUser() {
        // When
        userAuditHelper.logUserDeletedAudit(testUser, loggedInUserId, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(
            eventTypeCaptor.capture(),
            componentCaptor.capture(),
            eq(SUCCESS),
            descriptionCaptor.capture(),
            any(),
            any(),
            any(),
            eq(null),
            any(String.class),
            eq(null),
            eq(null)
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo(AuditEventType.ADMIN_USER_DELETED.getType());
        assertThat(componentCaptor.getValue()).isEqualTo(ApiConstants.COMPONENT_NAME);
    }

    @Test
    void testLogUserDeletedAudit_SystemUser() {
        // When - null loggedInUserId indicates system user
        userAuditHelper.logUserDeletedAudit(testUser, null, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(
            eventTypeCaptor.capture(),
            componentCaptor.capture(),
            eq(SUCCESS),
            descriptionCaptor.capture(),
            any(),
            any(),
            any(),
            eq(null),
            any(String.class),
            eq(null),
            eq(null)
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo(AuditEventType.ADMIN_USER_DELETED.getType());
    }

    @Test
    void testLogUserStatusChangedAudit() {
        // Given
        UserStatus oldStatus = UserStatus.ACTIVE;
        UserStatus newStatus = UserStatus.DEACTIVATED;

        // When
        userAuditHelper.logUserStatusChangedAudit(testUser, loggedInUserId, oldStatus, newStatus, accountIdToNameMapping);

        // Then
        ArgumentCaptor<String> beforeValueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> afterValueCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditLogger, times(1)).logWithStateChange(
            eventTypeCaptor.capture(),
            componentCaptor.capture(),
            eq(SUCCESS),
            descriptionCaptor.capture(),
            any(),
            any(),
            any(),
            eq(null),
            beforeValueCaptor.capture(),
            afterValueCaptor.capture(),
            eq(null)
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo(AuditEventType.ADMIN_USER_STATUS_CHANGED.getType());
        assertThat(beforeValueCaptor.getValue()).contains("ACTIVE");
        assertThat(afterValueCaptor.getValue()).contains("DEACTIVATED");
        assertThat(descriptionCaptor.getValue()).contains("from ACTIVE to DEACTIVATED");
    }

    @Test
    void testLogPasswordResetCompletedAudit() {
        // Given
        testUser.setPwdChangedtime(Timestamp.from(Instant.now()));

        // When
        userAuditHelper.logPasswordResetCompletedAudit(testUser, accountIdToNameMapping);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(
            eventTypeCaptor.capture(),
            componentCaptor.capture(),
            eq(SUCCESS),
            descriptionCaptor.capture(),
            any(),
            any(),
            any(),
            eq(null),
            eq(null),
            any(String.class),
            eq(null)
        );

        assertThat(eventTypeCaptor.getValue()).isEqualTo(AuditEventType.SELF_PASSWORD_RESET_COMPLETED.getType());
        assertThat(componentCaptor.getValue()).isEqualTo(ApiConstants.COMPONENT_NAME);
    }

    @Test
    void testBuildUserStateJson_CompleteUser() {
        // When
        String jsonState = userAuditHelper.buildUserStateJson(testUser, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        assertThat(jsonState).isNotNull();
        assertThat(jsonState).contains("\"id\":\"12345\"");
        assertThat(jsonState).contains("\"userName\":\"testuser\"");
        assertThat(jsonState).contains("\"email\":\"test@example.com\"");
        assertThat(jsonState).contains("\"firstName\":\"Test\"");
        assertThat(jsonState).contains("\"lastName\":\"User\"");
        assertThat(jsonState).contains("\"status\":\"ACTIVE\"");
        assertThat(jsonState).contains("\"accountId\":\"100\"");
        assertThat(jsonState).contains("\"accountName\":\"Test Account\"");
        assertThat(jsonState).contains("\"roleId\":\"200\"");
        assertThat(jsonState).contains("\"roleName\":\"Test Role\"");
    }

    @Test
    void testBuildUserStateJson_UserWithoutMappings() {
        // Given
        testUser.setAccountRoleMapping(null);

        // When
        String jsonState = userAuditHelper.buildUserStateJson(testUser, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        assertThat(jsonState).isNotNull();
        assertThat(jsonState).contains("\"id\":\"12345\"");
        assertThat(jsonState).contains("\"userName\":\"testuser\"");
        assertThat(jsonState).doesNotContain("accountRoleMappings");
    }

    @Test
    void testBuildUserStateJson_UserWithEmptyMappings() {
        // Given
        testUser.setAccountRoleMapping(new ArrayList<>());

        // When
        String jsonState = userAuditHelper.buildUserStateJson(testUser, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        assertThat(jsonState).isNotNull();
        assertThat(jsonState).contains("\"id\":\"12345\"");
        assertThat(jsonState).doesNotContain("accountRoleMappings");
    }

    @Test
    void testBuildUserStateJson_WithNullFields() {
        // Given
        testUser.setStatus(null);
        testUser.setGender(null);
        testUser.setCreateDate(null);
        testUser.setUpdateDate(null);

        // When
        String jsonState = userAuditHelper.buildUserStateJson(testUser, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        assertThat(jsonState).isNotNull();
        assertThat(jsonState).contains("\"status\":null");
        assertThat(jsonState).contains("\"gender\":null");
        assertThat(jsonState).contains("\"createDate\":null");
        assertThat(jsonState).contains("\"updateDate\":null");
    }

    @Test
    void testAuditLogging_WithoutRequestContext() {
        // Given - clear request context
        RequestContextHolder.resetRequestAttributes();

        // When - should still work without HTTP request
        userAuditHelper.logUserCreatedAudit(testUser, loggedInUserId, false, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        verify(auditLogger, times(1)).logWithStateChange(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void testLogUserCreatedAudit_WithMultipleAccountRoleMappings() {
        // Given
        UserAccountRoleMappingEntity mapping2 = new UserAccountRoleMappingEntity();
        mapping2.setAccountId(new BigInteger("101"));
        mapping2.setRoleId(new BigInteger("201"));
        testUser.getAccountRoleMapping().add(mapping2);

        accountIdToNameMapping.put(new BigInteger("101"), "Second Account");
        roleIdToNameMapping.put(new BigInteger("201"), "Second Role");

        // When
        userAuditHelper.logUserCreatedAudit(testUser, loggedInUserId, false, accountIdToNameMapping, roleIdToNameMapping);

        // Then
        ArgumentCaptor<String> afterValueCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogger).logWithStateChange(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), afterValueCaptor.capture(), any()
        );

        String afterValue = afterValueCaptor.getValue();
        assertThat(afterValue).contains("\"accountId\":\"100\"");
        assertThat(afterValue).contains("\"accountId\":\"101\"");
        assertThat(afterValue).contains("Test Account");
        assertThat(afterValue).contains("Second Account");
    }
}
