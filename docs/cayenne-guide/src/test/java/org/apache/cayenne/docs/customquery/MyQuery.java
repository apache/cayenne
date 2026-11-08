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

package org.apache.cayenne.docs.customquery;

import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.resolved.ResolvedQuery;

// tag::content[]
public class MyQuery implements Query<Artist> {

    private final String namePrefix;

    public MyQuery(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public ResolvedQuery resolve(CompiledMapping mapping) {
        // the type parameter of the delegate matches the one of this query, as both select Artists
        return ObjectSelect.query(Artist.class)
                .where(Artist.NAME.startsWith(namePrefix))
                .orderBy(Artist.NAME.asc())
                .resolve(mapping);
    }
}
// end::content[]
