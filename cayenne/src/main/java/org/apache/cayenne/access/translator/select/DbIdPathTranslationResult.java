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

package org.apache.cayenne.access.translator.select;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;

/**
 * @since 4.2
 */
class DbIdPathTranslationResult implements PathTranslationResult {

    private final CayennePath path;
    private final DbAttribute pk;

    DbIdPathTranslationResult(CayennePath path, DbAttribute pk) {
        this.path = Objects.requireNonNull(path);
        this.pk = Objects.requireNonNull(pk);
    }

    @Override
    public CayennePath getFinalPath() {
        return path;
    }

    @Override
    public Optional<DbRelationship> getDbRelationship() {
        return Optional.empty();
    }

    @Override
    public List<DbAttribute> getDbAttributes() {
        return Collections.singletonList(pk);
    }

    @Override
    public List<CayennePath> getAttributePaths() {
        return Collections.singletonList(path);
    }
}
