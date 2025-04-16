package org.eclipse.ecsp.uidam.common.utils;

import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_2;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.SCOPE_SELF_MNG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * RoleManagementUtilsTest.
 */
public class RoleManagementUtilsTest {

    private static final BigInteger ROLEID_1 = new BigInteger("145911385530649019822702644100150");
    private static final BigInteger ROLEID_2 = new BigInteger("145911385530649019822709644100150");
    private static final int INT_2 = 2;
    
    @Test
    void testValidateRoleExists() {
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        Set<String> roles = new HashSet<>();
        roles.add(ROLE_VALUE);
        boolean result = RoleManagementUtils.validateRoleExistsByName(roleListDto, roles);

        assertTrue(result);
    }

    @Test
    void testNullroleListDto() {
        Set<String> roles = new HashSet<>();
        roles.add(ROLE_VALUE);
        boolean result = RoleManagementUtils.validateRoleExistsByName(null, roles);

        assertFalse(result);
    }

    @Test
    void testGetScopesFromRolesDto() {
        RoleListRepresentation rolesDto = createRoleListDtoRepresentation();
        Set<String> roles = new HashSet<>();
        roles.add(ROLE_VALUE);

        Set<String> scopes = RoleManagementUtils.getScopesFromRolesDto(rolesDto, roles);

        assertNotNull(scopes);
        assertEquals(1, scopes.size());
    }

    @Test
    void testGetRoleNamesFromRolesDto() {
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();
        Set<BigInteger> roleIds = new HashSet<>();
        roleIds.add(ROLEID_1);
        roleIds.add(ROLEID_2);

        Set<String> roleNames = RoleManagementUtils.getRoleNamesFromRolesDto(roleListDto);

        assertNotNull(roleNames);
        assertEquals(INT_2, roleNames.size());
    }

    @Test
    void testGetScopesFromRoles() {
        RoleListRepresentation roleListDto = createRoleListDtoRepresentation();

        Set<String> scopes = RoleManagementUtils.getScopesFromRoles(roleListDto);

        assertNotNull(scopes);
        assertEquals(1, scopes.size());
    }

    @Test
    void testGetRoleIdsFromRoleList() {
        RoleListRepresentation rolesDto = createRoleListDtoRepresentation();
        Set<BigInteger> roleIds = RoleManagementUtils.getRoleIdsFromRoleList(rolesDto);

        assertNotNull(roleIds);
        assertEquals(INT_2, roleIds.size());
    }

    /**
     * Return RoleListRepresentation.
     *
     * @return RoleListRepresentation.
     */
    public static RoleListRepresentation createRoleListDtoRepresentation() {
        RoleCreateResponse role2Dto = new RoleCreateResponse();
        RoleCreateResponse vehicleOwnerRole = new RoleCreateResponse();
        Scope scopeDto = new Scope();
        scopeDto.setName(SCOPE_SELF_MNG);
        role2Dto.setName(ROLE_2);
        vehicleOwnerRole.setName(ROLE_VALUE);
        role2Dto.setId(ROLEID_1);
        vehicleOwnerRole.setId(ROLEID_2);
        role2Dto.setScopes(Collections.singletonList(scopeDto));
        vehicleOwnerRole.setScopes(Collections.singletonList(scopeDto));

        Set<RoleCreateResponse> roleDtoList = new HashSet<>();
        roleDtoList.add(role2Dto);
        roleDtoList.add(vehicleOwnerRole);
        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }
}