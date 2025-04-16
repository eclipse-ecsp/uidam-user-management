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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigInteger;
import java.time.Instant;

/**
 * Entity class for client details - client_details.
 */
@Entity
@Table(name = "client_details")
@Getter
@Setter
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;

    @Column(name = "CLIENT_ID")
    private String clientId;

    @Column(name = "TENANT_ID")
    private String tenantId;

    @Column(name = "CLIENT_SECRET")
    private String secret;

    @Column(name = "CLIENT_NAME")
    private String clientName;

    @Column(name = "CLIENT_AUTHENTICATION_METHODS")
    private String authenticationMethods;

    @Column(name = "AUTHORIZED_GRANT_TYPES")
    private String grantTypes;

    @Column(name = "REDIRECT_URI")
    private String redirectUrls;

    @Column(name = "SCOPES")
    private String scopes;

    @Column(name = "ACCESS_TOKEN_VALIDITY")
    private long accessTokenValidity;

    @Column(name = "REFRESH_TOKEN_VALIDITY")
    private long refreshTokenValidity;

    @Column(name = "AUTHORIZATION_CODE_VALIDITY")
    private long authorizationCodeValidity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ADDITIONAL_INFORMATION", columnDefinition = "jsonb")
    private String additionalInformation;

    @Column(name = "REQUIRE_AUTHORIZATION_CONSENT ")
    private boolean requiredAuthorizationConsent;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "APPROVED_BY")
    private String approvedBy;

    @Column(name = "APPROVED_DATE")
    private Instant approvedDate;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "UPDATED_BY")
    private String updatedBy;

    @Column(name = "CREATE_DATE")
    private Instant createDate;

    @Column(name = "CLIENT_SECRET_EXPIRES_AT")
    private Instant clientSecretExpireAt;

    @Column(name = "UPDATE_DATE")
    private Instant updateDate;

    @PrePersist
    private void prePersist() {
        createDate = Instant.now();
        approvedDate = Instant.now();
    }

    @PreUpdate
    private void setUpdatedAt() {
        updateDate = Instant.now();
    }

    @Override
    public String toString() {
        return "ClientEntity [id=" + id + ", clientId=" + clientId + ", tenantId=" + tenantId + ", clientName="
            + clientName + ", authenticationMethods=" + authenticationMethods + ", grantTypes=" + grantTypes
            + ", redirectUrls=" + redirectUrls + ", scopes=" + scopes + ", accessTokenValidity="
            + accessTokenValidity + ", refreshTokenValidity=" + refreshTokenValidity
            + ", authorizationCodeValidity=" + authorizationCodeValidity + ", additionalInformation="
            + additionalInformation + ", requiredAuthorizationConsent=" + requiredAuthorizationConsent + ", status="
            + status + ", approvedBy=" + approvedBy + ", approvedDate=" + approvedDate + ", createdBy=" + createdBy
            + ", updatedBy=" + updatedBy + ", createDate=" + createDate + ", clientSecretExpireAt="
            + clientSecretExpireAt + ", updateDate=" + updateDate + "]";
    }

}
