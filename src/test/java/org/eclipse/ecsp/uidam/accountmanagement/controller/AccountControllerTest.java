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

package org.eclipse.ecsp.uidam.accountmanagement.controller;

import io.prometheus.client.CollectorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.CreateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.request.dto.UpdateAccountDto;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.CreateAccountResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.GetAccountApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.exception.AccountManagementException;
import org.eclipse.ecsp.uidam.accountmanagement.service.AccountService;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.exception.UserNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.service.TenantConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.Assert;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_V1_VERSION;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.PATH_VARIABLE_ACCOUNT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SLASH;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TENANT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Utilities.asJsonString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@WebMvcTest({ AccountController.class })
@MockBean(JpaMetamodelMappingContext.class)
class AccountControllerTest {

    @MockBean
    org.eclipse.ecsp.uidam.accountmanagement.utilities.AccountAuditHelper accountAuditHelper;

    private static final BigInteger ACCOUNT_ID_VALUE = new BigInteger("145911385530649019822702644100150");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountController accountController;

    @MockBean
    private AccountService accountService;

    @MockBean
    private TenantConfigurationService tenantConfigurationService;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    @Order(1)
    void contextLoad() {
        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
                .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
        map.forEach((key, value) -> log.info("{} {}", key, value));
        Assert.notEmpty(map, "Handler mappings map is empty. Controller APIs are not exposed.");
        Assert.notNull(accountController, "accountController not created");
        Assert.notNull(accountService, "accountService not created");
    }

    @Test
    void createAccountSuccess() throws Exception {
        when(accountService.createAccount(any(CreateAccountDto.class), any(BigInteger.class)))
            .thenReturn(getAccountResponse());
        mockMvc
            .perform(post(ACCOUNT_V1_VERSION + ACCOUNT_RESOURCE_PATH)
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(asJsonString(getAccountDto()))
            .header(CORRELATION_ID, "12345").header(LOGGED_IN_USER_ID, USER_ID_VALUE)
            .header(TENANT_ID, "tenant1"))
            .andExpect(status().isCreated())
            .andExpectAll(MockMvcResultMatchers.jsonPath("$.id").exists());

    }

