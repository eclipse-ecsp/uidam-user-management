package org.eclipse.ecsp.uidam.usermanagement.utilities;

import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAccountRoleMappingEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.SCOPE_SELF_MNG;

/**
 * Utilities for testing user account role mapping in associate and edit user.
 *
 * @author sputhanveett
 *
 */
public class RoleAssociationUtilities {
    
    private RoleAssociationUtilities() {}
    
    /**
     * Creates role entity.
     *
     * @param description role description
     * @param name role name
     * @param id role id
     * @return role entity
     */
    public static RolesEntity addRoles(String description, String name, int id) {
        RolesEntity rolesEntity = new RolesEntity();
        rolesEntity.setDescription(description);
        rolesEntity.setName(name);
        rolesEntity.setId(BigInteger.valueOf(id));
        return rolesEntity;
    }
    
    /**
     * Creates account entity.
     *
     * @param name Account name
     * @param status account status
     * @param roles account roles
     * @param id account id
     * @return account entity
     */
    public static AccountEntity addAccount(String name, AccountStatus status, Set<BigInteger> roles, int id) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setAccountName(name);
        accountEntity.setStatus(status);
        accountEntity.setDefaultRoles(roles);
        accountEntity.setId(BigInteger.valueOf(id));
        return accountEntity;
    }
    
    /**
     * creates user entity.
     *
     * @param name user name
     * @param password user password
     * @param email user email
     * @return user entity
     */
    public static UserEntity createUser(String name, String password, String email) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(name);
        userEntity.setUserPassword(password);
        userEntity.setEmail(email);
        userEntity.setStatus(UserStatus.ACTIVE);
        return userEntity;
    }
    
    /**
     * creates user account role mapping entity.
     *
     * @param userId user id
     * @param accountId account id
     * @param roleId role id
     * @param id id of this record
     * @return UserAccountRoleMappingEntity
     */
    public static UserAccountRoleMappingEntity addUserRoleMappingEntity(BigInteger userId, BigInteger accountId,
            BigInteger roleId, BigInteger id) {
        UserAccountRoleMappingEntity userRoleMappingEntity = new UserAccountRoleMappingEntity();
        userRoleMappingEntity.setAccountId(accountId);
        userRoleMappingEntity.setUserId(userId);
        userRoleMappingEntity.setRoleId(roleId);
        userRoleMappingEntity.setId(id);
        return userRoleMappingEntity;
    }
    
    /**
     * creates role list dto.
     *
     * @param commonRolesEntities role entities list
     * @param indices list of indexes
     * @return RoleListRepresentation
     */
    public static RoleListRepresentation createRoleListDtoRepresentation(
            List<RolesEntity> commonRolesEntities, Object... indices) {
        Scope scopeDto = new Scope();
        scopeDto.setName(SCOPE_SELF_MNG);
        scopeDto.setId("1");
        Set<RoleCreateResponse> roleDtoList = new HashSet<>();
        for (Object index : indices) {
            int idx = (Integer) index;
            RoleCreateResponse roleDto = new RoleCreateResponse();
            roleDto.setName(commonRolesEntities.get(idx).getName());
            roleDto.setId(commonRolesEntities.get(idx).getId());
            roleDto.setScopes(Collections.singletonList(scopeDto));
            roleDtoList.add(roleDto);
        }

        RoleListRepresentation roleListDto = new RoleListRepresentation();
        roleListDto.setRoles(roleDtoList);
        return roleListDto;
    }

}
