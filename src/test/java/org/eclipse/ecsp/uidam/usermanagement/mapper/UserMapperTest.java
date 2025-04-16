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

package org.eclipse.ecsp.uidam.usermanagement.mapper;

import org.eclipse.ecsp.uidam.usermanagement.entity.UserAddressEntity;
import org.eclipse.ecsp.uidam.usermanagement.entity.UserEntity;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoV1;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.UserResponseBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    /**
     * junit initial setup.
     */
    @BeforeEach
    void setUp() {
    }

    /**
     * junit after each.
     */
    @AfterEach
    void tearDown() {
    }

    @Test
    void mapToUser() {
        UserDtoV1 userDto = new UserDtoV1();
        userDto.setCity("Delhi");
        userDto.setState("Delhi");
        UserEntity userEntity = UserMapper.USER_MAPPER.mapToUser(userDto);
        assertEquals("Delhi", userEntity.getUserAddresses().get(0).getCity());
        assertEquals("Delhi", userEntity.getUserAddresses().get(0).getState());
    }

    @Test
    void mapToUserDto() {
        UserEntity userEntity = new UserEntity();
        UserAddressEntity userAddressEntity = new UserAddressEntity();
        userAddressEntity.setState("Delhi");
        userEntity.setUserAddresses(List.of(userAddressEntity));
        UserResponseBase userDto = UserMapper.USER_MAPPER.mapToUserResponse(userEntity);
        assertEquals("Delhi", userDto.getState());
    }
}