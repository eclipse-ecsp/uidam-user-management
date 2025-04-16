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

package org.eclipse.ecsp.uidam.accountmanagement.repository;

import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The AccountRepository interface provides methods for accessing account data
 * in the database.
 */
@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, BigInteger>,
        JpaSpecificationExecutor<AccountEntity> {
    Optional<AccountEntity> findByIdAndStatusNot(BigInteger userId, AccountStatus accountStatus);

    Optional<AccountEntity> findByAccountName(String accountName);
       
    List<AccountEntity> findAllByStatusAndAccountNameIn(AccountStatus status, Set<String> accountNames);
        
    @Query("SELECT id, accountName FROM AccountEntity a WHERE a.accountName IN :accountNames AND a.status=:status")
    List<Object[]> findIdAndNameByStatusAndAccountNameIn(@Param("status") AccountStatus status, 
        @Param("accountNames") Set<String> accountNames);

    @Query(value = "select ae from  AccountEntity ae "
            + "where ae.status != :status and ae.id in :ids")
    List<AccountEntity> findByAccountIdInAndStatusNot(@Param(value = "ids") Set<BigInteger> accountIds,
            @Param(value = "status") AccountStatus accountStatus);
}
