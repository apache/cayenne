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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

class CatalogTableNode extends TableNode<CatalogNode> {

    CatalogTableNode(String name, CatalogNode parent) {
        super(name, parent);
    }

    @Override
    List<FilterContainer> getContainers(ReverseEngineering config) {
        List<FilterContainer> containers = new ArrayList<>();
        if(getParent() != null) {
            containers.add(getParent().getCatalog(config));
        }
        containers.add(config);
        return containers;
    }
}
