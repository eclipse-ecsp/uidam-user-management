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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.math.BigInteger;
import java.time.Instant;

/**
 * UserEvents entity for login scenario.
 */
@Entity
@Table(name = "user_event_details")
@Getter
@Setter
@ToString
public class UserEvents {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "NUMERIC(38) DEFAULT get_uuid()")
    private BigInteger id;

    @Column(name = "USER_ID")
    private BigInteger userId;

    @Column(name = "EVENT_TYPE")
    private String eventType;

    @Column(name = "EVENT_STATUS")
    private String eventStatus;

    @Column(name = "EVENT_MESSAGE")
    private String eventMessage;

    @Column(name = "EVENT_GENERATED_AT")
    private Instant eventGeneratedAt;

    @PrePersist
    private void prePersist() {
        eventGeneratedAt = Instant.now();
    }

}
