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

package org.apache.cayenne.modeler.editor.dbimport;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DbImportSorterTest {

    private DbImportTreeNode node;

    @Before
    public void createNode(){
        node = new DbImportTreeNode(new Catalog());

        DbImportTreeNode tableNode = new DbImportTreeNode(new IncludeTable("a"));
        tableNode.add(new DbImportTreeNode(new IncludeColumn("2")));
        tableNode.add(new DbImportTreeNode(new IncludeColumn("1")));

        node.add(new DbImportTreeNode(new IncludeTable("c")));
        node.add(new DbImportTreeNode(new IncludeTable("b")));
        node.add(tableNode);
    }

    @Test
    public void sortNodeTest(){
        DbImportSorter.sortSingleNode(node);

        assertEquals("a", node.getChildNodes().get(0).getSimpleNodeName());
        assertEquals("b", node.getChildNodes().get(1).getSimpleNodeName());
        assertEquals("c", node.getChildNodes().get(2).getSimpleNodeName());
    }


    @Test
    public void sortNodeWithAllChildrenTest(){
        DbImportSorter.sortSubtree(node);

        DbImportTreeNode tableNode = node.getChildNodes().get(0);
        DbImportTreeNode columnNode = tableNode.getChildNodes().get(0);
        assertEquals("1", columnNode.getSimpleNodeName());
    }

}
