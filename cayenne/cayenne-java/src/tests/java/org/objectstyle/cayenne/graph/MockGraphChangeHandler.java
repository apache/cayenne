package org.objectstyle.cayenne.graph;

public class MockGraphChangeHandler implements GraphChangeHandler {

    int callbackCount;

    public int getCallbackCount() {
        return callbackCount;
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        callbackPosted();
    }

    public void nodeCreated(Object nodeId) {
        callbackPosted();
    }

    public void nodeRemoved(Object nodeId) {
        callbackPosted();
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        callbackPosted();
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        callbackPosted();
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        callbackPosted();
    }

    void callbackPosted() {
        callbackCount++;
    }
}
