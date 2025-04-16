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
import java.math.BigInteger;
import java.sql.Timestamp;
import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.EMAIL_VERIFICATION_TABLE_NAME;

/**
 * EmailVerificationEntity class to store email data in db.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = EMAIL_VERIFICATION_TABLE_NAME)
public class EmailVerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;
    @Column(name = "user_id", unique = true, nullable = false)
    private BigInteger userId;
    @Column(name = "token", nullable = false)
    private String token;
    @Column(name = "update_date")
    private Timestamp updateDate;
    @Column(name = "create_date")
    private Timestamp createDate;
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;
}
