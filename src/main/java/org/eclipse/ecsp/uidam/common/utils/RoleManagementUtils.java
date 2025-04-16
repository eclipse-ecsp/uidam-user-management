/*
*********************************************************************
* COPYRIGHT (c) 2024 Harman International Industries, Inc.          *
*                                                                   *
* All rights reserved                                               *
*                                                                   *
* This software embodies materials and concepts which are           *
* confidential to Harman International Industries, Inc. and is      *
* made available solely pursuant to the terms of a written license  *
* agreement with Harman International Industries, Inc.              *
*                                                                   *
* Designed and Developed by Harman International Industries, Inc.   *
*-------------------------------------------------------------------*
* MODULE OR UNIT: uidam-user-management                                   *
*********************************************************************
*/

package org.eclipse.ecsp.uidam.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.RoleCreateResponse;
import org.eclipse.ecsp.uidam.usermanagement.auth.response.dto.Scope;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.RoleListRepresentation;
import org.springframework.util.CollectionUtils;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for role management operations.
 */
@Slf4j
public class RoleManagementUtils {

    private RoleManagementUtils() {

    }

    /**
     * Validates if the given roles exist in the provided RoleListRepresentation.
     *
     * @param roleListDto The RoleListRepresentation object containing the list of
     *                    roles.
     * @param roles       The set of role names to validate.
     * @return true if all the roles exist, false otherwise.
     */
    public static boolean validateRoleExistsByName(RoleListRepresentation roleListDto, Set<String> roles) {
        if (log.isDebugEnabled()) {
            log.debug("Check for user role {}", roles);
        }
        // Verify result
        if (roleListDto == null) {
            log.warn("auth management response does NOT contain results, roles {} do not exist", roles);
            return false;
        }
        Set<String> savedRoles = roleListDto.getRoles().stream().map(RoleCreateResponse::getName)
                .collect(Collectors.toSet());
        log.debug("Check for user role {}", roles);
        Set<String> userRoles = new HashSet<>(roles);
        userRoles.removeAll(savedRoles);
        if (CollectionUtils.isEmpty(savedRoles) || !CollectionUtils.isEmpty(userRoles)) {
            log.warn("Provided roles {} do not exist in our db", userRoles);
            return false;
        }
        return true;
    }
    
    
    /**
     * Retrieves the scopes from the RoleListRepresentation for the given roles.
     *
     * @param rolesDto The RoleListRepresentation object containing the list of
     *                 roles.
     * @param roles    The set of roles to retrieve the scopes from.
     * @return a set of scope names associated with the given roles.
     */
    public static Set<String> getScopesFromRolesDto(RoleListRepresentation rolesDto, Set<String> roles) {
        return roles.stream().flatMap(role -> rolesDto.getRoles().stream().filter(o -> role.equals(o.getName()))
                .flatMap(o -> o.getScopes().stream())).map(Scope::getName).collect(Collectors.toSet());
        
    }

    /**
     * Retrieves the role names from the RoleListRepresentation for the given role
     * IDs.
     *
     * @param roleListDto The RoleListRepresentation object containing the list of
     *                    roles.
     * @return a set of role names associated with the given role IDs.
     */
    public static Set<String> getRoleNamesFromRolesDto(RoleListRepresentation roleListDto) {
        return roleListDto.getRoles().stream().map(RoleCreateResponse::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves the scopes from the RoleListRepresentation for the given role IDs.
     *
     * @param roleListDto The RoleListRepresentation object containing the list of
     *                    roles.
     * @return a set of scope names associated with the given role IDs.
     */
    public static Set<String> getScopesFromRoles(RoleListRepresentation roleListDto) {
        return roleListDto.getRoles().stream()
                .flatMap(role -> role.getScopes().stream())
                .map(Scope::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves the role IDs from the RoleListRepresentation for the given roles.
     *
     * @param rolesDto The RoleListRepresentation object containing the list of
     *                 roles.
     * @return a set of role IDs associated with the given roles.
     */
    public static Set<BigInteger> getRoleIdsFromRoleList(RoleListRepresentation rolesDto) {
        return Optional.ofNullable(rolesDto).map(RoleListRepresentation::getRoles).orElse(Collections.emptySet())
                .stream()
                .map(RoleCreateResponse::getId).collect(Collectors.toSet());
    }
    
}
