package org.eclipse.ecsp.uidam.usermanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.CollectorRegistry;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.accountmanagement.repository.AccountRepository;
import org.eclipse.ecsp.uidam.security.policy.handler.PasswordValidationService;
import org.eclipse.ecsp.uidam.security.policy.service.PasswordPolicyService;
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import org.eclipse.ecsp.uidam.usermanagement.repository.RolesRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAccountRoleMappingRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UserAttributeValueRepository;
import org.eclipse.ecsp.uidam.usermanagement.repository.UsersRepository;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.AssociateAccountAndRolesDto;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.AssociateAccountAndRolesResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.LOGGED_IN_USER_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.USER_ID_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UsersController Integration tests.
 *
 * @author sputhanveett
 */
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "3600000")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestExecutionListeners(listeners = {
    org.eclipse.ecsp.uidam.common.test.TenantContextTestExecutionListener.class
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.springframework.context.annotation.Import(org.eclipse.ecsp.uidam.common.test.TestTenantConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsersControllerIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(UsersControllerIntegrationTest.class);

    private static final int INDEX_1 = 1;
    private static final int INDEX_2 = 2;
    private static final int INDEX_3 = 3;
    private static final int INDEX_4 = 4;
    private static final int INDEX_5 = 5;
    private static final int INDEX_0 = 0;

    private static final String DELIMITER = "_";
    //User_Account_role_mapping ids
    private static final BigInteger UAR_MAPPING_ID_1 = new BigInteger("1111385530649019822702644100150");
    private static final BigInteger UAR_MAPPING_ID_2 = new BigInteger("2222385530649019822702644100150");
    private static final BigInteger UAR_MAPPING_ID_3 = new BigInteger("3333385530649019822702644100150");
    private static final BigInteger UAR_MAPPING_ID_4 = new BigInteger("44444385530649019822702644100150");
    private static final BigInteger UAR_MAPPING_ID_5 = new BigInteger("55555385530649019822702644100150");

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private UsersRepository userRepository;

    @MockBean
    private RolesRepository rolesRepository;

    @MockBean
    private UserAttributeValueRepository userAttributeValueRepository;

    @MockBean
    private UserAccountRoleMappingRepository userAccountRoleMappingRepository;

    private ObjectMapper mapper = new ObjectMapper();

    private List<RolesEntity> rolesEntities;

    private List<AccountEntity> accountEntities;
    
    @MockBean
    PasswordValidationService passwordValidationService;
    
    @MockBean
    PasswordPolicyService passwordPolicyService;
    
    @MockBean
    org.eclipse.ecsp.uidam.usermanagement.utilities.UserAuditHelper userAuditHelper;
    
    /**
     * Init before each test.
     */
    @BeforeAll
    public void init() {
        CollectorRegistry.defaultRegistry.clear();
        
        // Configure WebTestClient with default tenantId header for all requests
        webTestClient = webTestClient.mutate()
                .defaultHeader("tenantId", "ecsp")
                .build();

        rolesEntities = List.of(addRoles("Bussiness_admin", "BUSSINESS_ADMIN", INDEX_1),
                addRoles("Guest", "GUEST", INDEX_2), addRoles("Tenant", "TENANT", INDEX_3),
                addRoles("Admin", "ADMIN", INDEX_4), addRoles("Vehicle Owner", "VEHICLE_OWNER", INDEX_5));
        accountEntities = List.of(addAccount("Ignite_Account", AccountStatus.ACTIVE,
                        Set.of(rolesEntities.get(INDEX_0).getId(), rolesEntities.get(INDEX_2).getId()), INDEX_1),
                addAccount("UIDAM_Account", AccountStatus.ACTIVE,
                        Set.of(rolesEntities.get(INDEX_0).getId(),
                                rolesEntities.get(INDEX_1).getId(),
                                rolesEntities.get(INDEX_2).getId()), INDEX_2),
                addAccount("AiLabs_Account", AccountStatus.ACTIVE,
                        Set.of(rolesEntities.get(INDEX_1).getId(),
                                rolesEntities.get(INDEX_3).getId()), INDEX_3),
                addAccount("Analytics_Account", AccountStatus.DELETED,
                        Set.of(rolesEntities.get(INDEX_0).getId(),
                                rolesEntities.get(INDEX_3).getId(),
                                rolesEntities.get(INDEX_4).getId()), INDEX_4));
        // Ensure mocked repository returns the entity passed to save(), to mimic JPA behaviour
        when(userRepository.save(any(org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
    
    @BeforeEach
    public void perTestSetup() {
        // Re-stub save for each test to ensure it persists across all test executions
        when(userRepository.save(any(org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testAssociateRolesWithInvalidUser() {

        UserEntity userEntity = new UserEntity();
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                null);
        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(new ArrayList<>()).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);


    }

    @Test
    void testAssociateAccountAndRolesDtoForInvalidPayload() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);
        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("ADD",
                "/account/accountId/roleName",
                rolesEntities.get(INDEX_3).getName()));

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("REMOVE",
                "/account/accountId1",
                rolesEntities.get(INDEX_3).getName()));

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("REMOVE",
                "/account/c344c206-269c-4edc-898d-g90003bf3617/"
                + rolesEntities.get(INDEX_3).getName(), ""));

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("REPLACE",
                "/account/c344c206-269c-4edc-898d-g90003bf3617/"
                + rolesEntities.get(INDEX_3).getName(), ""));

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

    }

    @Test
    void testAssociateRolesWithValidData() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName",
                rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/"
                + rolesEntities.get(INDEX_2).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2),
                rolesEntities.get(INDEX_3)
        ));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                        accountEntities.get(INDEX_0),
                        accountEntities.get(INDEX_1),
                        accountEntities.get(INDEX_2)
                ));

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(rolesEntities.get(INDEX_2),
                rolesEntities.get(INDEX_3)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class))).thenReturn(new ArrayList<>());

        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "Before, User role mapping entity list size should be 2");

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.OK);

        verify(userRepository, times(1)).save(any(UserEntity.class));

        assertEquals(INDEX_1, userEntity.getAccountRoleMapping().size(),
                "After, User role mapping entity list size should be 1");

        UserAccountRoleMappingEntity userAccountRoleMappingEntity = userEntity.getAccountRoleMapping().get(INDEX_0);
        assertEquals(USER_ID_VALUE, userAccountRoleMappingEntity.getUserId());
        assertEquals(accountEntities.get(INDEX_2).getId(), userAccountRoleMappingEntity.getAccountId());
        assertEquals(rolesEntities.get(INDEX_3).getId(), userAccountRoleMappingEntity.getRoleId());
    }

    @Test
    void associateRolesWithMultipleRemovePayloads() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_0).getId(), UAR_MAPPING_ID_3));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_0).getId(), UAR_MAPPING_ID_4));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_5));

        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/"
                + rolesEntities.get(INDEX_2).getName(), ""));
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_1).getId() + "/"
                + rolesEntities.get(INDEX_2).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2)));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(accountEntities.get(INDEX_0), accountEntities.get(INDEX_1)));

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(rolesEntities.get(INDEX_2)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class))).thenReturn(new ArrayList<>());

        assertEquals(INDEX_5, userEntity.getAccountRoleMapping().size(), "Before, URM list size should be 5");

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.OK);

        verify(userRepository, times(1)).save(any(UserEntity.class));

        userRoleMappingEntityList = userEntity.getAccountRoleMapping();
        assertEquals(INDEX_3, userRoleMappingEntityList.size(), "After, URM list size should be 3");

        Set<String> userAccountRoleMappingDataSet = getUserAccountRoleMappingData(userEntity);
        assertEquals(INDEX_3, userAccountRoleMappingDataSet.size());
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_0), rolesEntities.get(INDEX_0))));
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_1), rolesEntities.get(INDEX_0))));
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_1), rolesEntities.get(INDEX_1))));
    }

    private static String getDelimitedData(BigInteger userId, AccountEntity accountEntity, RolesEntity rolesEntity) {
        return userId + DELIMITER + accountEntity.getId() + DELIMITER + rolesEntity.getId();
    }

    private static Set<String> getUserAccountRoleMappingData(UserEntity userEntity) {
        return userEntity.getAccountRoleMapping().stream()
                .map(entity -> entity.getUserId() + DELIMITER + entity.getAccountId() + DELIMITER
                        + entity.getRoleId()).collect(Collectors.toSet());
    }

    @Test
    void testAssociateRolesWithMultiplePayloads() {


        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_0).getId(), UAR_MAPPING_ID_2));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_2).getId(),
                        rolesEntities.get(INDEX_3).getId(), UAR_MAPPING_ID_3));

        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_2).getId(),
                accountEntities.get(INDEX_2).getAccountName()));

        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                        accountEntities.get(INDEX_2)
                ));

        assertEquals(INDEX_3, userEntity.getAccountRoleMapping().size(),
                "Before, User account role mapping list size should be 3");

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.OK);

        verify(userRepository, times(1)).save(any(UserEntity.class));
        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "After, User account role mapping list size should be 2");

        Set<String> userAccountRoleMappingDataSet = getUserAccountRoleMappingData(userEntity);
        assertEquals(INDEX_2, userAccountRoleMappingDataSet.size());
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_1), rolesEntities.get(INDEX_0))
        ));
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_1), rolesEntities.get(INDEX_1))
        ));

    }

    @Test
    void associateRolesWithMultiRemoveAndAddPayloads() {
        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_0).getId(), UAR_MAPPING_ID_3));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_0).getId(), UAR_MAPPING_ID_4));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_5));

        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_0).getId(),
                accountEntities.get(INDEX_0).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add",
            "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName", rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("remove",
            "/account/" + accountEntities.get(INDEX_0).getId() + "/" + rolesEntities.get(INDEX_2).getName(), ""));
        list.add(new AssociateAccountAndRolesDto("remove",
            "/account/" + accountEntities.get(INDEX_1).getId() + "/" + rolesEntities.get(INDEX_2).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2), rolesEntities.get(INDEX_3)));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                     accountEntities.get(INDEX_0), accountEntities.get(INDEX_1), accountEntities.get(INDEX_2)));

        UserEntity loggedInuser = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuser.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuser.setUserAddresses(new ArrayList<>());
        loggedInuser.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(rolesEntities.get(INDEX_3),
            rolesEntities.get(INDEX_2)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(loggedInuser);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class))).thenReturn(new ArrayList<>());

        assertEquals(INDEX_5, userEntity.getAccountRoleMapping().size(), "Before, URM list size should be 5");
        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.OK);

        verify(userRepository, times(1)).save(any(UserEntity.class));
        assertEquals(INDEX_3, userEntity.getAccountRoleMapping().size(), "After, URM list size should be 3");

        Set<String> userAccountRoleMappingDataSet = getUserAccountRoleMappingData(userEntity);
        assertEquals(INDEX_3, userAccountRoleMappingDataSet.size());
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_1), rolesEntities.get(INDEX_0))));
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_1), rolesEntities.get(INDEX_1))));
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_2), rolesEntities.get(INDEX_3))));
    }

    @Test
    void testAssociateRolesWithInvalidPayloadForAddAccount() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName",
                rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/"
                + rolesEntities.get(INDEX_1).getName(), ""));
        list.add(new AssociateAccountAndRolesDto("add", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void associateRolesWithInvalidAccountAndRoleForRemoveAndAddPayload() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        userAccountRoleMappingRepository.saveAll(userRoleMappingEntityList);
        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName",
                rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/"
                + rolesEntities.get(INDEX_1).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_1), rolesEntities.get(INDEX_3)));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                        accountEntities.get(INDEX_0), accountEntities.get(INDEX_1),
                        accountEntities.get(INDEX_2)));

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3), rolesEntities.get(INDEX_1)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class)))
                .thenReturn(new ArrayList<>());

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    void associateRolesWithValidAccountAndRoleForRemoveAndAddPayload2() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        userAccountRoleMappingRepository.saveAll(userRoleMappingEntityList);
        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);

        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                        accountEntities.get(INDEX_0), accountEntities.get(INDEX_1),
                        accountEntities.get(INDEX_2)));

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class)))
                .thenReturn(new ArrayList<>());

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName",
                rolesEntities.get(INDEX_4).getName()));
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/"
                + rolesEntities.get(INDEX_2).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2), rolesEntities.get(INDEX_4)));

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_2), rolesEntities.get(INDEX_4)));

        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "Before, User account role mapping list size should be 2");

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.OK);

        verify(userRepository, times(1)).save(any(UserEntity.class));
        assertEquals(INDEX_1, userEntity.getAccountRoleMapping().size(),
                "After, User account role mapping list size should be 1");

        Set<String> userAccountRoleMappingDataSet = getUserAccountRoleMappingData(userEntity);
        assertEquals(INDEX_1, userAccountRoleMappingDataSet.size());
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_2), rolesEntities.get(INDEX_4))
        ));
    }

    @Test
    void testAssociateRolesByRemovingAllAccountsForUser() throws ResourceNotFoundException {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        userAccountRoleMappingRepository.saveAll(userRoleMappingEntityList);
        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class)))
                .thenReturn(new ArrayList<>());

        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "Before, User role mapping entity list size should be 2");

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_0).getId(),
                accountEntities.get(INDEX_0).getAccountName()));

        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                        accountEntities.get(INDEX_0), accountEntities.get(INDEX_1)
                ));

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "After, User role mapping entity list size should be 2");
    }


    @Test
    void testAssociateRolesByRemovingAllAccountsForUser2() throws ResourceNotFoundException {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        userAccountRoleMappingRepository.saveAll(userRoleMappingEntityList);
        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class)))
                .thenReturn(new ArrayList<>());

        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "Before, User role mapping entity list size should be 2");

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_0).getId(),
                accountEntities.get(INDEX_0).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName",
                rolesEntities.get(INDEX_3).getName()));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_3)
        ));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                        accountEntities.get(INDEX_0), accountEntities.get(INDEX_1),
                        accountEntities.get(INDEX_2)));

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.OK);

        verify(userRepository, times(1)).save(any(UserEntity.class));
        assertEquals(INDEX_1, userEntity.getAccountRoleMapping().size(),
                "After, User role mapping entity list size should be 1");

        Set<String> userAccountRoleMappingDataSet = getUserAccountRoleMappingData(userEntity);
        assertEquals(INDEX_1, userAccountRoleMappingDataSet.size());
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_2), rolesEntities.get(INDEX_3))));
    }

    @Test
    void associateRolesWithInvalidAccountIdAndNameInPayload() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(userEntity);

        //Invalid accountId in the add payload
        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/00000000-0000-0000-0000-000000000000/roleName", rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("remove", "/account/"
                 + accountEntities.get(INDEX_0).getId() + "/" + rolesEntities.get(INDEX_2).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2), rolesEntities.get(INDEX_3)));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(accountEntities.get(INDEX_0), accountEntities.get(INDEX_2)));

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3), rolesEntities.get(INDEX_2)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class))).thenReturn(new ArrayList<>());

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        //Invalid account name  in the remove payload
        list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/1023", "Invalid_UIDAM_Account"));
        list.add(new AssociateAccountAndRolesDto("add", "/account/"
                 + accountEntities.get(INDEX_2).getId() + "/roleName", rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("remove", "/account/"
                 + accountEntities.get(INDEX_0).getId() + "/" + rolesEntities.get(INDEX_2).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2), rolesEntities.get(INDEX_3)));

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        //valid account name  in the remove payload but not present in the mapping table
        list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_2).getId(),
                accountEntities.get(INDEX_2).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add", "/account/"
                 + accountEntities.get(INDEX_2).getId() + "/roleName", rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("remove", "/account/"
                 + accountEntities.get(INDEX_0).getId() + "/" + rolesEntities.get(INDEX_2).getName(), ""));

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

    }

    @Test
    void testRoleAssociationForInvalidRequestPayload() {

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("add", "",
                accountEntities.get(INDEX_2).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("", "/account/accountName",
                accountEntities.get(INDEX_2).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("remove", "/account/accountName", ""));


        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

    }

    @Test
    void testRoleAssociationWithDuplicateRemovePayload() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        userAccountRoleMappingRepository.saveAll(userRoleMappingEntityList);
        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName",
                rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/"
                + rolesEntities.get(INDEX_2).getName(), ""));
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/"
                + rolesEntities.get(INDEX_2).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2), rolesEntities.get(INDEX_3)));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                        accountEntities.get(INDEX_0), accountEntities.get(INDEX_1), accountEntities.get(INDEX_2)));

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3), rolesEntities.get(INDEX_2)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class))).thenReturn(new ArrayList<>());

        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "Before, User role mapping entity list size should be 2");
        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.OK);

        verify(userRepository, times(1)).save(any(UserEntity.class));
        assertEquals(INDEX_1, userEntity.getAccountRoleMapping().size(),
                "After, User account role mapping list size should be 1");

        Set<String> userAccountRoleMappingDataSet = getUserAccountRoleMappingData(userEntity);
        assertEquals(INDEX_1, userAccountRoleMappingDataSet.size());
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_2), rolesEntities.get(INDEX_3))
        ));
    }

    @Test
    void testRoleAssociationWithDuplicateAddPayload() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        userAccountRoleMappingRepository.saveAll(userRoleMappingEntityList);
        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add", "/account/"
                + accountEntities.get(INDEX_2).getId() + "/roleName", rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("remove", "/account/"
                + accountEntities.get(INDEX_0).getId() + "/" + rolesEntities.get(INDEX_2).getName(), ""));
        list.add(new AssociateAccountAndRolesDto("add", "/account/"
                + accountEntities.get(INDEX_2).getId() + "/roleName", rolesEntities.get(INDEX_3).getName()));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2), rolesEntities.get(INDEX_3)));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                        accountEntities.get(INDEX_0),
                        accountEntities.get(INDEX_1),
                        accountEntities.get(INDEX_2)
        ));

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3), rolesEntities.get(INDEX_2)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class)))
                .thenReturn(new ArrayList<>());

        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "Before, User role mapping entity list size should be 2");
        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.OK);

        verify(userRepository, times(1)).save(any(UserEntity.class));
        assertEquals(INDEX_1, userEntity.getAccountRoleMapping().size(),
                "After, User account role mapping list size should be 1");

        Set<String> userAccountRoleMappingDataSet = getUserAccountRoleMappingData(userEntity);
        assertEquals(INDEX_1, userAccountRoleMappingDataSet.size());
        assertTrue(userAccountRoleMappingDataSet.contains(
                getDelimitedData(USER_ID_VALUE, accountEntities.get(INDEX_2), rolesEntities.get(INDEX_3))
        ));
    }

    @Test
    void testAssociateRolesWithInvalidRoleName() {

        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();

        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        userAccountRoleMappingRepository.saveAll(userRoleMappingEntityList);
        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("remove", "/account/" + accountEntities.get(INDEX_1).getId(),
                accountEntities.get(INDEX_1).getAccountName()));
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName", "INVALID_ADMIN"));
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/"
                + rolesEntities.get(INDEX_2).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2)
        ));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(
                        accountEntities.get(INDEX_0),
                        accountEntities.get(INDEX_1),
                        accountEntities.get(INDEX_2)
                ));

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3), rolesEntities.get(INDEX_2)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class))).thenReturn(new ArrayList<>());

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

    }

    @Test
    void testAssociateRolesWithDeletedAccountData() {
        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));

        userAccountRoleMappingRepository.saveAll(userRoleMappingEntityList);
        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_3).getId() + "/roleName",
                rolesEntities.get(INDEX_3).getName()));
        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_3)
        ));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of());

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class)))
                .thenReturn(new ArrayList<>());

        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "Before, User role mapping entity list size should be 2");

        webTestClient.patch().uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
            http.add("Content-Type", "application/json");
            http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
            http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
        }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);

        assertEquals(INDEX_2, userEntity.getAccountRoleMapping().size(),
                "After, User role mapping entity list size should be 2");
    }

    @Test
    void testAssociateRolesResponseWithValidData() {
        UserEntity userEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        userEntity.setId(USER_ID_VALUE);

        List<UserAccountRoleMappingEntity> userRoleMappingEntityList = new ArrayList<>();
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_2).getId(), UAR_MAPPING_ID_1));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_0).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_2));
        userRoleMappingEntityList.add(
                addUserRoleMappingEntity(USER_ID_VALUE, accountEntities.get(INDEX_1).getId(),
                        rolesEntities.get(INDEX_1).getId(), UAR_MAPPING_ID_3));

        userAccountRoleMappingRepository.saveAll(userRoleMappingEntityList);
        userEntity.setAccountRoleMapping(userRoleMappingEntityList);
        when(userRepository.findByIdAndStatusNot(USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                userEntity);

        List<AssociateAccountAndRolesDto> list = new ArrayList<>();
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName",
                rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/roleName",
                rolesEntities.get(INDEX_3).getName()));
        list.add(new AssociateAccountAndRolesDto("add",
                "/account/" + accountEntities.get(INDEX_2).getId() + "/roleName",
                rolesEntities.get(INDEX_2).getName()));
        list.add(new AssociateAccountAndRolesDto("remove",
                "/account/" + accountEntities.get(INDEX_0).getId() + "/"
                + rolesEntities.get(INDEX_2).getName(), ""));

        when(rolesRepository.findByNameInAndIsDeleted(anySet(), anyBoolean())).thenReturn(List.of(
                rolesEntities.get(INDEX_2), rolesEntities.get(INDEX_3)));
        when(accountRepository.findByAccountIdInAndStatusNot(anySet(), any(AccountStatus.class)))
                .thenReturn(List.of(accountEntities.get(INDEX_0), accountEntities.get(INDEX_2)));
        when(accountRepository.findAllById(anySet())).thenReturn(List.of(
                accountEntities.get(INDEX_0), accountEntities.get(INDEX_1), accountEntities.get(INDEX_2)));
        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_1), rolesEntities.get(INDEX_2), rolesEntities.get(INDEX_3)));

        UserEntity loggedInuserEntity = createUser("Ignite_User", "Ignite_Password", "ignite@gamil.com");
        loggedInuserEntity.setId(LOGGED_IN_USER_ID_VALUE);
        loggedInuserEntity.setUserAddresses(new ArrayList<>());
        loggedInuserEntity.setAccountRoleMapping(userRoleMappingEntityList);

        when(rolesRepository.findByIdIn(anySet())).thenReturn(List.of(
                rolesEntities.get(INDEX_3), rolesEntities.get(INDEX_2)));
        when(userRepository.findByIdAndStatusNot(LOGGED_IN_USER_ID_VALUE, UserStatus.DELETED)).thenReturn(
                loggedInuserEntity);

        when(userAttributeValueRepository.findAllByUserIdIn(any(List.class)))
                .thenReturn(new ArrayList<>());

        assertEquals(INDEX_3, userEntity.getAccountRoleMapping().size(), "Before, URM entity list size should be 3");

        byte[] response = webTestClient.patch()
                .uri("/v1/users/" + USER_ID_VALUE + "/accountRoleMapping").headers(http -> {
                    http.add("Content-Type", "application/json");
                    http.add(ApiConstants.CORRELATION_ID, UUID.randomUUID().toString());
                    http.add(ApiConstants.LOGGED_IN_USER_ID, String.valueOf(LOGGED_IN_USER_ID_VALUE));
                }).bodyValue(list).exchange().expectStatus().isEqualTo(HttpStatus.OK).expectBody()
                .returnResult().getResponseBody();

        assertEquals(INDEX_5, userEntity.getAccountRoleMapping().size(), "After, URM entity list size should be 5");

        AssociateAccountAndRolesResponse associateAccountAndRolesResponse = convertResponse(response);
        assertEquals(INDEX_3, associateAccountAndRolesResponse.getAccounts().size(), "Number of accounts must be 3");
    }

    private AssociateAccountAndRolesResponse convertResponse(byte[] arr) {
        AssociateAccountAndRolesResponse associateAccountAndRolesResponse = null;
        try {
            associateAccountAndRolesResponse = mapper.readValue(arr, AssociateAccountAndRolesResponse.class);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return associateAccountAndRolesResponse;
    }

    private UserAccountRoleMappingEntity addUserRoleMappingEntity(BigInteger userId, BigInteger accountId,
                                                                  BigInteger roleId, BigInteger id) {
        UserAccountRoleMappingEntity userRoleMappingEntity = new UserAccountRoleMappingEntity();
        userRoleMappingEntity.setAccountId(accountId);
        userRoleMappingEntity.setUserId(userId);
        userRoleMappingEntity.setRoleId(roleId);
        userRoleMappingEntity.setId(id);
        return userRoleMappingEntity;
    }

    private UserEntity createUser(String name, String password, String email) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(name);
        userEntity.setUserPassword(password);
        userEntity.setEmail(email);
        userEntity.setStatus(UserStatus.ACTIVE);
        return userEntity;
    }

    private RolesEntity addRoles(String description, String name, int id) {
        RolesEntity rolesEntity = new RolesEntity();
        rolesEntity.setDescription(description);
        rolesEntity.setName(name);
        rolesEntity.setId(BigInteger.valueOf(id));
        return rolesEntity;
    }

    private AccountEntity addAccount(String name, AccountStatus status, Set<BigInteger> roles, int id) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setAccountName(name);
        accountEntity.setStatus(status);
        accountEntity.setDefaultRoles(roles);
        accountEntity.setId(BigInteger.valueOf(id));
        return accountEntity;
    }
}
