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

package org.apache.cayenne.modeler.editor.dbimport.tree;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.dbsync.reverse.dbimport.SchemaContainer;

class SchemaNode extends Node<CatalogNode> {

    SchemaNode(String name, CatalogNode parent) {
        super(name, parent);
    }

    @Override
    public Status getStatus(ReverseEngineering config) {
        // check via parent path
        if(getParent() != null) {
            Status parentStatus = getParent().getStatus(config);
            if(parentStatus != Status.INCLUDE) {
                return parentStatus;
            }

            Catalog parentCatalog = getParent().getCatalog(config);
            if(parentCatalog != null && includesSchema(parentCatalog) == Status.INCLUDE) {
                return Status.INCLUDE;
            }
        }

        // check root
        return includesSchema(config);
    }

    Status includesSchema(SchemaContainer container) {
        if(container.getSchemas().isEmpty()) {
            return Status.INCLUDE;
        }
        if(getSchema(container) != null) {
            return Status.INCLUDE;
        }
        return Status.EXCLUDE_IMPLICIT;
    }

    Schema getSchema(SchemaContainer container) {
        for(Schema schema : container.getSchemas()) {
            if(schema.getName().equals(getName())) {
                return schema;
            }
        }
        return null;
    }
}
