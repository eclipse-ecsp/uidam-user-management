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

package org.eclipse.ecsp.uidam.usermanagement.user.response.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.util.CollectionUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a template to prepare messages for response.
 */
public class BaseRepresentation {
    private static final int INITIAL_ODD_NUMBER = 17;
    private static final int MULTIPLIER_ODD_NUMBER = 37;

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    protected List<ResponseMessage> messages;

    public BaseRepresentation() {
    }

    public BaseRepresentation(List<ResponseMessage> messages) {
        this.messages = messages;
    }

    /**
     * returns message.
     *
     * @return messages
     */
    public List<ResponseMessage> getMessages() {
        if (CollectionUtils.isEmpty(messages)) {
            messages = new ArrayList<>();
        }
        return messages;
    }

    public void setMessages(List<ResponseMessage> messages) {
        this.messages = messages;
    }

    public void addMessage(ResponseMessage message) {
        this.getMessages();
        this.messages.add(message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BaseRepresentation that = (BaseRepresentation) o;

        return new EqualsBuilder().append(messages, that.messages).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(INITIAL_ODD_NUMBER, MULTIPLIER_ODD_NUMBER).append(messages).toHashCode();
    }

    @Override
    public String toString() {
        return "BaseRepresentation{" + "messages=" + messages + '}';
    }
}
