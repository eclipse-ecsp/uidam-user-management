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
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

/**
 * Entity class for role scope mapping.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ROLE_SCOPE_MAPPING")
@EntityListeners(AuditingEntityListener.class)
public class RoleScopeMappingEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;

    @ManyToOne
    @JoinColumn(name = "ROLE_ID")
    private RolesEntity role;

    @ManyToOne
    @JoinColumn(name = "SCOPE_ID")
    private ScopesEntity scope;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATE_DATE")
    @CreatedDate
    private Date createDate;

    @Column(name = "UPDATED_BY")
    private String updateBy;

    @Column(name = "UPDATE_DATE")
    @LastModifiedDate
    private Date updateDate;

    /**
     * Constructor for role scope mapping.
     *
     * @param role - role entity
     * @param scope - scope entity
     * @param createdBy - created by 
     * @param updateBy - updated by
     */
    public RoleScopeMappingEntity(RolesEntity role, ScopesEntity scope, String createdBy, String updateBy) {
        this.role = role;
        this.scope = scope;
        this.createdBy = createdBy;
        this.updateBy = updateBy;
    }
}
