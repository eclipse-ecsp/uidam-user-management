/********************************************************************************
 
 * Copyright © 2023-24 Harman International 
 
 * Licensed under the Apache License, Version 2.0 (the "License");
  
 * you may not use this file except in compliance with the License.
  
 * You may obtain a copy of the License at
 
 *  http://www.apache.org/licenses/LICENSE-2.0
      
 * Unless required by applicable law or agreed to in writing, software
  
 * distributed under the License is distributed on an "AS IS" BASIS,
  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  
 * See the License for the specific language governing permissions and
  
 * limitations under the License.
 
 * SPDX-License-Identifier: Apache-2.0
  
 ********************************************************************************/

package org.eclipse.ecsp.uidam.accountmanagement.enums;

/**
 * The AccountStatus enum represents the different status of an account. It can
 * be PENDING, ACTIVE, SUSPENDED, BLOCKED or DEACTIVATED.
 */
public enum AccountStatus {
    PENDING, ACTIVE, SUSPENDED, BLOCKED, DELETED;

    /**
     * Returns the name of the account status.
     *
     * @return the name of the account status
     */
    @Override
    public String toString() {
        switch (this) {
            case PENDING -> {
                return PENDING.name();
            }
            case ACTIVE -> {
                return ACTIVE.name();
            }
            case SUSPENDED -> {
                return SUSPENDED.name();
            }
            case BLOCKED -> {
                return BLOCKED.name();
            }
            case DELETED -> {
                return DELETED.name();
            }
            default -> throw new IllegalArgumentException();
        }
    }
}
