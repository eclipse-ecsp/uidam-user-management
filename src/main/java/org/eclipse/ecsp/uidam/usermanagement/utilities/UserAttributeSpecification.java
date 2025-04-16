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
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserAttributeValueEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.CONTAINS;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.PREFIX;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.SUFFIX;

/**
 * UserAttributeSpecification class to define specification methods for UserAttributeValueEntity.
 */
@AllArgsConstructor
@NoArgsConstructor
public class UserAttributeSpecification implements Specification<UserAttributeValueEntity> {
    private static final long serialVersionUID = 1L;
    private SearchCriteria criteria;

    /**
     * Method to update string as per criteria value.
     *
     * @param str input string value to be searched.
     * @param searchCriteria searchCriteria.
     * @return updated string as per criteria value.
     */
    private static String updateCriteriaValue(String str, SearchCriteria searchCriteria) {
        return handleSearchType(handleCaseSensitive(str, searchCriteria.getIgnoreCase()),
            searchCriteria.getSearchType());
    }

    /**
     * Method to update string value to uppercase if ignore-case is true.
     *
     * @param str input string.
     * @param ignoreCase flag to determine if ignore case is true or false.
     * @return updated string.
     */
    private static String handleCaseSensitive(String str, boolean ignoreCase) {
        if (!ignoreCase) {
            return str;
        }

        return str.toUpperCase();
    }

    /**
     * Method to update string with respect to search type.
     *
     * @param str input string.
     * @param searchType enum prefix suffix etc.
     * @return updated string.
     */
    private static String handleSearchType(String str, SearchType searchType) {
        if (searchType == null) {
            return str;
        }

        return buildValueBySearchType(str, searchType);
    }

    /**
     * Method to update string with respect to search type.
     *
     * @param str input string.
     * @param searchType enum prefix suffix etc.
     * @return updated string.
     */
    private static String buildValueBySearchType(String str, SearchType searchType) {
        StringBuilder builtBySearchTypeValue = new StringBuilder(str);
        if (SUFFIX.equals(searchType) || CONTAINS.equals(searchType)) {
            builtBySearchTypeValue.insert(0, "%");
        }
        if (PREFIX.equals(searchType) || CONTAINS.equals(searchType)) {
            builtBySearchTypeValue.append("%");
        }
        return builtBySearchTypeValue.toString();
    }

    /**
     * Method to filter out user attribute values based on custom query and values.
     *
     * @param root must not be {@literal null}.
     * @param query must not be {@literal null}.
     * @param builder must not be {@literal null}.
     * @return predicate.
     */
    @Override
    public Predicate toPredicate(
        Root<UserAttributeValueEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Predicate[] predicatesValues = null;
        List<Predicate> predicateList = null;
        predicateList = criteria.getValue().stream()
            .map(value -> builder.like(
                Boolean.TRUE.equals(criteria.getIgnoreCase())
                    ? builder.upper(root.<String>get("value")) :
                    root.<String>get("value"), updateCriteriaValue(
                        (String) value, criteria)
            )).toList();
        predicatesValues = new Predicate[predicateList.size()];
        predicateList.toArray(predicatesValues);
        Predicate predicateValue = builder.or(predicatesValues);
        Integer field = (Integer) criteria.getKey();
        Predicate predicateField = builder.equal(root.get("attributeId"), field);
        return builder.and(predicateField, predicateValue);
    }
}
