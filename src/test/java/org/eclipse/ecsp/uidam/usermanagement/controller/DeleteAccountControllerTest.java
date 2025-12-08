/*
package org.eclipse.ecsp.uidam.usermanagement.controller;

import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.CreateAccountResponse;
import org.eclipse.ecsp.uidam.accountmanagement.account.response.dto.GetAccountApiResponse;
import org.eclipse.ecsp.uidam.accountmanagement.controller.AccountController;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.service.AccountService;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_RESOURCE_PATH;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_V1_VERSION;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.PATH_VARIABLE_ACCOUNT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.CORRELATION_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.LOGGED_IN_USER_ID;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.TENANT_ID;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ AccountController.class })
@MockBean(JpaMetamodelMappingContext.class)
class DeleteAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;
    
    @MockBean
    org.eclipse.ecsp.uidam.accountmanagement.utilities.AccountAuditHelper accountAuditHelper;

    @BeforeEach
    @AfterEach
    public void cleanup() {
        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    void testDeleteAccountSuccess() throws Exception {
        Mockito.doNothing().when(accountService).deleteAccount(any(UUID.class), any(UUID.class));
        mockMvc
            .perform(MockMvcRequestBuilders.delete(ACCOUNT_V1_VERSION + ACCOUNT_RESOURCE_PATH
                    + PATH_VARIABLE_ACCOUNT_ID, String.valueOf(UUID.randomUUID()))
            .header(CORRELATION_ID, "12345").header(LOGGED_IN_USER_ID, USER_ID_VALUE)
            .header(TENANT_ID, "tenant1"))
            .andExpect(status().isOk());
    }

    @Test
    void deleteAccountEmptyUserId() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders
                        .delete(ACCOUNT_V1_VERSION + ACCOUNT_RESOURCE_PATH + PATH_VARIABLE_ACCOUNT_ID,
                                String.valueOf(UUID.randomUUID()))
                        .header(CORRELATION_ID, "12345").header(TENANT_ID, "tenant1"))
                .andExpect(status().isBadRequest())
                .andExpectAll(MockMvcResultMatchers.jsonPath("$.messages[0].key").value("missing.request.header"));
    }

    public static GetAccountApiResponse getAccountResponse() {
        Set<String> roles = new HashSet<>(Set.of("Role1", "Role2"));
        GetAccountApiResponse accountResponse = new GetAccountApiResponse();
        accountResponse.setAccountName("Test Account");
        accountResponse.setId(UUID.randomUUID().toString());
        accountResponse.setStatus(AccountStatus.DELETED);
        accountResponse.setRoles(roles);
        return accountResponse;
    }

}*/
