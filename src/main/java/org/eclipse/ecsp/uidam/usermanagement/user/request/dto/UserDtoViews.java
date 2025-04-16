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

package org.eclipse.ecsp.uidam.usermanagement.user.request.dto;

/**
 * UserDtoViews.
 */
public class UserDtoViews {

    /**
     * BaseView.
     */
    public static class BaseView {
    }

    /**
     * V1InternalView.
     */
    public static class UserDtoV1View extends BaseView {
    }

    /**
     * V2InternalView.
     */
    public static class UserDtoV2View extends UserDtoV1View {
    }

    /**
     * V1ExternalView.
     */
    public static class UserDtoV1ExternalView extends BaseView {
    }

    /**
     * V1SelfView.
     */
    public static class UserDtoV1SelfView extends BaseView {
    }

    /**
     * V1FederatedView.
     */
    public static class UserDtoV1FederatedView extends BaseView {
    }

}
