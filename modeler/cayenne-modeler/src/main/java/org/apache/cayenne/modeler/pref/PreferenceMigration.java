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
package org.apache.cayenne.modeler.pref;

/**
 * One-time data migration applied by {@link PreferencesRepository#runMigrations()}.
 */
public interface PreferenceMigration {

    /**
     * Strictly positive, monotonic version. The repository tracks the highest applied
     * version under {@code app/_meta/migrations.appliedVersion} and runs every
     * migration with a higher version, in ascending order.
     */
    int version();

    void apply(PreferencesRepository repo);
}
