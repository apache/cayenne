package org.apache.cayenne.modeler.editor.dbentity;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.DbEntityCounterpartAction;
import org.apache.cayenne.modeler.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;

/**
 * Combines DbEntityAttributeTab and DbEntityRelationshipTab in JSplitPane.
 */

public class DbEntityAttributeRelationshipTab extends JPanel implements DbEntityDisplayListener, DbEntityListener {

    public DbEntityAttributePanel attributePanel;
    public DbEntityRelationshipPanel relationshipPanel;
    public JButton resolve = new JButton();
    private JSplitPane splitPane;

    private ProjectController mediator;

    private CutAttributeRelationshipAction cut;
    private RemoveAttributeRelationshipAction remove;
    private CopyAttributeRelationshipAction copy;

    public DbEntityAttributeRelationshipTab(ProjectController mediator) {
        this.mediator = mediator;

        init();
        initToolBar();
    }

    private void init() {
        this.setLayout(new BorderLayout());

        attributePanel = new DbEntityAttributePanel(mediator, this);
        relationshipPanel = new DbEntityRelationshipPanel(mediator, this);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, attributePanel, relationshipPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);

        add(splitPane);
    }

    private void initToolBar() {
        JToolBar toolBar = new JToolBar();
        ActionManager actionManager = Application.getInstance().getActionManager();

        toolBar.add(actionManager.getAction(CreateObjEntityAction.class).buildButton());
        toolBar.add(actionManager.getAction(CreateAttributeAction.class).buildButton());
        toolBar.add(actionManager.getAction(CreateRelationshipAction.class).buildButton());
        toolBar.add(actionManager.getAction(DbEntitySyncAction.class).buildButton());
        toolBar.add(actionManager.getAction(DbEntityCounterpartAction.class).buildButton());
        toolBar.addSeparator();

        Icon ico = ModelerUtil.buildIcon("icon-info.gif");
        resolve.setToolTipText("Database Mapping");
        resolve.setIcon(ico);
        toolBar.add(resolve).setEnabled(false);

        cut = actionManager.getAction(CutAttributeRelationshipAction.class);
        remove = actionManager.getAction(RemoveAttributeRelationshipAction.class);
        copy = actionManager.getAction(CopyAttributeRelationshipAction.class);

        toolBar.addSeparator();
        toolBar.add(remove.buildButton());
        toolBar.addSeparator();
        toolBar.add(cut.buildButton());
        toolBar.add(copy.buildButton());
        toolBar.add(actionManager.getAction(PasteAction.class).buildButton());

        add(toolBar, BorderLayout.NORTH);
    }

    public void updateActions(Object[] params) {
        ModelerUtil.updateActions(
                params.length,
                RemoveAttributeRelationshipAction.class,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
        if (params instanceof DbRelationship[]) {
            resolve.setEnabled(params.length > 0);
        }
    }

    public JButton getResolve() {
        return resolve;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public DbEntityAttributePanel getAttributePanel() {
        return attributePanel;
    }

    public DbEntityRelationshipPanel getRelationshipPanel() {
        return relationshipPanel;
    }

    public void dbEntityChanged(EntityEvent e) {
        relationshipPanel.dbEntityChanged(e);
    }

    public void dbEntityAdded(EntityEvent e) {
        relationshipPanel.dbEntityAdded(e);
    }

    public void dbEntityRemoved(EntityEvent e) {
        relationshipPanel.dbEntityRemoved(e);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        attributePanel.currentDbEntityChanged(e);
        relationshipPanel.currentDbEntityChanged(e);
    }
}