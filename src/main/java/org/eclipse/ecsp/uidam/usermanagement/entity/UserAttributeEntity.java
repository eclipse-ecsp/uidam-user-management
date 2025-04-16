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

package org.eclipse.ecsp.uidam.usermanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.math.BigInteger;
import java.sql.Timestamp;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.SYSTEM;

/**
 * User Attribute Entity class.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_attributes")
public class UserAttributeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private Boolean mandatory;
    @Column(name = "is_unique", nullable = false)
    private Boolean isUnique;
    @Column(name = "read_only", nullable = false)
    private Boolean readOnly;
    @Column(name = "searchable", nullable = false)
    private Boolean searchable;
    @Column(name = "dynamic_attribute", nullable = false)
    private Boolean dynamicAttribute;
    @Column(nullable = false)
    private String types;
    private String regex;
    @Column(name = "created_by")
    private String createdBy = SYSTEM;
    @Column(name = "created_date")
    private Timestamp createdDate;
    @Column(name = "updated_by")
    private String updatedBy;
    @Column(name = "updated_Date")
    private Timestamp updatedDate;
}
