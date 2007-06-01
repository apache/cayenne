package org.objectstyle.cayenne.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

/**
 * @author Andrei Adamchik
 */
public class ActionBinding extends BindingBase {

    protected Component view;

    public ActionBinding(JButton button, String propertyExpression) {
        super(propertyExpression);

        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireAction();
            }
        });

        this.view = button;
    }

    public Component getComponent() {
        if (view == null) {
            throw new BindingException("headless action");
        }

        return view;
    }

    public void updateView() {
        // noop
    }

    protected void fireAction() {
        // TODO: catch exceptions...
        getValue();
    }

}