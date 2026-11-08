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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.map.DataMap;

import java.util.Collection;

/**
 * Compiles authored DataMaps into a {@link CompiledMapping}. An injectable service: a DataDomain compiles its maps on
 * startup, and again whenever a map is added or removed.
 *
 * @since 6.0
 */
public interface MappingCompiler {

    /**
     * Compiles the maps into a single mapping. The maps must be consistent as a whole: a reference from one map to an
     * entity or a table of another is resolved across the set.
     */
    CompiledMapping compile(Collection<DataMap> maps);
}
