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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeValueEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class UserAttributeSpecificationTest {

    Root<UserAttributeValueEntity> userEntityRoot;
    CriteriaQuery criteriaQuery;
    CriteriaBuilder builder;
    Path path;
    Predicate predicate;

    @BeforeEach
    void setUp() {
        userEntityRoot = Mockito.mock(Root.class);
        criteriaQuery = Mockito.mock(CriteriaQuery.class);
        builder = Mockito.mock(CriteriaBuilder.class);
        path = Mockito.mock(Path.class);
        predicate = Mockito.mock(Predicate.class);
    }

    @Test
    void toPredicateSearchTypeSuffixAndIgnoreCaseTrue() {
        Object key = 1;
        SearchType searchType = SearchType.SUFFIX;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("harman"), searchType, true);
        UserAttributeSpecification userAttributeSpecification = new UserAttributeSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Mockito.when(builder.equal(any(Path.class), any(Integer.class))).thenReturn(predicate);
        Mockito.when(builder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        Predicate predicate = userAttributeSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void toPredicateSearchTypePrefixAndIgnoreCaseTrue() {
        Object key = 1;
        SearchType searchType = SearchType.PREFIX;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("harman"), searchType, true);
        UserAttributeSpecification userAttributeSpecification = new UserAttributeSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Mockito.when(builder.equal(any(Path.class), any(Integer.class))).thenReturn(predicate);
        Mockito.when(builder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        Predicate predicate = userAttributeSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void toPredicateSearchTypeContainsAndIgnoreCaseTrue() {
        Object key = 1;
        SearchType searchType = SearchType.CONTAINS;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("harman"), searchType, true);
        UserAttributeSpecification userAttributeSpecification = new UserAttributeSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Mockito.when(builder.equal(any(Path.class), any(Integer.class))).thenReturn(predicate);
        Mockito.when(builder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        Predicate predicate = userAttributeSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void toPredicateSearchTypeContainsAndIgnoreCaseFalse() {
        Object key = 1;
        SearchType searchType = SearchType.CONTAINS;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("harman"), searchType, false);
        UserAttributeSpecification userAttributeSpecification = new UserAttributeSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Mockito.when(builder.equal(any(Path.class), any(Integer.class))).thenReturn(predicate);
        Mockito.when(builder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        Predicate predicate = userAttributeSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void toPredicateSearchTypeNullAndIgnoreCaseFalse() {
        Object key = 1;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("harman"), null, false);
        UserAttributeSpecification userAttributeSpecification = new UserAttributeSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Mockito.when(builder.equal(any(Path.class), any(Integer.class))).thenReturn(predicate);
        Mockito.when(builder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        Predicate predicate = userAttributeSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }
}