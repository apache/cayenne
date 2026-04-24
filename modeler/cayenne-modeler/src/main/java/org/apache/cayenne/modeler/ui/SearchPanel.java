package org.apache.cayenne.modeler.ui;

import org.apache.cayenne.modeler.action.FindAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

class SearchPanel extends JPanel {

    private final JLabel searchLabel;
    private final JPanel box;
    private final JTextField findField;

    public SearchPanel(FindAction findAction) {
        super(new BorderLayout());
        searchLabel = new JLabel("Search: ");
        box = new JPanel();

        findField = new JTextField(10);
        findField.putClientProperty("JTextField.variant", "search");
        findField.setMaximumSize(new Dimension(100, 22));
        findField.setPreferredSize(new Dimension(100, 22));
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
        findField.setAction(findAction);

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof KeyEvent) {
                if (((KeyEvent) event).getModifiersEx() == Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
                        && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_F) {
                    findField.requestFocus();
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);

        searchLabel.setLabelFor(findField);
        // is used to place label and text field one after another
        box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS));
        box.add(searchLabel);
        box.add(findField);

        add(box, BorderLayout.EAST);
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
