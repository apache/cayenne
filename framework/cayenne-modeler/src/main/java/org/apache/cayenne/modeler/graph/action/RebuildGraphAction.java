package org.apache.cayenne.modeler.graph.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.graph.DataDomainGraphTab;
import org.apache.cayenne.modeler.util.CayenneAction;

/**
 * Action for refreshing the graph 
 */
public class RebuildGraphAction extends CayenneAction {
    private final DataDomainGraphTab dataDomainGraphTab;

    public RebuildGraphAction(DataDomainGraphTab dataDomainGraphTab, Application application) {
        super("Rebuild", application);
        this.dataDomainGraphTab = dataDomainGraphTab;
        setEnabled(true);
    }
    
    @Override
    public String getIconName() {
        return "icon-refresh.png";
    }
    
    @Override
    public void performAction(ActionEvent e) {
        this.dataDomainGraphTab.rebuild();
    }
}