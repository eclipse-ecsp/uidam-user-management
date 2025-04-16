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

package org.eclipse.ecsp.uidam.usermanagement.service;

import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfilePatch;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.request.dto.CloudProfileRequest;
import org.eclipse.ecsp.uidam.usermanagement.cloudprofile.response.dto.CloudProfileResponse;
import org.eclipse.ecsp.uidam.usermanagement.exception.ResourceNotFoundException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 *  CloudProfileService methods for cloud profile crud operations.
 */
public interface CloudProfileService {

    List<CloudProfileResponse> getCloudProfile(BigInteger userId, String profileName) throws ResourceNotFoundException;

    Map<String, String> getCloudProfiles(BigInteger userId) throws ResourceNotFoundException;

    CloudProfileResponse addCloudProfile(CloudProfileRequest cloudProfileRequest) throws ResourceNotFoundException;

    CloudProfileResponse updateCloudProfile(String etagFromRequest, CloudProfileRequest cloudProfileRequest,
                                            String cloudProfileName, BigInteger id)
        throws ResourceNotFoundException;

    CloudProfileResponse editCloudProfile(String etagFromRequest, CloudProfilePatch patch,
                                          String cloudProfileName, BigInteger id)
        throws ResourceNotFoundException;

    CloudProfileResponse deleteCloudProfile(String etagFromRequest, String cloudProfileName, BigInteger id)
        throws ResourceNotFoundException;
}
