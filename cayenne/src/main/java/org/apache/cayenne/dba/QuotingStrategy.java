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
package org.apache.cayenne.dba;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.Entity;

/**
 * @since 3.0
 */
public interface QuotingStrategy {

    /**
     * Builds a fully qualified name from catalog, schema, name parts of
     * DbEntity, enclosing them in quotations according to this strategy
     * algorithm. Analog of "quotedIdentifier(entity.getCatalog(),
     * entity.getSchema(), entity.getName())".
     * 
     * @since 4.0
     */
    String quotedFullyQualifiedName(DbEntity entity);

    /**
     * 
     * @since 4.0
     */
    String quotedName(DbAttribute attribute);

    /**
     * @since 4.0
     */
    String quotedSourceName(DbJoin join);

    /**
     * @since 4.0
     */
    String quotedTargetName(DbJoin join);

    /**
     * @since 4.0
     */
    default String quotedIdentifier(DbEntity entity, String... identifierParts) {
        return quotedIdentifier(entity.getDataMap(), identifierParts);
    }
    
    /**
     * @since 4.0
     */
    String quotedIdentifier(DataMap dataMap, String... identifierParts);

    /**
     * Append quoted identifier to provided appender
     * @since 4.2
     */
    default void quotedIdentifier(DbEntity entity, CharSequence identifier, Appendable appender) {
        quotedIdentifier(entity.getDataMap(), identifier, appender);
    }

    /**
     * Append quoted identifier to provided appender
     * @since 4.2
     */
    void quotedIdentifier(DataMap dataMap, CharSequence identifier, Appendable appender);
}
