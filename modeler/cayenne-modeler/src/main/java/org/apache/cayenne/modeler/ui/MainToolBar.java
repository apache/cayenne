package org.apache.cayenne.modeler.ui;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.toolkit.AppToolBar;
import org.apache.cayenne.modeler.ui.action.CopyAction;
import org.apache.cayenne.modeler.ui.action.CreateDataMapAction;
import org.apache.cayenne.modeler.ui.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.ui.action.CreateEmbeddableAction;
import org.apache.cayenne.modeler.ui.action.CreateNodeAction;
import org.apache.cayenne.modeler.ui.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.ui.action.CreateProcedureAction;
import org.apache.cayenne.modeler.ui.action.CreateQueryAction;
import org.apache.cayenne.modeler.ui.action.CutAction;
import org.apache.cayenne.modeler.ui.action.NavigateBackwardAction;
import org.apache.cayenne.modeler.ui.action.NavigateForwardAction;
import org.apache.cayenne.modeler.ui.action.NewProjectAction;
import org.apache.cayenne.modeler.ui.action.OpenProjectAction;
import org.apache.cayenne.modeler.ui.action.PasteAction;
import org.apache.cayenne.modeler.ui.action.RedoAction;
import org.apache.cayenne.modeler.ui.action.RemoveAction;
import org.apache.cayenne.modeler.ui.action.SaveAction;
import org.apache.cayenne.modeler.ui.action.UndoAction;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

class MainToolBar extends AppToolBar {

    public MainToolBar(Application app) {
        super(app);
        initLayout();
    }

    private void initLayout() {
        setFloatable(false);

        GlobalActions actions = app.getActionManager();

        Dimension smallBtnDim = new Dimension(30, 30);

        JButton backButton = actions.getAction(NavigateBackwardAction.class).buildButton(1);
        backButton.setMinimumSize(smallBtnDim);
        backButton.setPreferredSize(smallBtnDim);
        add(backButton);

        JButton forwardButton = actions.getAction(NavigateForwardAction.class).buildButton(3);
        forwardButton.setMinimumSize(smallBtnDim);
        forwardButton.setPreferredSize(smallBtnDim);
        add(forwardButton);

        addSeparator(new Dimension(30, 0));

        add(actions.getAction(NewProjectAction.class).buildButton(1));
        add(actions.getAction(OpenProjectAction.class).buildButton(2));
        add(actions.getAction(SaveAction.class).buildButton(3));

        addSeparator();

        JButton removeButton = actions.getAction(RemoveAction.class).buildButton();
        add(removeButton);

        addSeparator();

        add(actions.getAction(CutAction.class).buildButton(1));
        add(actions.getAction(CopyAction.class).buildButton(2));
        add(actions.getAction(PasteAction.class).buildButton(3));

        addSeparator();

        add(actions.getAction(UndoAction.class).buildButton(1));
        add(actions.getAction(RedoAction.class).buildButton(3));

        addSeparator();

        add(actions.getAction(CreateNodeAction.class).buildButton(1));
        add(actions.getAction(CreateDataMapAction.class).buildButton(3));

        addSeparator();

        add(actions.getAction(CreateDbEntityAction.class).buildButton(1));
        add(actions.getAction(CreateProcedureAction.class).buildButton(3));

        addSeparator();

        add(actions.getAction(CreateObjEntityAction.class).buildButton(1));
        add(actions.getAction(CreateEmbeddableAction.class).buildButton(2));
        add(actions.getAction(CreateQueryAction.class).buildButton(3));

        add(new SearchPanel(app));
    }

    @Override
    public void setBorder(Border b) {
        Object border = UIManager.get("MainToolBar.border");
        if (border instanceof Border) {
            super.setBorder((Border) border);
        }
    }

    @Override
    public void setBackground(Color bg) {
        Object background = UIManager.get("MainToolBar.background");
        if (background instanceof Color) {
            super.setBackground((Color) background);
        }
    }
}
