/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.project.upgrade;

import java.util.List;

/**
 * A change made by a project upgrade that the user needs to know about.
 *
 * @param message        a description of the change and of the manual steps it requires, if any
 * @param details        the facts needed to complete the manual steps, that the upgraded project no longer has, e.g.
 *                       the settings of a removed DataNode, one per line, as "label: value"
 * @param affectsRuntime whether the change may affect the runtime behavior, e.g. because a part of the project was
 *                       removed without a replacement, and the user has to recreate it in code
 * @since 6.0
 */
public record UpgradeChange(String message, List<String> details, boolean affectsRuntime) {
}
