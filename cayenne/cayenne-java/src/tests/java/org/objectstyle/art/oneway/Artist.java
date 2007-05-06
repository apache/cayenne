package org.objectstyle.art.oneway;

import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.event.DataObjectTransactionEventListener;

public class Artist extends org.objectstyle.art.oneway.auto._Artist implements DataObjectTransactionEventListener {
    private boolean _receivedWillCommit = false;
    private boolean _receivedDidCommit = false;

    protected String someOtherProperty;
    protected Object someOtherObjectProperty;

    public Artist() {
        super();
    }

    public void didCommit(DataContextEvent event) {
        _receivedDidCommit = true;
    }

    public void willCommit(DataContextEvent event) {
        _receivedWillCommit = true;
    }

    public boolean receivedDidCommit() {
        return _receivedDidCommit;
    }

    public boolean receivedWillCommit() {
        return _receivedWillCommit;
    }

    public void resetEvents() {
        _receivedWillCommit = false;
        _receivedDidCommit = false;
    }

    public String getSomeOtherProperty() {
        return someOtherProperty;
    }

    public void setSomeOtherProperty(String string) {
        someOtherProperty = string;
    }

    public Object getSomeOtherObjectProperty() {
        return someOtherObjectProperty;
    }

    public void setSomeOtherObjectProperty(Object object) {
        someOtherObjectProperty = object;
    }

}
