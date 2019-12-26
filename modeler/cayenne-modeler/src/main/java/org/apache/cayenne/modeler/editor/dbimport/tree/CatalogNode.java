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

class CatalogNode extends Node<Node> {

    CatalogNode(String name) {
        super(name, null);
    }

    @Override
    public Status getStatus(ReverseEngineering config) {
        if(config.getCatalogs().isEmpty()) {
            return Status.INCLUDE;
        }

        if(getCatalog(config) != null) {
            return Status.INCLUDE;
        }

        return Status.EXCLUDE_IMPLICIT;
    }

    Catalog getCatalog(ReverseEngineering config) {
        for(Catalog catalog : config.getCatalogs()) {
            if(catalog.getName().equals(getName())) {
                return catalog;
            }
        }
        return null;
    }
}
