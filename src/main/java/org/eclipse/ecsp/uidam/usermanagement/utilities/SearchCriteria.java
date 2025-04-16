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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.enums.SearchType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pojo to define SearchCriteria for entities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteria implements Serializable {
    @Serial
    private static final long serialVersionUID = 2405172041950251807L;
    private Object key;
    private Set<?> value;
    private SearchType searchType;
    private Boolean ignoreCase;

    /**
     * Param Constructor to initialize search criteria.
     *
     * @param key key
     * @param searchType searchType
     * @param ignoreCase ignoreCase
     */
    public SearchCriteria(Object key, SearchType searchType, boolean ignoreCase) {
        this.key = key;
        this.searchType = searchType;
        this.ignoreCase = ignoreCase;
    }

    /**
     * Method to filter input set with non-blank and not-null values.
     *
     * @param value input set of search values
     */
    public void setStringValue(Set<String> value) {
        this.value = value.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    /**
     * Method to set value for which field needs to be filtered.
     *
     * @param value set of values to be updated.
     */
    public void setValue(Set<?> value) {
        this.value = value.stream().filter(searchBy -> searchBy instanceof String
            ? StringUtils.isNotBlank(searchBy.toString()) :
            searchBy != null).collect(Collectors.toSet());
    }

    /**
     * Enum to separate user fields on the basis of address and main user table.
     */
    public enum RootParam {
        USER_ROOT,
        USER_ADDRESS_ROOT,
        USER_ACCOUNT_ROLE_ROOT;
    }
}
