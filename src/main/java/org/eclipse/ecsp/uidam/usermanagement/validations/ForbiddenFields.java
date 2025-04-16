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

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static org.eclipse.ecsp.uidam.usermanagement.constants.LocalizationKey.FIELD_CANNOT_BE_MODIFIED;

/**
 * Custom annotation to specify fields on which updates are prohibited.
 */
@Target({TYPE_PARAMETER, TYPE_USE, METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
    validatedBy = {ForbiddenFieldsValidator.class}
)
public @interface ForbiddenFields {

    /**
     * Error message to be thrown if user enters forbidden fields.
     *
     * @return error message
     */
    String message() default FIELD_CANNOT_BE_MODIFIED;

    /**
     * Forbidden fields group.
     *
     * @return Class type array
     */
    Class<?>[] groups() default {};

    /**
     * Returns request payload.
     *
     * @return request payload
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Array of forbidden fields.
     *
     * @return Array of forbidden fields
     */
    String[] forbiddenFields();

    /**
     * List of ForbiddenFields.
     *
     */
    @Target({TYPE_PARAMETER, TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        /**
         * List of ForbiddenField values.
         *
         */
        ForbiddenFields[] value();
    }
}
