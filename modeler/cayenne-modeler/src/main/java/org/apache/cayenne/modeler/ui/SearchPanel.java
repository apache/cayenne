package org.apache.cayenne.modeler.ui;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.modeler.ui.action.FindAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class SearchPanel extends AppPanel {

    private final JLabel searchLabel;
    private final JPanel box;
    private final JTextField findField;

    public SearchPanel(Application app) {
        super(app);

        this.searchLabel = new JLabel("Search: ");
        this.box = new JPanel();
        this.findField = new JTextField(10);

        initLayout();
        initBindings();
    }

    private void initLayout() {
        setLayout(new BorderLayout());

        findField.putClientProperty("JTextField.variant", "search");
        findField.setMaximumSize(new Dimension(100, 22));
        findField.setPreferredSize(new Dimension(100, 22));
        searchLabel.setLabelFor(findField);
        // is used to place label and text field one after another
        box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS));
        box.add(searchLabel);
        box.add(findField);
        add(box, BorderLayout.EAST);
    }

    private void initBindings() {
        findField.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                    findField.setBackground(Color.white);
                }
            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
            }
        });
        findField.setAction(app.getActionManager().getAction(FindAction.class));

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof KeyEvent) {
                if (((KeyEvent) event).getModifiersEx() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
                        && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_F) {
                    findField.requestFocus();
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }

    public void hideSearchLabel() {
        searchLabel.setVisible(false);
        findField.setMaximumSize(null);
        findField.setPreferredSize(new Dimension(100, 40));
        findField.setToolTipText("Search");
        box.setOpaque(false);
        box.setBackground(null);
    }
}
