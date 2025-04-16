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
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.CONTAINS;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.PREFIX;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.SUFFIX;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.ACCOUNTIDS;
import static org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UsersGetFilterBase.UserGetFilterEnum.ROLEIDS;

/**
 * UserSpecification defines specs for userEntity.
 */
@AllArgsConstructor
@NoArgsConstructor
public class UserSpecification implements Specification<UserEntity> {
    private static final long serialVersionUID = 1L;
    private SearchCriteria criteria;

    /**
     * Method to fetch details for non deleted users.
     *
     * @return query specification for non deleted users.
     */
    public static Specification<UserEntity> getExistingUserSpecification() {
        return (root, query, builder) -> builder.notEqual(root.get("status"), UserStatus.DELETED);
    }

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
     * Method to filter out user based on custom query and values.
     *
     * @param root must not be {@literal null}.
     * @param query must not be {@literal null}.
     * @param builder must not be {@literal null}.
     * @return predicate.
     */
    @Override
    public Predicate toPredicate(Root<UserEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Predicate[] predicates = null;
        List<Predicate> predicateList = null;
        String[] keyParams = ((String) criteria.getKey()).split("\\.");
        SearchCriteria.RootParam rootParam = SearchCriteria.RootParam.valueOf(keyParams[0]);
        String field = keyParams[1];
        From<?, ?> from = switch (rootParam) {
            case USER_ROOT -> root;
            case USER_ADDRESS_ROOT -> root.join("userAddresses");
            case USER_ACCOUNT_ROLE_ROOT -> root.join("accountRoleMapping");
        };
        if (from.get(field).getJavaType() == String.class) {
            predicateList = criteria.getValue().stream()
                .map(value -> builder.like(
                    Boolean.TRUE.equals(criteria.getIgnoreCase())
                        ? builder.upper(from.<String>get(field))
                        : from.<String>get(field), updateCriteriaValue(
                            (String) value, criteria)
                )).toList();
            predicates = new Predicate[predicateList.size()];
            predicateList.toArray(predicates);
        } else if (field.equals(ROLEIDS.getField()) || field.equals(ACCOUNTIDS.getField())) {
            return from.<String>get(field).in(criteria.getValue());
        } else {
            predicateList = criteria.getValue().stream()
                .map(value -> builder.equal(from.get(field),
                    value)).toList();
            predicates = new Predicate[predicateList.size()];
            predicateList.toArray(predicates);
        }
        return builder.or(predicates);
    }
}
