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
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.Arrays;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UserManagementDao.class})
@MockBean(JpaMetamodelMappingContext.class)
class UserManagementDaoTest {

    @Autowired
    UserManagementDao userManagementDao;


    @Test
    void getColumnDataType() {
        EntityManager entityManager = Mockito.mock(EntityManager.class);
        Query query = Mockito.mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(
            Arrays.asList(new String[] {"tenant_id", "uuid"}, new String[] {"id", "uuid"}));
        Map<String, String> result =
            userManagementDao.getColumnDataType(entityManager, ApiConstants.USER_ADDRESS_ENTITY_TABLE_NAME);
        assertEquals("uuid", result.get("tenant_id"));
        assertEquals("uuid", result.get("id"));
    }
}
