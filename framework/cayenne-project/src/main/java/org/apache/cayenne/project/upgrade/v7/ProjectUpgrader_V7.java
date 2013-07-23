package org.apache.cayenne.project.upgrade.v7;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.project.upgrade.ProjectUpgrader;
import org.apache.cayenne.project.upgrade.UpgradeHandler;
import org.apache.cayenne.resource.Resource;

/**
 * A ProjectUpgrader that handles project upgrades from version 3.0.0.1 and 6
 * to version 7
 */
public class ProjectUpgrader_V7 implements ProjectUpgrader {

    @Inject
    protected Injector injector;

    public UpgradeHandler getUpgradeHandler(Resource projectSource) {
        UpgradeHandler_V7 handler = new UpgradeHandler_V7(projectSource);
        System.out.println("PH OK");
        injector.injectMembers(handler);
        return handler;
    }

}
