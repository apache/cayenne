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

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledEmbeddable;

/**
 * Interface that describes result of path translation
 *
 * @since 4.2
 */
interface PathTranslationResult {

    CayennePath getFinalPath();

    Optional<CompiledJoin> getDbRelationship();

    List<CompiledColumn> getDbAttributes();

    List<CayennePath> getAttributePaths();

    default Optional<CompiledEmbeddable> getEmbeddable() {
        return Optional.empty();
    }

    default CompiledColumn getLastAttribute() {
        return getDbAttributes().get(getDbAttributes().size() - 1);
    }

    default CayennePath getLastAttributePath() {
        return getAttributePaths().get(getAttributePaths().size() - 1);
    }

}
