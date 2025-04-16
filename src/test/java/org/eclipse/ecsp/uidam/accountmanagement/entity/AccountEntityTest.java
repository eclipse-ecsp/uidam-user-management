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

package org.eclipse.ecsp.uidam.accountmanagement.entity;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Set;

import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.PARENT_ID_VALUE;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_1;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.ROLE_ID_2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AccountEntityTest {


    @Test
    void testAccountEntity() {
        // Create a new AccountEntity object
        AccountEntity account = new AccountEntity();

        // Set values for the account
        account.setAccountName("Test Account");
        account.setParentId(PARENT_ID_VALUE);
        account.setDefaultRoles(Set.of(ROLE_ID_1, ROLE_ID_2));
        account.setCreatedBy("John Doe");
        account.setCreateDate(new Timestamp(System.currentTimeMillis()));
        account.setUpdatedBy("Jane Smith");
        account.setUpdateDate(new Timestamp(System.currentTimeMillis()));

        // Verify the values are set correctly
        assertEquals("Test Account", account.getAccountName());
        assertNotNull(account.getParentId());
        assertEquals(Set.of(ROLE_ID_1, ROLE_ID_2), account.getDefaultRoles());
        assertEquals("John Doe", account.getCreatedBy());
        assertNotNull(account.getCreateDate());
        assertEquals("Jane Smith", account.getUpdatedBy());
        assertNotNull(account.getUpdateDate());
    }
}
