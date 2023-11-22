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

package org.apache.cayenne.access.sqlbuilder.sqltree;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.2
 * @param <T> type of the node to process
 */
public class PerAttributeChildProcessor<T extends Node> implements ChildProcessor<T> {

    private final Map<DbAttribute, ChildProcessor<T>> processorByAttribute = new ConcurrentHashMap<>();
    private final Function<T, DbAttribute> attributeMapper;
    private final Function<DbAttribute, ChildProcessor<T>> processorFactory;

    public PerAttributeChildProcessor(Function<T, DbAttribute> attributeMapper,
                                      Function<DbAttribute, ChildProcessor<T>> processorFactory) {
        this.processorFactory = processorFactory;
        this.attributeMapper = attributeMapper;
    }

    @Override
    public Optional<Node> process(Node parent, T child, int index) {
        DbAttribute dbAttribute = attributeMapper.apply(child);
        if(dbAttribute == null) {
            return processorFactory.apply(null).process(parent, child, index);
        }
        return processorByAttribute
                .computeIfAbsent(dbAttribute, processorFactory)
                .process(parent, child, index);
    }
}
