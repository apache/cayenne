package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.modeler.editor.SQLTemplatePrefetchTab;
import org.apache.cayenne.modeler.editor.SelectQueryPrefetchTab;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class AddPrefetchUndoableEditForSqlTemplate extends AbstractUndoableEdit {

    private String prefetch;
    private SQLTemplatePrefetchTab tab;

    public AddPrefetchUndoableEditForSqlTemplate(String prefetch, SQLTemplatePrefetchTab tab) {
        super();
        this.prefetch = prefetch;
        this.tab = tab;
    }

    @Override
    public String getPresentationName() {
        return "Add Prefetch";
    }

    @Override
    public void redo() throws CannotRedoException {
        tab.addPrefetch(prefetch);
    }

    @Override
    public void undo() throws CannotUndoException {
        tab.removePrefetch(prefetch);
    }
}
