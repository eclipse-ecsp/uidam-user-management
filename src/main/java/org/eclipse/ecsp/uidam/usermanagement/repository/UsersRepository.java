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

import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Repository to perform user CRUD operations.
 */
public interface UsersRepository extends JpaRepository<UserEntity, BigInteger>, JpaSpecificationExecutor<UserEntity> {
    UserEntity findByIdAndStatusNot(BigInteger userId, UserStatus userStatus);

    UserEntity findByIdAndStatus(BigInteger userId, UserStatus userStatus);

    UserEntity findByUserNameIgnoreCaseAndStatusNot(String userName, UserStatus userStatus);

    List<UserEntity> findAllByIdInAndStatusNot(Set<BigInteger> ids, UserStatus userStatus);

    @Query("SELECT count(1) > 0 from UserEntity user "
            + " inner join UserAccountRoleMappingEntity usermapping on user.id = usermapping.userId "
            + " where user.status != :userStatus and usermapping.roleId = :roleId ")
    boolean existsAccountRoleMappingForStatusNot(@Param("roleId") BigInteger roleId,
        @Param("userStatus") UserStatus userStatus);

    List<UserEntity> findByUserName(String userName);
}
