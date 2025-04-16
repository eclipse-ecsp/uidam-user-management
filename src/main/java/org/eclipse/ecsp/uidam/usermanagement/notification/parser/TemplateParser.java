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

package org.eclipse.ecsp.uidam.usermanagement.notification.parser;

import org.springframework.core.io.Resource;
import java.util.Map;

/**
 * Template Manager will resolve the placeholder in the content template.
 */
public interface TemplateParser {
    /**
     * This method will take the template name or the template content as input and the placeholders values.
     *
     * @param template  template name or the template content
     * @param placeholderValues placeholders key will be placeholder and the value of the placeholder to be replaced
     * @return the resolved String template
     */
    String parseText(String template, Map<String, Object> placeholderValues);

    /**
     * This method will take the template name or the template content as input and the placeholders values.
     *
     * @param template  template name or the template content
     * @param placeholderValues placeholders key will be placeholder and the value of the placeholder to be replaced
     * @return the resolved String template
     */
    String parseTemplate(String template, Map<String, Object> placeholderValues);


    Resource getFile(String path);
}
