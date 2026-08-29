/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.ui.project.editor;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.ConstantSize;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.forms.util.LayoutStyle;
import org.apache.cayenne.modeler.toolkit.AppAction;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import java.awt.image.BufferedImage;

/**
 * Shared measurements of the editor forms: the column the labels line up in, the borders that let a
 * form span more than one panel, and the spacer that lines up an editor without a toolbar with the
 * editors that have one. Used by the editor main views and by the panels nested in their forms.
 */
public final class EditorForm {

    /**
     * Label column of the editor forms. Pinned to a fixed width instead of "pref", so that labels line
     * up across editors and in the panels nested in them. Just wide enough for the longest label in
     * any of the forms, which is the DataMap editor's "Default Java Package:".
     */
    public static final String LABEL_COLUMN = "right:67dlu";

    private EditorForm() {
    }

    /**
     * An empty stand-in for the toolbar of the editors that have one, so that a form in an editor
     * without a toolbar still starts at the same height on the screen.
     */
    public static JToolBar toolBarSpacer() {
        // toolbar icons are 16x16, and it is the button around one that sets the toolbar height
        JButton probe = new AppAction.CayenneToolbarButton(null, 0);
        probe.setIcon(new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)));

        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        toolBar.add(Box.createVerticalStrut(probe.getPreferredSize().height));
        return toolBar;
    }

    /**
     * Border of a form panel that another section of the same form follows: the standard dialog
     * border without the bottom margin, which is left to the section below.
     */
    public static Border formBorder() {
        return border(LayoutStyle.getCurrent().getDialogMarginY(), Sizes.ZERO);
    }

    /**
     * Border of a form section that continues the panel above it: dialog side margins, so that the
     * labels line up, the form row gap on top, so that the rows read as one list across the panel
     * boundary, and no bottom margin, as another section may follow.
     */
    public static Border sectionBorder() {
        return border(LayoutStyle.getCurrent().getLinePad(), Sizes.ZERO);
    }

    /**
     * Border of the section that closes a form: like {@link #sectionBorder()}, plus the dialog
     * bottom margin.
     */
    public static Border lastSectionBorder() {
        return border(LayoutStyle.getCurrent().getLinePad(), LayoutStyle.getCurrent().getDialogMarginY());
    }

    /**
     * Assembles a form border out of dialog units. The sizes are resolved by the border itself, once
     * it knows the component it applies to, as the same amount in dialog units is a different number
     * of pixels depending on the component font.
     */
    private static Border border(ConstantSize top, ConstantSize bottom) {
        ConstantSize sides = LayoutStyle.getCurrent().getDialogMarginX();
        return Borders.createEmptyBorder(top, sides, bottom, sides);
    }
}
