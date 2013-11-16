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
 * A {@link ConfigurationNodeVisitor} that does nothing, used as a convenience superclass
 * for partial visitors. All methods of this visitor throw an
 * {@link UnsupportedOperationException}.
 * 
 * @since 3.1
 */
public abstract class BaseConfigurationNodeVisitor<T> implements
        ConfigurationNodeVisitor<T> {

    public T visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
        throw new UnsupportedOperationException(
                "Not implemented for DataChannelDescriptor");
    }

    public T visitDataMap(DataMap dataMap) {
        throw new UnsupportedOperationException("Not implemented for DataMap");
    }

    public T visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
        throw new UnsupportedOperationException("Not implemented for DataNodeDescriptor");
    }

    public T visitDbAttribute(DbAttribute attribute) {
        throw new UnsupportedOperationException("Not implemented for DbAttribute");
    }

    public T visitDbEntity(DbEntity entity) {
        throw new UnsupportedOperationException("Not implemented for DbEntity");
    }

    public T visitDbRelationship(DbRelationship relationship) {
        throw new UnsupportedOperationException("Not implemented for DbRelationship");
    }

    public T visitEmbeddable(Embeddable embeddable) {
        throw new UnsupportedOperationException("Not implemented for Embeddable");
    }

    public T visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        throw new UnsupportedOperationException("Not implemented for EmbeddableAttribute");
    }

    public T visitObjAttribute(ObjAttribute attribute) {
        throw new UnsupportedOperationException("Not implemented for ObjAttribute");
    }

    public T visitObjEntity(ObjEntity entity) {
        throw new UnsupportedOperationException("Not implemented for ObjEntity");
    }

    public T visitObjRelationship(ObjRelationship relationship) {
        throw new UnsupportedOperationException("Not implemented for ObjRelationship");
    }

    public T visitProcedure(Procedure procedure) {
        throw new UnsupportedOperationException("Not implemented for Procedure");
    }

    public T visitProcedureParameter(ProcedureParameter parameter) {
        throw new UnsupportedOperationException("Not implemented for ProcedureParameter");
    }

    public T visitQuery(Query query) {
        throw new UnsupportedOperationException("Not implemented for Query");
    }

}
