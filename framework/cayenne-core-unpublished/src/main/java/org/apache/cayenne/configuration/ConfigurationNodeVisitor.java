/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.configuration;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.query.Query;

/**
 * A visitor interface for implementing operations on different types of
 * {@link ConfigurationNode} objects.
 * 
 * @since 3.1
 */
public interface ConfigurationNodeVisitor<T> {

    T visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor);

    T visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor);

    T visitDataMap(DataMap dataMap);

    T visitObjEntity(ObjEntity entity);

    T visitDbEntity(DbEntity entity);

    T visitEmbeddable(Embeddable embeddable);

    T visitEmbeddableAttribute(EmbeddableAttribute attribute);

    T visitObjAttribute(ObjAttribute attribute);

    T visitDbAttribute(DbAttribute attribute);

    T visitObjRelationship(ObjRelationship relationship);

    T visitDbRelationship(DbRelationship relationship);

    T visitProcedure(Procedure procedure);

    T visitProcedureParameter(ProcedureParameter parameter);

    T visitQuery(Query query);
}
