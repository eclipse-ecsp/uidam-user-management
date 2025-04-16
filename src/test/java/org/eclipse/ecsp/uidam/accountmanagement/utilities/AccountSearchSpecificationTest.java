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

package org.eclipse.ecsp.uidam.accountmanagement.utilities;

import io.prometheus.client.CollectorRegistry;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/*
 * Test cases for AccountSearchSpecification
 */
class AccountSearchSpecificationTest {

    Root<AccountEntity> accountEntityRoot;
    CriteriaQuery criteriaQuery;
    CriteriaBuilder builder;
    Path path;
    Predicate predicate;
    Expression expression;

    static final int TWO = 2;

    @BeforeEach
    void setUp() {
        CollectorRegistry.defaultRegistry.clear();
        accountEntityRoot = Mockito.mock(Root.class);
        criteriaQuery = Mockito.mock(CriteriaQuery.class);
        builder = Mockito.mock(CriteriaBuilder.class);
        path = Mockito.mock(Path.class);
        expression = Mockito.mock(Expression.class);
        predicate = Mockito.mock(Predicate.class);
    }

    @Test
    void handleSearchTypeTest() {
        assertEquals("This String", AccountSearchSpecification.handleSearchMode("This String", null));
    }

    @Test
    void toPredicateTest() {
        Object key = "accountName";
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("john"), null, false);
        AccountSearchSpecification accountSpec = new AccountSearchSpecification(searchCriteria);
        Mockito.when(accountEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);

        Mockito.when(builder.upper(path)).thenReturn(expression);
        Mockito.when(builder.like(expression, expression)).thenReturn(predicate);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);

        Predicate actualPredicate = accountSpec.toPredicate(accountEntityRoot, criteriaQuery, builder);
        verify(accountEntityRoot, times(TWO)).get(key.toString());
        assertEquals(predicate, actualPredicate);
    }

    @Test
    void updateCriteriaValueTestIgnoreCaseTrue() {
        Object key = "accountName";
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("Account1"), null, true);
        assertEquals("ACCOUNT1", AccountSearchSpecification.updateCriteriaValue(searchCriteria.getValue()
            .iterator().next().toString(), searchCriteria));
    }

    @Test
    void updateCriteriaValueTestIgnoreCaseFalse() {
        Object key = "accountName";
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("Account1"), null, false);
        assertEquals("Account1", AccountSearchSpecification.updateCriteriaValue(searchCriteria.getValue()
            .iterator().next().toString(), searchCriteria));
    }

    @Test
    void updateCriteriaValueTestOr() {
        Object key = "accountName";
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("Account1", "Account2"), null, false);
        String updatedCriteria = searchCriteria.getValue().iterator().next().toString();
        assertEquals(updatedCriteria, AccountSearchSpecification.updateCriteriaValue(updatedCriteria, searchCriteria));
    }

    @Test
    void toPredicateTestUuid() {
        Object key = "id";
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of(UUID.randomUUID()), null, false);
        AccountSearchSpecification accountSpec = new AccountSearchSpecification(searchCriteria);
        Mockito.when(accountEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(UUID.class);

        Mockito.when(builder.equal(expression, expression)).thenReturn(predicate);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);

        Predicate actualPredicate = accountSpec.toPredicate(accountEntityRoot, criteriaQuery, builder);
        verify(accountEntityRoot, times(TWO)).get(key.toString());
        assertEquals(predicate, actualPredicate);
    }

    @Test
    void buildValueBySearchTypeTest() {
        assertEquals("%account", AccountSearchSpecification.buildValueBySearchType("account", SearchType.SUFFIX));
        assertEquals("account%", AccountSearchSpecification.buildValueBySearchType("account", SearchType.PREFIX));
        assertEquals("account", AccountSearchSpecification.buildValueBySearchType("account", SearchType.EQUAL));
    }
}