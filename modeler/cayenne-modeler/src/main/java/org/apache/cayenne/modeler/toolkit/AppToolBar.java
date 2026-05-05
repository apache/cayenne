package org.apache.cayenne.modeler.toolkit;

import org.apache.cayenne.modeler.Application;

import javax.swing.*;

public class AppToolBar extends JToolBar {

    protected final Application app;

    protected AppToolBar(Application app) {
        this.app = app;
    }
}
