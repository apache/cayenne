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

package org.apache.cayenne.project;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
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
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.project.extension.SaverDelegate;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.XMLEncoder;

import java.util.Collection;

/**
 * @since 4.1
 */
class CompoundSaverDelegate implements SaverDelegate {

    Collection<SaverDelegate> delegates;

    CompoundSaverDelegate(Collection<SaverDelegate> delegates) {
        this.delegates = delegates;
        setParentDelegate(this);
    }

    @Override
    public Void visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitDataChannelDescriptor(channelDescriptor);
        }
        return null;
    }

    @Override
    public Void visitDataNodeDescriptor(DataNodeDescriptor nodeDescriptor) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitDataNodeDescriptor(nodeDescriptor);
        }
        return null;
    }

    @Override
    public Void visitDataMap(DataMap dataMap) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitDataMap(dataMap);
        }
        return null;
    }

    @Override
    public Void visitObjEntity(ObjEntity entity) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitObjEntity(entity);
        }
        return null;
    }

    @Override
    public Void visitDbEntity(DbEntity entity) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitDbEntity(entity);
        }
        return null;
    }

    @Override
    public Void visitEmbeddable(Embeddable embeddable) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitEmbeddable(embeddable);
        }
        return null;
    }

    @Override
    public Void visitEmbeddableAttribute(EmbeddableAttribute attribute) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitEmbeddableAttribute(attribute);
        }
        return null;
    }

    @Override
    public Void visitObjAttribute(ObjAttribute attribute) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitObjAttribute(attribute);
        }
        return null;
    }

    @Override
    public Void visitDbAttribute(DbAttribute attribute) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitDbAttribute(attribute);
        }
        return null;
    }

    @Override
    public Void visitObjRelationship(ObjRelationship relationship) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitObjRelationship(relationship);
        }
        return null;
    }

    @Override
    public Void visitDbRelationship(DbRelationship relationship) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitDbRelationship(relationship);
        }
        return null;
    }

    @Override
    public Void visitProcedure(Procedure procedure) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitProcedure(procedure);
        }
        return null;
    }

    @Override
    public Void visitProcedureParameter(ProcedureParameter parameter) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitProcedureParameter(parameter);
        }
        return null;
    }

    @Override
    public Void visitQuery(QueryDescriptor query) {
        for(SaverDelegate delegate : delegates) {
            delegate.visitQuery(query);
        }
        return null;
    }

    @Override
    public void setXMLEncoder(XMLEncoder encoder) {
        for(SaverDelegate delegate : delegates) {
            delegate.setXMLEncoder(encoder);
        }
    }

    @Override
    public void setParentDelegate(SaverDelegate parentDelegate) {
        for(SaverDelegate delegate : delegates) {
            delegate.setParentDelegate(parentDelegate);
        }
    }

    @Override
    public SaverDelegate getParentDelegate() {
        return null;
    }

    @Override
    public Resource getBaseDirectory() {
        return null;
    }

    @Override
    public void setBaseDirectory(Resource baseDirectory) {
        delegates.forEach(d -> d.setBaseDirectory(baseDirectory));
    }
}
