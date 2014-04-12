package org.apache.cayenne.modeler.editor.wrapper;

public interface Wrapper<T> {

    T getValue();

    boolean isValid();

    void commitEdits();

    void resetEdits();
}
