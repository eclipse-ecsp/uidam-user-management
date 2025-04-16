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

package org.eclipse.ecsp.uidam.accountmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.ecsp.uidam.accountmanagement.enums.AccountStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import static org.eclipse.ecsp.uidam.accountmanagement.constants.AccountApiConstants.ACCOUNT_ENTITY_TABLE_NAME;

/**
 * Represents an account entity in the system.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = ACCOUNT_ENTITY_TABLE_NAME)
public class AccountEntity implements Serializable {

    private static final long serialVersionUID = 8329291341766332556L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "NUMERIC(38) DEFAULT get_uuid()", nullable = false, updatable = false)
    private BigInteger id;

    @Column(name = "account_name", unique = true, nullable = false, updatable = false)
    private String accountName;

    @Column(name = "parent_id")
    private BigInteger parentId;

    @Column(name = "default_roles")
    private Set<BigInteger> defaultRoles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "create_date")
    @CreatedDate
    private Timestamp createDate;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "update_date")
    private Timestamp updateDate;

}
