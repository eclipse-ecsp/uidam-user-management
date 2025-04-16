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

import io.hypersistence.utils.hibernate.type.json.JsonType;
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
import org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants;
import org.hibernate.annotations.Type;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to specify entity of CloudProfile.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cloud_profile")
public class CloudProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, unique = true, columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;

    @Column(nullable = false)
    private BigInteger userId;

    @Column(length = 100, nullable = false)
    private String cloudProfileName;

    @Type(value = JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> cloudProfileData = new HashMap<>();

    @Column(length = 100, nullable = false, unique = true)
    private String cloudProfileBusinessKey;

    @Column(nullable = true)
    private String status = ApiConstants.CloudProfileApiConstants.ACTIVE;
}
