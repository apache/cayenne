/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import org.apache.cayenne.modeler.action.NewProjectAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.event.RecentFileListListener;
import org.apache.cayenne.modeler.util.ModelerUtil;

/**
 * Welcome screen (CAY-894) is a panel shown when no project is open.
 * User can quickly create new project or open an existing one.
 */
public class WelcomeScreen extends JPanel implements RecentFileListListener {

    /**
     * Top color of gradient background
     */
    private static final Color TOP_GRADIENT = new Color(153, 153, 153);

    /**
     * Bottom color of gradient background
     */
    private static final Color BOTTOM_GRADIENT = new Color(230, 230, 230);
    
    /**
     * List of recent projects
     */
    private JList recentsList;

    public WelcomeScreen() {
        initView();
    }

    /**
     * Creates all neccesary components
     */
    protected void initView() {
        setLayout(new GridBagLayout());

        ImageIcon welcome = ModelerUtil.buildIcon("welcome.jpg");
        JLabel imageLabel = new JLabel(welcome);

        ImageIcon newOutIcon = ModelerUtil.buildIcon("icon-welcome-new.png");
        ImageIcon newOverIcon = ModelerUtil.buildIcon("icon-welcome-new-over.png");
        ImageIcon openOutIcon = ModelerUtil.buildIcon("icon-welcome-open.png");
        ImageIcon openOverIcon = ModelerUtil.buildIcon("icon-welcome-open-over.png");

        JPanel buttonsPane = new JPanel(null);
        buttonsPane.setPreferredSize(new Dimension(300, 30));

        buttonsPane.setOpaque(false);

        JButton newButton = createButton(newOutIcon, newOverIcon);
        newButton.addActionListener(Application.getInstance().getAction(NewProjectAction.getActionName()));
        
        JLabel newLabel = new JLabel(NewProjectAction.getActionName(), SwingConstants.CENTER);

        JButton openButton = createButton(openOutIcon, openOverIcon);
        openButton.addActionListener(Application.getInstance().getAction(OpenProjectAction.getActionName()));
        
        JLabel openLabel = new JLabel(OpenProjectAction.getActionName(), SwingConstants.CENTER);

        imageLabel.setLayout(new BorderLayout());

        newButton.setLocation(10, 130);
        buttonsPane.add(newButton);

        newLabel.setLocation(newButton.getX()
                + newButton.getWidth() / 2 - newLabel.getPreferredSize().width / 2, 
                newButton.getY() + newButton.getHeight());
        newLabel.setSize(newLabel.getPreferredSize());
        buttonsPane.add(newLabel);

        openButton.setLocation(120, newButton.getY());
        buttonsPane.add(openButton);

        openLabel.setLocation(openButton.getX()
                + openButton.getWidth() / 2 - openLabel.getPreferredSize().width / 2, 
                openButton.getY() + openButton.getHeight());
        openLabel.setSize(openLabel.getPreferredSize());
        buttonsPane.add(openLabel);
        
        JLabel recents = new JLabel("Recent Projects:");
        recents.setLocation(207, newButton.getY());
        recents.setSize(recents.getPreferredSize());
        recents.setHorizontalTextPosition(10);
        
        buttonsPane.add(recents);
        
        recentsList = new JList();
        recentsList.setOpaque(false);
        
        recentsList.setLocation(recents.getX(), recents.getY() + 2 * recents.getHeight());
        recentsList.setSize(welcome.getIconWidth() - recentsList.getX() - 1, 
                welcome.getIconHeight() - recentsList.getY());
        
        recentsList.setCellRenderer(new RecentFileListRenderer(recentsList));
        
        buttonsPane.add(recentsList);

        imageLabel.add(buttonsPane);

        add(imageLabel);
    }

    /**
     * Creates welcome screen-specific button
     */
    private JButton createButton(Icon outIcon, Icon overIcon) {
        JButton button = new JButton();

        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);

        button.setPressedIcon(overIcon);
        button.setRolloverIcon(overIcon);
        button.setIcon(outIcon);

        button.setSize(outIcon.getIconWidth(), outIcon.getIconHeight());

        return button;
    }

    /**
     * Paints gradient background
     */
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new GradientPaint(
                        0,
                        0,
                        TOP_GRADIENT,
                        0,
                        getHeight(),
                        BOTTOM_GRADIENT));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.dispose();
    }
    
    public void recentFileListChanged() {
        ModelerPreferences pref = ModelerPreferences.getPreferences();
        final Vector<?> arr = (Vector<?>) pref.getVector(ModelerPreferences.LAST_PROJ_FILES).clone();
        
        recentsList.setModel(
            new AbstractListModel() {
                public int getSize() { return arr.size(); }
                public Object getElementAt(int i) { return arr.elementAt(i); }
            });
    }
    
    /**
     * Renderer for the list of last files. Ignores the selection, instead paints
     * with ROLLOVER_BACKGROUND (currently red) the row mouse is hovering over
     */
    class RecentFileListRenderer extends DefaultListCellRenderer implements MouseInputListener {
        /**
         * Color for background of row mouse is over
         */
        final Color ROLLOVER_BACKGROUND = Color.RED;
        
        /**
         * Color for foreground of row mouse is over
         */
        final Color ROLLOVER_FOREGROUND = Color.WHITE;
        
        /**
         * List which is rendered
         */
        private JList list;
        
        /**
         * Row mouse is over
         */
        private int rolloverRow;
        
        public RecentFileListRenderer(JList list) {
            list.addMouseListener(this);
            list.addMouseMotionListener(this);
            
            this.list = list;
            rolloverRow = -1;
            
            setHorizontalTextPosition(10);
        }
        
        @Override
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            //selection is ignored
            super.getListCellRendererComponent(list, value, index, false, false);
            
            if (rolloverRow == index) {
                setOpaque(true);
                setForeground(ROLLOVER_FOREGROUND);
                setBackground(ROLLOVER_BACKGROUND);
            }
            else {
                setOpaque(false);
            }
            
            return this;
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            mouseMoved(e);
        }

        public void mouseExited(MouseEvent e) {
            rolloverRow = -1;
            list.repaint();
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && rolloverRow != -1) {
                File file = new File((String) list.getModel().getElementAt(rolloverRow));
                
                /**
                 * Fire an action with the file as source
                 */
                Application.getInstance().getAction(OpenProjectAction.getActionName()).performAction(
                        new ActionEvent(file, 0, null));
                
                rolloverRow = -1; //clear selection
            }
        }

        public void mouseDragged(MouseEvent e) {
        }

        public void mouseMoved(MouseEvent e) {
            int newRow;
            
            /**
             * Check that a row boundary contains the mouse point, so that rolloverRow would
             * be -1 if we are below last row
             */
            if (list.getModel().getSize() > 0 && 
                    !list.getCellBounds(0, list.getModel().getSize() - 1).contains(e.getPoint())) {
                newRow = -1;
            }
            else {
                newRow = list.locationToIndex(e.getPoint()); 
            }
            
            if (rolloverRow != newRow) {
                rolloverRow = newRow;
                list.repaint();
            }
            
            
        }
        
    }
}
