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
package org.apache.cayenne.project;

/**
 * Defines which parts of the project model are loaded from the project XML files.
 *
 * @see org.apache.cayenne.CoreModuleExtender#loadFullProjectModel()
 * @since 6.0
 */
public enum ProjectLoadingMode {

    /**
     * Loads only the parts of the project model needed by the Cayenne runtime. This is the default.
     */
    RUNTIME,

    /**
     * Also loads the tool-only parts of the project model that the runtime ignores: comments, class generation,
     * reverse engineering and validation settings.
     */
    FULL
}
