package org.apache.cayenne.modeler.ui;

import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.*;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

class MainToolBar extends JToolBar {

    private final JButton backButton;
    private final JButton removeButton;

    public MainToolBar(GlobalActions globalActions) {
        setFloatable(false);

        Dimension smallBtnDim = new Dimension(30, 30);
        backButton = globalActions.getAction(NavigateBackwardAction.class).buildButton(1);
        backButton.setMinimumSize(smallBtnDim);
        backButton.setPreferredSize(smallBtnDim);
        add(backButton);

        JButton forwardButton = globalActions.getAction(NavigateForwardAction.class).buildButton(3);
        forwardButton.setMinimumSize(smallBtnDim);
        forwardButton.setPreferredSize(smallBtnDim);
        add(forwardButton);

        addSeparator(new Dimension(30, 0));

        add(globalActions.getAction(NewProjectAction.class).buildButton(1));
        add(globalActions.getAction(OpenProjectAction.class).buildButton(2));
        add(globalActions.getAction(SaveAction.class).buildButton(3));

        addSeparator();

        removeButton = globalActions.getAction(RemoveAction.class).buildButton();
        add(removeButton);

        addSeparator();

        add(globalActions.getAction(CutAction.class).buildButton(1));
        add(globalActions.getAction(CopyAction.class).buildButton(2));
        add(globalActions.getAction(PasteAction.class).buildButton(3));

        addSeparator();

        add(globalActions.getAction(UndoAction.class).buildButton(1));
        add(globalActions.getAction(RedoAction.class).buildButton(3));

        addSeparator();

        add(globalActions.getAction(CreateNodeAction.class).buildButton(1));
        add(globalActions.getAction(CreateDataMapAction.class).buildButton(3));

        addSeparator();

        add(globalActions.getAction(CreateDbEntityAction.class).buildButton(1));
        add(globalActions.getAction(CreateProcedureAction.class).buildButton(3));

        addSeparator();

        add(globalActions.getAction(CreateObjEntityAction.class).buildButton(1));
        add(globalActions.getAction(CreateEmbeddableAction.class).buildButton(2));
        add(globalActions.getAction(CreateQueryAction.class).buildButton(3));

        // is used to place search feature components the most right on a toolbar
        add(new SearchPanel(globalActions.getAction(FindAction.class)));
    }

    int getDefaultButtonWidth() {
        return removeButton.getUI().getPreferredSize(backButton).width;
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
