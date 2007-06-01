package org.objectstyle.cayenne.modeler.action;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.pref.DBConnectionInfo;
import org.objectstyle.cayenne.modeler.pref.DataNodeDefaults;
import org.objectstyle.cayenne.modeler.util.CayenneAction;
import org.objectstyle.cayenne.project.ProjectDataSource;

/**
 * @author Andrei Adamchik
 */
public abstract class DBWizardAction extends CayenneAction {

    public DBWizardAction(String name, Application application) {
        super(name, application);
    }

    // ==== Guessing user preferences... *****

    protected DataNode getPreferredNode() {
        ProjectController projectController = getProjectController();
        DataNode node = projectController.getCurrentDataNode();

        // try a node that belongs to the current DataMap ...
        if (node == null) {
            DataMap map = projectController.getCurrentDataMap();
            if (map != null) {
                node = projectController.getCurrentDataDomain().lookupDataNode(map);
            }
        }

        return node;
    }

    protected String preferredDataSourceLabel(DBConnectionInfo nodeInfo) {
        if (nodeInfo == null || nodeInfo.getDomainPreference() == null) {

            // only driver nodes have meaningful connection info set
            DataNode node = getPreferredNode();
            return (node != null && DriverDataSourceFactory.class.getName().equals(
                    node.getDataSourceFactory())) ? "DataNode Connection Info" : null;
        }

        return nodeInfo.getKey();
    }

    /**
     * Determines the most reasonable default DataSource choice.
     */
    protected DBConnectionInfo preferredDataSource() {
        DataNode node = getPreferredNode();

        // no current node...
        if (node == null) {
            return null;
        }

        // if node has local DS set, use it
        DataNodeDefaults nodeDefaults = (DataNodeDefaults) getProjectController()
                .getPreferenceDomainForDataDomain()
                .getDetail(node.getName(), DataNodeDefaults.class, false);

        String key = (nodeDefaults != null) ? nodeDefaults.getLocalDataSource() : null;
        if (key != null) {
            DBConnectionInfo info = (DBConnectionInfo) getApplication()
                    .getPreferenceDomain()
                    .getDetail(key, DBConnectionInfo.class, false);
            if (info != null) {
                return info;
            }
        }

        // extract data from the node
        if (!DriverDataSourceFactory.class.getName().equals(node.getDataSourceFactory())) {
            return null;
        }

        // create transient object..
        DBConnectionInfo nodeInfo = new DBConnectionInfo();

        nodeInfo.copyFrom(((ProjectDataSource) node.getDataSource()).getDataSourceInfo());
        if (node.getAdapter() != null) {
            nodeInfo.setDbAdapter(node.getAdapter().getClass().getName());
        }

        return nodeInfo;
    }

}