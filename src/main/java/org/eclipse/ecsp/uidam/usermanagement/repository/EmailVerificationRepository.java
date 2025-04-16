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

import jakarta.transaction.Transactional;
import org.eclipse.ecsp.uidam.usermanagement.entity.EmailVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.Optional;

/**
 * EmailVerificationRepository to perform CRUD operation on email data in db.
 */
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, BigInteger> {

    Optional<EmailVerificationEntity> findByUserId(BigInteger userId);

    Optional<EmailVerificationEntity> findByToken(String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM EmailVerificationEntity e WHERE e.userId = :userId")
    int deleteEmailVerification(@Param("userId") BigInteger userId);

    boolean existsByUserId(BigInteger userId);
}
