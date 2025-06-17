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

package org.eclipse.ecsp.uidam.usermanagement.validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.uidam.usermanagement.user.request.dto.UserDtoBase;
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ResponseMessage;

import static org.eclipse.ecsp.uidam.usermanagement.constants.ApiConstants.NULL_AND_EMPTY_USER_PASSWORD;

/**
 * Hibernate constraint validator for read only fields.
 */
public class UserPasswordValidator implements ConstraintValidator<UserPasswordValidation, UserDtoBase> {

    @Override
    public void initialize(UserPasswordValidation constraintAnnotation) {
        // No initialization needed as message is not used
    }

    @Override
    public boolean isValid(UserDtoBase userDto, ConstraintValidatorContext ctx) {
        if (Boolean.FALSE.equals(userDto.getIsExternalUser()) && StringUtils.isEmpty(userDto.getPassword())) {

            ctx.disableDefaultConstraintViolation();
            ConstraintValidatorContext.ConstraintViolationBuilder builder = ctx
                    .buildConstraintViolationWithTemplate(NULL_AND_EMPTY_USER_PASSWORD);
            ResponseMessage responseMessage = new ResponseMessage(NULL_AND_EMPTY_USER_PASSWORD);
            builder.addBeanNode().inIterable().atKey(responseMessage).addConstraintViolation();
            return false;
        }
        return true;
    }
}
