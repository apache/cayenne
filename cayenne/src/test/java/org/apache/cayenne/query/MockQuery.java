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

package org.apache.cayenne.query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.testdo.testmap.Artist;

/**
 * A custom query that delegates its resolution to a standard select, and records whether it was resolved, i.e. whether
 * the context let it run.
 */
public class MockQuery implements Query<Object> {

    protected boolean resolved;

    public boolean isResolved() {
        return resolved;
    }

    @Override
    public ResolvedQuery resolve(CompiledMapping mapping) {
        resolved = true;
        if (mapping == null) {
            throw new CayenneRuntimeException("Null mapping");
        }
        return ObjectSelect.query(Artist.class).resolve(mapping);
    }
}
