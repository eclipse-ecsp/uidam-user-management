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
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.eclipse.ecsp.uidam.usermanagement.entity.ClientEntity;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import org.springframework.data.jpa.domain.Specification;
import java.io.Serial;
import java.util.List;
import java.util.Locale;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.CONTAINS;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.PREFIX;
import static org.eclipse.ecsp.uidam.usermanagement.enums.SearchType.SUFFIX;

/**
 * Specification building the search predicates for {@link ClientEntity}.
 */
@AllArgsConstructor
@NoArgsConstructor
public class ClientSearchSpecification implements Specification<ClientEntity> {

    @Serial
    private static final long serialVersionUID = 8264398160145279234L;

    private static final char ESCAPE_CHARACTER = '\\';
    private static final String WILDCARD = "%";

    private SearchCriteria criteria;

    /**
     * Build the predicate matching any of the values configured for the search criteria field.
     *
     * @param root the client entity root
     * @param query the criteria query
     * @param builder the criteria builder
     * @return predicate combining all criteria values with OR
     */
    @Override
    public Predicate toPredicate(Root<ClientEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Path<String> path = root.get((String) criteria.getKey());
        boolean ignoreCase = Boolean.TRUE.equals(criteria.getIgnoreCase());
        Expression<String> searchPath = ignoreCase ? builder.upper(path) : path;
        List<Predicate> predicates = criteria.getValue().stream()
            .map(value -> builder.like(searchPath, buildSearchValue(String.valueOf(value), ignoreCase),
                ESCAPE_CHARACTER))
            .toList();
        return builder.or(predicates.toArray(new Predicate[0]));
    }

    private String buildSearchValue(String value, boolean ignoreCase) {
        String escapedValue = escapeWildcards(value);
        String searchValue = ignoreCase ? escapedValue.toUpperCase(Locale.ROOT) : escapedValue;
        SearchType searchType = criteria.getSearchType();
        StringBuilder searchValueBuilder = new StringBuilder(searchValue);
        if (SUFFIX.equals(searchType) || CONTAINS.equals(searchType)) {
            searchValueBuilder.insert(0, WILDCARD);
        }
        if (PREFIX.equals(searchType) || CONTAINS.equals(searchType)) {
            searchValueBuilder.append(WILDCARD);
        }
        return searchValueBuilder.toString();
    }

    /**
     * Escape the wildcards so that user supplied values are matched literally.
     */
    private static String escapeWildcards(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
