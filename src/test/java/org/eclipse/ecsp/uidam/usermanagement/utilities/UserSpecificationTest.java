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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.ACCOUNTIDS;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.CITIES;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.ROLEIDS;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.Constants.FIRST_NAME;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria.RootParam.USER_ACCOUNT_ROLE_ROOT;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria.RootParam.USER_ADDRESS_ROOT;
import static org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria.RootParam.USER_ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;

class UserSpecificationTest {
    Root<UserEntity> userEntityRoot;
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
    void toPredicateSearchTypeNullAndIgnoreCaseFalse() {
        Object key = USER_ROOT + "." + FIRST_NAME;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("john"), null, false);
        UserSpecification userSpecification = new UserSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Predicate predicate = userSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void toPredicateSearchTypeContainsAndIgnoreCaseTrue() {
        Object key = USER_ROOT + "." + FIRST_NAME;
        SearchType searchType = SearchType.CONTAINS;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("john"), searchType, true);
        UserSpecification userSpecification = new UserSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Predicate predicate = userSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void toPredicateSearchTypePrefixAndIgnoreCaseTrue() {
        Object key = USER_ROOT + "." + FIRST_NAME;
        SearchType searchType = SearchType.PREFIX;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("john"), searchType, true);
        UserSpecification userSpecification = new UserSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Predicate predicate = userSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void toPredicateSearchTypeSuffixAndIgnoreCaseTrue() {
        Object key = USER_ROOT + "." + FIRST_NAME;
        SearchType searchType = SearchType.SUFFIX;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("john"), searchType, true);
        UserSpecification userSpecification = new UserSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Predicate predicate = userSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void toPredicateRoleIds() {
        Object key = USER_ACCOUNT_ROLE_ROOT + "." + ROLEIDS.getField();
        SearchType searchType = SearchType.SUFFIX;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of(1L), searchType, true);
        UserSpecification userSpecification = new UserSpecification(searchCriteria);
        Join join = Mockito.mock(Join.class);
        Mockito.when(userEntityRoot.join(anyString())).thenReturn(join);
        Mockito.when(join.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(List.class);
        Mockito.when(userEntityRoot.join(anyString())).thenReturn(join);
        Mockito.when(path.in(anySet())).thenReturn(predicate);
        Predicate predicate = userSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }
    
    @Test
    void toPredicateAccountIds() {
        Object key = USER_ACCOUNT_ROLE_ROOT + "." + ACCOUNTIDS.getField();
        SearchType searchType = SearchType.SUFFIX;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of(1L), searchType, true);
        UserSpecification userSpecification = new UserSpecification(searchCriteria);
        Join join = Mockito.mock(Join.class);
        Mockito.when(userEntityRoot.join(anyString())).thenReturn(join);
        Mockito.when(join.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(List.class);
        Mockito.when(userEntityRoot.join(anyString())).thenReturn(join);
        Mockito.when(path.in(anySet())).thenReturn(predicate);
        Predicate predicate = userSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void getExistingUserSpecification() {
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(builder.notEqual(any(), any())).thenReturn(Mockito.mock(Predicate.class));
        Specification<UserEntity> userSpecification = UserSpecification.getExistingUserSpecification();
        assertEquals(true, Optional.ofNullable(userSpecification).isPresent());
    }

    @Test
    void toPredicateUserAddressCity() {
        Object key = USER_ADDRESS_ROOT + "." + CITIES.getField();
        SearchType searchType = SearchType.SUFFIX;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of("delhi"), searchType, true);
        UserSpecification userSpecification = new UserSpecification(searchCriteria);
        Join join = Mockito.mock(Join.class);
        Mockito.when(userEntityRoot.join(anyString())).thenReturn(join);
        Mockito.when(join.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(String.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Predicate predicate = userSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }

    @Test
    void toPredicateUserRootNonString() {
        Object key = USER_ROOT + "." + "pwdRequireChange";
        SearchType searchType = SearchType.SUFFIX;
        SearchCriteria searchCriteria = new SearchCriteria(key, Set.of(false), searchType, true);
        UserSpecification userSpecification = new UserSpecification(searchCriteria);
        Mockito.when(userEntityRoot.get(anyString())).thenReturn(path);
        Mockito.when(path.getJavaType()).thenReturn(Boolean.class);
        Mockito.when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        Predicate predicate = userSpecification.toPredicate(userEntityRoot, criteriaQuery, builder);
        assertEquals(true, Optional.ofNullable(predicate).isPresent());
    }
}