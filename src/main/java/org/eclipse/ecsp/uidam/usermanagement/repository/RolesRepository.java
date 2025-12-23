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

package org.eclipse.ecsp.uidam.usermanagement.repository;

import org.eclipse.ecsp.uidam.usermanagement.entity.RolesEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

/**
 * Repository class for role.
 *
 * <p>This interface extends JpaRepository and CrudRepository interfaces from Spring Data JPA,
 * providing several methods for querying RolesEntity objects from the database.</p>
 *
 * <p>These methods use Spring Data JPA's query creation mechanism, where the query is derived from the method name.</p>
 */
@Repository
public interface RolesRepository extends JpaRepository<RolesEntity, BigInteger>,
        CrudRepository<RolesEntity, BigInteger> {
    /**
     * Fetches a RolesEntity object where the name attribute matches the provided parameter.
     *
     * @param name The name of the role.
     * @return The RolesEntity object.
     */
    RolesEntity getRolesByName(String name);

    /**
     * Fetches a RolesEntity object where the name attribute matches the provided parameter and is deleted flag.
     *
     * @param name    The name of the role.
     * @param deleted The deleted flag of the role.
     * @return The RolesEntity object.
     */
    RolesEntity getRolesByNameAndIsDeleted(String name, boolean deleted);

    /**
     * Fetches a RolesEntity object where the id attribute matches the provided parameter.
     *
     * @param id The id of the role.
     * @return The RolesEntity object.
     */
    RolesEntity getRolesById(BigInteger id);

    /**
     * Fetches a list of RolesEntity objects where the name attribute matches any of the provided parameters.
     *
     * @param roleNames The set of role names.
     * @param pageable  The pagination information.
     * @param deleted   Flag based fetch, if role is deleted or not.
     * @return The list of RolesEntity objects.
     */
    List<RolesEntity> findByNameInAndIsDeleted(Set<String> roleNames, Pageable pageable, boolean deleted);

    /**
     *  Fetches a list of RolesEntity objects where the name attribute matches and are not deleted.
     *
     * @param roleNames The set of role names.
     * @param deleted   Flag based fetch, if role is deleted or not.
     * @return The list of RolesEntity objects.
     */
    List<RolesEntity> findByNameInAndIsDeleted(Set<String> roleNames, boolean deleted);

    /**
     * Fetches all RolesEntity objects with pagination and is deleted flag.
     *
     * @param pageable The pagination information.
     * @param deleted  Flag based fetch, if role is deleted or not.
     * @return The list of RolesEntity objects.
     */
    List<RolesEntity> findByIsDeleted(Pageable pageable, boolean deleted);

    /**
     * Fetches a list of RolesEntity objects where the id attribute matches any of the provided parameters.
     *
     * @param roleIds The set of role ids.
     * @return The list of RolesEntity objects.
     */
    List<RolesEntity> findByIdIn(Set<BigInteger> roleIds);

    /**
     * Deletes RoleScopeMappingEntity records associated with a given role ID.
     *
     * @param roleId The id of the role.
     * @return The number of records deleted.
     */
    @Modifying
    @Query("delete from RoleScopeMappingEntity mapping where mapping.role.id=:roleId")
    int deleteRoleScopeMapping(@Param("roleId") BigInteger roleId);

    /**
     * Checks if a role with the given name exists and is not deleted.
     *
     * @param roleName The name of the role.
     * @param deleted  The deleted flag of the role.
     * @return True if the role exists and is not deleted, false otherwise.
     */
    boolean existsByNameIgnoreCaseAndIsDeleted(String roleName, boolean deleted);
}
