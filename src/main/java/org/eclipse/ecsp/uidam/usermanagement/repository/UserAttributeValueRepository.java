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

import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.math.BigInteger;
import java.util.List;

/**
 * Repository to query additional attribute data from db.
 */
public interface UserAttributeValueRepository extends JpaRepository<UserAttributeValueEntity, BigInteger>,
    JpaSpecificationExecutor<UserAttributeValueEntity> {
    List<UserAttributeValueEntity> findAllByUserIdIn(List<BigInteger> userIds);

    List<UserAttributeValueEntity> findAllByUserIdAndAttributeIdIn(BigInteger userId, List<BigInteger> attributeIds);

    UserAttributeValueEntity findByUserIdAndAttributeId(BigInteger userId, BigInteger attributeId);
}
