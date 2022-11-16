package org.apache.cayenne.project;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.project.extension.ProjectExtension;
import org.apache.cayenne.project.upgrade.handlers.UpgradeHandler;

/**
 * @since 5.0
 */
public class ProjectModuleExtender {

    private final Binder binder;

    private ListBuilder<UpgradeHandler> upgradeHandlers;
    private ListBuilder<ProjectExtension> extensions;

    public ProjectModuleExtender(Binder binder) {
        this.binder = binder;
    }

    protected ProjectModuleExtender initAllExtensions() {
        contributeExtensions();
        contributeUpgradeHandler();
        return this;
    }

    public ProjectModuleExtender addUpgradeHandler(UpgradeHandler handler) {
        contributeUpgradeHandler().add(handler);
        return this;
    }

    public ProjectModuleExtender addUpgradeHandler(Class<? extends UpgradeHandler> handler) {
        contributeUpgradeHandler().add(handler);
        return this;
    }

    public ProjectModuleExtender addExtension(ProjectExtension extension) {
        contributeExtensions().add(extension);
        return this;
    }

    public ProjectModuleExtender addExtension(Class<? extends ProjectExtension> extension) {
        contributeExtensions().add(extension);
        return this;
    }

    private ListBuilder<ProjectExtension> contributeExtensions() {
        if (extensions == null) {
            extensions = binder.bindList(ProjectExtension.class);
        }
        return extensions;
    }

    private ListBuilder<UpgradeHandler> contributeUpgradeHandler() {
        if (upgradeHandlers == null) {
            upgradeHandlers = binder.bindList(UpgradeHandler.class);
        }
        return upgradeHandlers;
    }
}
