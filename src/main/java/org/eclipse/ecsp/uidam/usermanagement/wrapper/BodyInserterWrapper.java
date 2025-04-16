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

package org.eclipse.ecsp.uidam.usermanagement.wrapper;

import lombok.Getter;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

/**
 * Class to wrap webClient request body value.
 *
 * @param <T> represent request body can be of any type.
 */
public class BodyInserterWrapper<T> implements BodyInserter<T, ReactiveHttpOutputMessage> {

    private final BodyInserter<T, ReactiveHttpOutputMessage> delegate;
    @Getter
    private final T body;

    public BodyInserterWrapper(T body) {
        this.body = body;
        this.delegate = BodyInserters.fromValue(body);
    }

    @Override
    public Mono<Void> insert(ReactiveHttpOutputMessage outputMessage, Context context) {
        return this.delegate.insert(outputMessage, context);
    }
}
