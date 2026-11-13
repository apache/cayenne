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
package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportTree;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.DbImportTreeNode;
import org.apache.cayenne.project.dbimport.Catalog;
import org.apache.cayenne.project.dbimport.ReverseEngineering;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DbImportTreeUndoableEditTest {

    private DataMap dataMap;
    private ReverseEngineering previous;
    private ReverseEngineering next;
    private RecordingTree tree;
    private TestSession session;
    private DbImportTreeUndoableEdit edit;

    @BeforeEach
    public void setUp() {
        dataMap = new DataMap("map");
        previous = new ReverseEngineering();
        previous.setName("previous");
        next = new ReverseEngineering();
        next.setName("next");
        dataMap.setReverseEngineering(new ReverseEngineering(next));

        tree = new RecordingTree();
        session = new TestSession();
        edit = new DbImportTreeUndoableEdit(previous, next, tree, session, dataMap);
    }

    @Test
    public void undoAndRedo() {
        session.selectedDataMap = dataMap;

        edit.undo();
        ReverseEngineering undone = dataMap.getReverseEngineering();
        assertApplied(previous, undone);
        assertSame(undone, tree.getReverseEngineering());
        assertEquals(1, tree.translated.size());
        assertSame(undone, tree.translated.get(0));

        edit.redo();
        ReverseEngineering redone = dataMap.getReverseEngineering();
        assertApplied(next, redone);
        assertSame(redone, tree.getReverseEngineering());
        assertEquals(2, tree.translated.size());
        assertSame(redone, tree.translated.get(1));
        assertFalse(tree.transferable);

        assertEquals(2, session.dirtyCalls);
    }

    @Test
    public void laterEditsDoNotChangeSnapshots() {
        edit.undo();
        dataMap.getReverseEngineering().addCatalog(new Catalog("c1"));

        edit.redo();
        dataMap.getReverseEngineering().addCatalog(new Catalog("c2"));

        edit.undo();
        assertApplied(previous, dataMap.getReverseEngineering());
        assertEquals(0, dataMap.getReverseEngineering().getCatalogs().size());

        edit.redo();
        assertApplied(next, dataMap.getReverseEngineering());
        assertEquals(0, dataMap.getReverseEngineering().getCatalogs().size());
    }

    private static void assertApplied(ReverseEngineering snapshot, ReverseEngineering applied) {
        assertNotSame(snapshot, applied);
        assertEquals(snapshot.getName(), applied.getName());
    }

    @Test
    public void undoWithOtherDataMapSelected() {
        DataMap otherMap = new DataMap("otherMap");
        session.selectedDataMap = otherMap;

        // the tree keeps showing the selected DataMap
        edit.undo();
        assertApplied(previous, dataMap.getReverseEngineering());
        assertNull(otherMap.getReverseEngineering());
        assertEquals(List.of(), tree.calls);
        assertEquals(1, session.dirtyCalls);
    }

    @Test
    public void undoWithNoDataMapSelected() {
        edit.undo();
        assertApplied(previous, dataMap.getReverseEngineering());
        assertEquals(List.of(), tree.calls);

        edit.redo();
        assertApplied(next, dataMap.getReverseEngineering());
        assertEquals(List.of(), tree.calls);
    }

    /**
     * Records the calls the edit makes, instead of rebuilding the tree.
     */
    static class RecordingTree extends DbImportTree {

        final List<String> calls = new ArrayList<>();
        final List<ReverseEngineering> translated = new ArrayList<>();
        boolean transferable;

        RecordingTree() {
            super(new DbImportTreeNode());
        }

        @Override
        public boolean stopEditing() {
            calls.add("stopEditing");
            return true;
        }

        @Override
        public void setReverseEngineering(ReverseEngineering reverseEngineering) {
            calls.add("setReverseEngineering");
            super.setReverseEngineering(reverseEngineering);
        }

        @Override
        public List<DbImportTreeNode> getTreeExpandList() {
            calls.add("getTreeExpandList");
            return List.of();
        }

        @Override
        public void translateReverseEngineeringToTree(ReverseEngineering reverseEngineering, boolean isTransferable) {
            calls.add("translateReverseEngineeringToTree");
            translated.add(reverseEngineering);
            transferable |= isTransferable;
        }

        @Override
        public void expandTree(List<DbImportTreeNode> expandIndexesList) {
            calls.add("expandTree");
        }
    }

    /**
     * A session with a settable selected DataMap, that counts the calls marking the project dirty.
     */
    static class TestSession extends ProjectSession {

        DataMap selectedDataMap;
        int dirtyCalls;

        TestSession() {
            super(null);
        }

        @Override
        public DataMap getSelectedDataMap() {
            return selectedDataMap;
        }

        @Override
        public void setDirty(boolean dirty) {
            if (dirty) {
                dirtyCalls++;
            }
        }
    }
}