    @Test
    void addAccountWithNullUserId() throws Exception {
        when(accountService.createAccount(any(CreateAccountDto.class), any())).thenReturn(getAccountResponse());
        mockMvc.perform(post(ACCOUNT_V1_VERSION + ACCOUNT_RESOURCE_PATH).contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getAccountDto())).header(CORRELATION_ID, "12345").header(TENANT_ID, "tenant1"))
                .andExpect(status().isBadRequest())
                .andExpectAll(MockMvcResultMatchers.jsonPath("$.messages[0].key").value("missing.request.header"));

    }

    private UpdateAccountDto getUpdateAccountDto() {
        UpdateAccountDto updateAccount = new UpdateAccountDto();
        updateAccount.setStatus(AccountStatus.PENDING);
        updateAccount.setRoles(new HashSet<String>(Arrays.asList("Role1", "Role2")));
        return updateAccount;
    }

    private CreateAccountDto getAccountDto() {
        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName("Test Account");
        accountDto.setRoles(new HashSet<String>(Arrays.asList("Role1", "Role2")));
        return accountDto;
    }

    public static CreateAccountResponse getAccountResponse() {
        CreateAccountResponse accountResponse = new CreateAccountResponse();
        accountResponse.setId(UUID.randomUUID().toString());
        return accountResponse;
    }

    public static GetAccountApiResponse buildGetAccountApiResponse() {
        final long createdDate = 1709639065965L;
        final long updatedDate = 1709639312204L;
        Set<String> roles = new HashSet<>(Set.of("Role1", "Role2"));
        GetAccountApiResponse accountResponse = new GetAccountApiResponse();
        accountResponse.setAccountName("Test Account");
        accountResponse.setId(UUID.randomUUID().toString());
        accountResponse.setStatus(AccountStatus.ACTIVE);
        accountResponse.setRoles(roles);
        accountResponse.setCreatedBy("User1");
        accountResponse.setUpdatedBy("User2");
        accountResponse.setCreateDate(new Timestamp((new Date(createdDate)).getTime()));
        accountResponse.setUpdateDate(new Timestamp((new Date(updatedDate)).getTime()));
        return accountResponse;
    }

    @Test
    void testCreateAccount() throws NoSuchAlgorithmException, ResourceNotFoundException,
        AccountManagementException, UserNotFoundException {
        // Arrange
        String accountName = "Test Account";

        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName(accountName);
        CreateAccountResponse expectedResponse = new CreateAccountResponse();

        when(accountService.createAccount(accountDto, LOGGED_IN_USER_ID_VALUE))
            .thenReturn(expectedResponse);

        // Act
        ResponseEntity<CreateAccountResponse> response = accountController.createAccount(
            String.valueOf(LOGGED_IN_USER_ID_VALUE), accountDto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(accountService, times(1)).createAccount(accountDto, LOGGED_IN_USER_ID_VALUE);
    }

    @Test
    void updateAccountSuccess() throws Exception {
        mockMvc
            .perform(post(ACCOUNT_V1_VERSION + ACCOUNT_RESOURCE_PATH + PATH_VARIABLE_ACCOUNT_ID,
                    String.valueOf(ACCOUNT_ID_VALUE))
            .contentType(MediaType.APPLICATION_JSON_VALUE).content(asJsonString(getUpdateAccountDto()))
            .header(CORRELATION_ID, "12345").header(LOGGED_IN_USER_ID, USER_ID_VALUE)
            .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk());

    }

    @Test
    void updateAccountWithNullUserId() throws Exception {
        mockMvc.perform(post(ACCOUNT_V1_VERSION + ACCOUNT_RESOURCE_PATH + PATH_VARIABLE_ACCOUNT_ID,
                String.valueOf(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(getUpdateAccountDto())).header(CORRELATION_ID, "12345")
                .header(TENANT_ID, "tenant1")).andExpect(status().isBadRequest())
                .andExpectAll(MockMvcResultMatchers.jsonPath("$.messages[0].key").value("missing.request.header"));

    }

    @Test
    void testUpdateAccount() throws ResourceNotFoundException, UserNotFoundException {
        UpdateAccountDto accountBase = new UpdateAccountDto();
        accountBase.setStatus(AccountStatus.SUSPENDED);
        ResponseEntity response = accountController.updateAccount(String.valueOf(LOGGED_IN_USER_ID_VALUE),
            ACCOUNT_ID_VALUE, accountBase);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(accountService, times(1)).updateAccount(ACCOUNT_ID_VALUE, accountBase, LOGGED_IN_USER_ID_VALUE);
    }

    @Test
    void testGetAccount() throws ResourceNotFoundException {
        // Arrange
        String accountName = "Test Account";
        CreateAccountDto accountDto = new CreateAccountDto();
        accountDto.setAccountName(accountName);
        GetAccountApiResponse expectedResponse = new GetAccountApiResponse();

        when(accountService.getAccount(ACCOUNT_ID_VALUE))
            .thenReturn(expectedResponse);

        // Act
        ResponseEntity<GetAccountApiResponse> response = accountController.getAccount(ACCOUNT_ID_VALUE);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
        verify(accountService, times(1)).getAccount(ACCOUNT_ID_VALUE);
    }

    @Test
    void getAccountSuccess() throws Exception {
        when(accountService.getAccount(any(BigInteger.class))).thenReturn(buildGetAccountApiResponse());
        mockMvc.perform(get(ACCOUNT_V1_VERSION + ACCOUNT_RESOURCE_PATH + SLASH + ACCOUNT_ID_VALUE)
              .contentType(MediaType.APPLICATION_JSON_VALUE).header(CORRELATION_ID, "12345")
              .header(LOGGED_IN_USER_ID, USER_ID_VALUE).header(TENANT_ID, "tenant1"))
              .andExpect(status().is2xxSuccessful())
              .andExpectAll(MockMvcResultMatchers.jsonPath("$.accountName").value("Test Account"),
                      MockMvcResultMatchers.jsonPath("$.status").value(AccountStatus.ACTIVE.name()),
                      MockMvcResultMatchers.jsonPath("$.roles[0]").value("Role2"),
                      MockMvcResultMatchers.jsonPath("$.roles[1]").value("Role1"),
                      MockMvcResultMatchers.jsonPath("$.createdBy").value("User1"),
                      MockMvcResultMatchers.jsonPath("$.updatedBy").value("User2"),
                      MockMvcResultMatchers.jsonPath("$.createDate").value("2024-03-05T11:44:25.965+00:00"),
                      MockMvcResultMatchers.jsonPath("$.updateDate").value("2024-03-05T11:48:32.204+00:00"));

    }

    @Test
    void getAccountInvalidAccountId() throws Exception {
        when(accountService.getAccount(any(BigInteger.class))).thenReturn(
              buildGetAccountApiResponse());
        String accountId = "shjgakhfsdahy";
        mockMvc.perform(get(ACCOUNT_V1_VERSION + ACCOUNT_RESOURCE_PATH + SLASH + accountId)
              .contentType(MediaType.APPLICATION_JSON_VALUE)
              .header(CORRELATION_ID, "12345")
              .header(LOGGED_IN_USER_ID, USER_ID_VALUE)
              .header(TENANT_ID, "tenant1"))
              .andExpect(status().isBadRequest())
              .andExpectAll(
                      MockMvcResultMatchers.jsonPath("$.detail").value("Failed to convert 'account_id' "
                              + "with value: 'shjgakhfsdahy'"));
    }
}
