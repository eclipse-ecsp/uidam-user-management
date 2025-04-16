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
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ACCOUNT_ROLE_MAPPING_TABLE_NAME;
/**
 * Entity class for user-account-role scope mapping.
 */

@NoArgsConstructor
@Data
@Entity
@Table(name = USER_ACCOUNT_ROLE_MAPPING_TABLE_NAME)
@EntityListeners(AuditingEntityListener.class)
public class UserAccountRoleMappingEntity implements Serializable {       
    
    private static final long serialVersionUID = 9199291341766332556L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "NUMERIC(38) DEFAULT get_uuid()", nullable = false, updatable = false)
    private BigInteger id;
   
    @Column(name = "role_id", nullable = false, updatable = false)
    private BigInteger roleId;

    @JoinColumn(name = "user_id")
    private BigInteger userId;
    
    @Column(name = "account_id", nullable = false, updatable = false)
    private BigInteger accountId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "create_date")
    @CreatedDate
    private Date createDate;

    @Column(name = "updated_by")
    private String updateBy;

    @Column(name = "update_date")
    @LastModifiedDate
    private Date updateDate;

    /**
     * Constructor using role, user and account.
     *
     * @param roleId roleId
     * @param user user
     * @param accountId accountId
     * @param createdBy created by user
     *
     */
    public UserAccountRoleMappingEntity(BigInteger roleId, BigInteger user, BigInteger accountId, String createdBy) {
        super();
        this.roleId = roleId;
        this.userId = user;
        this.accountId = accountId;
        this.createdBy = createdBy;
    }
}
