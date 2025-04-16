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

package org.eclipse.ecsp.uidam.usermanagement.validations.password.policy;

/**
 * This is the interface to be used by custom policy implementations such as password policy
 * enforcement.
 */
public interface PasswordPolicyEnforcer {

    /**
     * This method is used to enforce the policy forcing it to apply it and validate the outcome.
     * It returns true if the policy enforcement is successful and no violations have been occurred.
     * A false return means the particular policy have been violated and no more processing needs
     * to be done.
     *
     * @param args - arguments to the policy implementer. Order is implementation dependent.
     * @return - true if policy enforcement success. false if violated.
     */
    boolean enforce(Object... args);

    /**
     * Descriptive error message about the policy violation. Implementor should give descriptive
     * message.
     *
     * @return - error string
     */
    String getErrorMessage();
}
