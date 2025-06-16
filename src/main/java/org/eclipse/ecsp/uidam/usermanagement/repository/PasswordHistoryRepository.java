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

import org.eclipse.ecsp.uidam.usermanagement.entity.PasswordHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * Repository class for password history of the user.
 */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, BigInteger> {

    @Query("SELECT a FROM PasswordHistoryEntity a WHERE a.userName = :userName ORDER BY a.createDate DESC")
    List<PasswordHistoryEntity> findPasswordHistoryByUserName(@Param("userName") String userName, Pageable pageable);

    @Query("SELECT MAX(p.createDate) FROM PasswordHistoryEntity p WHERE p.userName = :username")
    Date findLastPasswordChangeDate(@Param("username") String username);      
}
