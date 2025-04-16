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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity class for scope.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "SCOPE")
@EntityListeners(AuditingEntityListener.class)
public class ScopesEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;

    @Column(name = "SCOPE_NAME", unique = true)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IS_ADMINISTRATIVE")
    private boolean administrative;

    @Column(name = "IS_PREDEFINED")
    private boolean predefined;

    @OneToMany(mappedBy = "scope", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<RoleScopeMappingEntity> mapping = new ArrayList<>();

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

}
