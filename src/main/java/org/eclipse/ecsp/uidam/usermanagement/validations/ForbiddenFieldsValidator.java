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
import org.eclipse.ecsp.uidam.usermanagement.user.response.dto.ResponseMessage;
import org.eclipse.ecsp.uidam.usermanagement.utilities.PatchMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hibernate constraint validator for read only fields.
 */
public class ForbiddenFieldsValidator implements ConstraintValidator<ForbiddenFields, PatchMap> {

    private String[] forbiddenFields;

    private String message;

    public ForbiddenFieldsValidator() {
        //empty constructor for sonar vulnerability
    }

    @Override
    public void initialize(ForbiddenFields parameters) {
        this.forbiddenFields = parameters.forbiddenFields();
        this.message = parameters.message();
    }

    /**
     * For each key check if it contained in the forbidden fields collection and if so,
     * build constraint violation with a bean node, that as a key which is {@link ResponseMessage response message}.
     *
     * @param map - A map to validate
     */
    @Override
    public boolean isValid(PatchMap map, ConstraintValidatorContext constraintValidatorContext) {
        boolean isValid = true;
        constraintValidatorContext.disableDefaultConstraintViolation();
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
            constraintValidatorContext.buildConstraintViolationWithTemplate(message);

        ResponseMessage responseMessage = new ResponseMessage(message);
        List<String> forbiddenFieldsList = Arrays.asList(forbiddenFields);
        List<Object> collectedForbiddenFields = new ArrayList<>();

        // Collect al forbidden fields which are present in the object-map parameter
        for (String currentField : map.keySet()) {
            if (forbiddenFieldsList.contains(currentField)) {
                collectedForbiddenFields.add(currentField);
                isValid = false;
            }
        }

        // When object parameter is not valid, it means that it contains read only fields, therefore create
        // a constraint violation which have a bean node with relevant response message
        if (!isValid) {
            responseMessage.setParameters(collectedForbiddenFields);
            builder.addBeanNode().inIterable().atKey(responseMessage).addConstraintViolation();
        }
        return isValid;
    }
}
