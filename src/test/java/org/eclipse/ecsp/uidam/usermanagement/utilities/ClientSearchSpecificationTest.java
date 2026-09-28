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

package org.eclipse.ecsp.uidam.usermanagement.utilities;

import io.prometheus.client.CollectorRegistry;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.eclipse.ecsp.uidam.usermanagement.entity.ClientEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test cases for ClientSearchSpecification.
 */
class ClientSearchSpecificationTest {

    private static final int TWO = 2;
    private static final String CLIENT_NAME_FIELD = "clientName";

    private Root<ClientEntity> clientEntityRoot;
    private CriteriaQuery<?> criteriaQuery;
    private CriteriaBuilder builder;
    private Path path;
    private Expression expression;
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        CollectorRegistry.defaultRegistry.clear();
        clientEntityRoot = Mockito.mock(Root.class);
        criteriaQuery = Mockito.mock(CriteriaQuery.class);
        builder = Mockito.mock(CriteriaBuilder.class);
        path = Mockito.mock(Path.class);
        expression = Mockito.mock(Expression.class);
        predicate = Mockito.mock(Predicate.class);
        Mockito.when(clientEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(builder.upper(path)).thenReturn(expression);
        Mockito.when(builder.like(any(Expression.class), anyString(), anyChar())).thenReturn(predicate);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
    }

    @Test
    void equalSearchTypeMatchesTheLiteralValue() {
        assertEquals(predicate, toPredicate(Set.of("uidam-portal"), SearchType.EQUAL, false));
        verify(builder, never()).upper(path);
        assertEquals(List.of("uidam-portal"), capturedSearchValues());
    }

    @Test
    void nullSearchTypeMatchesTheLiteralValue() {
        assertEquals(predicate, toPredicate(Set.of("uidam-portal"), null, false));
        assertEquals(List.of("uidam-portal"), capturedSearchValues());
    }

    @Test
    void prefixSearchTypeAppendsWildcard() {
        toPredicate(Set.of("uidam"), SearchType.PREFIX, false);
        assertEquals(List.of("uidam%"), capturedSearchValues());
    }

    @Test
    void suffixSearchTypePrependsWildcard() {
        toPredicate(Set.of("portal"), SearchType.SUFFIX, false);
        assertEquals(List.of("%portal"), capturedSearchValues());
    }

    @Test
    void containsSearchTypeSurroundsValueWithWildcards() {
        toPredicate(Set.of("dam"), SearchType.CONTAINS, false);
        assertEquals(List.of("%dam%"), capturedSearchValues());
    }

    @Test
    void ignoreCaseUppercasesBothValueAndColumn() {
        toPredicate(Set.of("Uidam"), SearchType.CONTAINS, true);
        verify(builder).upper(path);
        assertEquals(List.of("%UIDAM%"), capturedSearchValues());
    }

    @Test
    void wildcardCharactersInTheValueAreEscaped() {
        toPredicate(Set.of("a%_b\\c"), SearchType.EQUAL, false);
        assertEquals(List.of("a\\%\\_b\\\\c"), capturedSearchValues());
    }

    @Test
    void allCriteriaValuesAreCombinedWithOr() {
        toPredicate(Set.of("first", "second"), SearchType.EQUAL, false);
        verify(builder, times(TWO)).like(any(Expression.class), anyString(), anyChar());
        verify(builder).or(any(Predicate[].class));
    }

    private Predicate toPredicate(Set<String> values, SearchType searchType, boolean ignoreCase) {
        SearchCriteria searchCriteria = new SearchCriteria(CLIENT_NAME_FIELD, values, searchType, ignoreCase);
        return new ClientSearchSpecification(searchCriteria).toPredicate(clientEntityRoot, criteriaQuery, builder);
    }

    private List<String> capturedSearchValues() {
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(builder).like(any(Expression.class), valueCaptor.capture(), anyChar());
        return valueCaptor.getAllValues();
    }
}
