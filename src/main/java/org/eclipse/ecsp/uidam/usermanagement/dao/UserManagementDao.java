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

package org.eclipse.ecsp.uidam.usermanagement.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dao class get field meta data from db.
 */
@Repository
@AllArgsConstructor
@Slf4j
public class UserManagementDao {
    private static final String TABLE_NAME = "tableName";

    /**
     * Method to return table metadata from db.
     *
     * @param entityManager entityManager
     * @param tableName tableName
     * @return metadata map
     */
    public Map<String, String> getColumnDataType(EntityManager entityManager, String tableName) {
        String queryDataType =
            "SELECT column_name, udt_name FROM information_schema.columns WHERE table_name = :tableName";
        Query query = entityManager.createNativeQuery(queryDataType);
        query.setParameter(TABLE_NAME, tableName);
        List<Object[]> resultSet = query.getResultList();
        return resultSet.stream().collect(Collectors.toMap(
            array -> (String) array[0],
            array -> (String) array[1]));
    }

}
