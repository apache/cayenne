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
package org.apache.cayenne.mcp.tools.dbimport.protocol;

/**
 * Per-step validation flags for {@code dbimport_run}. Boxed {@link Boolean} so unchecked slots can be {@code null}
 * (short-circuit on first failure).
 *
 * @since 5.0
 */
public record DbImportValidation(
        Boolean projectFound,
        Boolean dataMapFound,
        Boolean dbConnectorPresent,
        Boolean jdbcDriverLoadable,
        Boolean jdbcConnectionOpened
) {
    public static final DbImportValidation ALL_PASSED = new DbImportValidation(true, true, true, true, true);
}
