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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.eclipse.ecsp.uidam.accountmanagement.entity.AccountEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.utilities.SearchCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serial;
import java.util.List;

import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ESCAPE_CHARACTER;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.CONTAINS;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.PREFIX;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.SUFFIX;

/**
 * Class for building AccountSearchSpecification.
 */
@AllArgsConstructor
@NoArgsConstructor
public class AccountSearchSpecification implements Specification<AccountEntity> {

    @Serial
    private static final long serialVersionUID = 3369474966352366974L;

    private SearchCriteria criteria;
    
    /**
     * Creates the predicates for the sql query construction using the search fields provided.
     *
     * @param root The root entity
     * @param query query
     * @param builder The criteria builder
     * @return The predicate built     
     */
    @Override
    public Predicate toPredicate(Root<AccountEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Predicate[] predicates = null;
        List<Predicate> predicateList = null;
        String field = (String) criteria.getKey();
        if (root.get(field).getJavaType() == String.class) {
            predicateList = criteria.getValue().stream()
                .map(value -> builder.like(
                    Boolean.TRUE.equals(criteria.getIgnoreCase())
                        ? builder.upper(root.<String>get(field)) :
                        root.<String>get(field), updateCriteriaValue(
                        (String) value, criteria), ESCAPE_CHARACTER
                )).toList();
            predicates = new Predicate[predicateList.size()];
            predicateList.toArray(predicates);
        } else {
            predicateList = criteria.getValue().stream()
                .map(value -> builder.equal(root.get(field),
                    value)).toList();
            predicates = new Predicate[predicateList.size()];
            predicateList.toArray(predicates);
        }        
        return builder.or(predicates);
    }

    protected static String updateCriteriaValue(String str, SearchCriteria searchCriteria) {
        return handleSearchMode(handleCaseSensitive(str, searchCriteria.getIgnoreCase()),
            searchCriteria.getSearchType());
    }

    private static String handleCaseSensitive(String str, boolean ignoreCase) {
        if (!ignoreCase) {
            return str;
        }

        return str.toUpperCase();
    }

    protected static String handleSearchMode(String str, SearchType searchMode) {
        if (searchMode == null) {
            return str;
        }

        return buildValueBySearchType(str, searchMode);
    }

    protected static String buildValueBySearchType(String str, SearchType searchMode) {
        StringBuilder builtBySearchTypeValue = new StringBuilder(str);
        if (SUFFIX.equals(searchMode) || CONTAINS.equals(searchMode)) {
            builtBySearchTypeValue.insert(0, "%");
        }
        if (PREFIX.equals(searchMode) || CONTAINS.equals(searchMode)) {
            builtBySearchTypeValue.append("%");
        }
        return builtBySearchTypeValue.toString();
    }
}