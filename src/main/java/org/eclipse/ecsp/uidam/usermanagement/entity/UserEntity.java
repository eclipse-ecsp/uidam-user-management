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

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.uidam.usermanagement.enums.Gender;
import org.eclipse.ecsp.uidam.usermanagement.enums.UserStatus;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.USER_ENTITY_TABLE_NAME;

/**
 * User Data Entity.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = USER_ENTITY_TABLE_NAME)
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;
    @Column(name = "user_name", unique = true, nullable = false)
    private String userName;
    @Column(name = "tenant_id")
    private BigInteger tenantId;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "user_password", nullable = false)
    private String userPassword;
    @Column(name = "password_salt")
    private String passwordSalt;
    @Column(name = "pwd_require_change")
    private Boolean pwdRequireChange = Boolean.FALSE;
    @Column(nullable = false)
    private Boolean enabled = Boolean.FALSE;
    @Column(name = "is_external_user")
    private Boolean isExternalUser;
    @Column(name = "account_no_locked", nullable = false)
    private Boolean accountNoLocked = Boolean.FALSE;
    @Column(name = "account_no_expired", nullable = false)
    private Boolean accountNoExpired = Boolean.FALSE;
    @Column(name = "pwd_changedtime", nullable = false)
    private Timestamp pwdChangedtime = Timestamp.valueOf(LocalDateTime.now());
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Column(nullable = false, unique = true)
    private String email;
    private String locale;
    @Column(name = "birth_date")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate birthDate;
    @Column(name = "phone_no")
    private String phoneNumber;
    @OneToMany(mappedBy = "userEntity", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<UserAddressEntity> userAddresses;
    @Type(value = JsonType.class)
    @Column(name = "device_ids", columnDefinition = "jsonb", nullable = false)
    private Set<String> devIds = new HashSet<>();
    @Column(name = "notification_consent")
    private Boolean notificationConsent;
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    @Column(name = "identity_provider_name")
    private String identityProviderName;
    @Column(name = "created_by")
    private String createdBy = "system";
    @OneToMany(mappedBy = "userId", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAccountRoleMappingEntity> accountRoleMapping = new ArrayList<>();
    @CreatedDate
    @Column(name = "create_date")
    private Timestamp createDate;
    @Column(name = "updated_by")
    private String updatedBy;
    @LastModifiedDate
    @Column(name = "update_date")
    private Timestamp updateDate;
}
