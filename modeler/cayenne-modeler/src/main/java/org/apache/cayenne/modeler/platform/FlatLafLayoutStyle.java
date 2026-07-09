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
package org.apache.cayenne.modeler.platform;

import com.jgoodies.forms.layout.ConstantSize;
import com.jgoodies.forms.layout.Size;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.forms.util.LayoutStyle;

public final class FlatLafLayoutStyle extends LayoutStyle {

    public static final FlatLafLayoutStyle INSTANCE = new FlatLafLayoutStyle();

    private FlatLafLayoutStyle() {
    }

    private static final Size BUTTON_WIDTH = Sizes.dluX(39);
    private static final Size BUTTON_HEIGHT = Sizes.dluY(14);

    private static final ConstantSize DIALOG_MARGIN_X = Sizes.DLUX9;
    private static final ConstantSize DIALOG_MARGIN_Y = Sizes.DLUY9;

    private static final ConstantSize TABBED_DIALOG_MARGIN_X = Sizes.DLUX4;
    private static final ConstantSize TABBED_DIALOG_MARGIN_Y = Sizes.DLUY4;

    private static final ConstantSize LABEL_COMPONENT_PADX = Sizes.DLUX3;
    private static final ConstantSize RELATED_COMPONENTS_PADX = Sizes.DLUX4;
    private static final ConstantSize UNRELATED_COMPONENTS_PADX = Sizes.DLUX8;

    private static final ConstantSize RELATED_COMPONENTS_PADY = Sizes.DLUY6;
    private static final ConstantSize UNRELATED_COMPONENTS_PADY = Sizes.DLUY6;
    private static final ConstantSize NARROW_LINE_PAD = Sizes.DLUY2;
    private static final ConstantSize LINE_PAD = Sizes.DLUY6;
    private static final ConstantSize PARAGRAPH_PAD = Sizes.DLUY9;
    private static final ConstantSize BUTTON_BAR_PAD = Sizes.DLUY4;

    @Override
    public Size getDefaultButtonWidth() {
        return BUTTON_WIDTH;
    }

    @Override
    public Size getDefaultButtonHeight() {
        return BUTTON_HEIGHT;
    }

    @Override
    public ConstantSize getDialogMarginX() {
        return DIALOG_MARGIN_X;
    }

    @Override
    public ConstantSize getDialogMarginY() {
        return DIALOG_MARGIN_Y;
    }

    @Override
    public ConstantSize getTabbedDialogMarginX() {
        return TABBED_DIALOG_MARGIN_X;
    }

    @Override
    public ConstantSize getTabbedDialogMarginY() {
        return TABBED_DIALOG_MARGIN_Y;
    }

    @Override
    public ConstantSize getLabelComponentPadX() {
        return LABEL_COMPONENT_PADX;
    }

    @Override
    public ConstantSize getRelatedComponentsPadX() {
        return RELATED_COMPONENTS_PADX;
    }

    @Override
    public ConstantSize getRelatedComponentsPadY() {
        return RELATED_COMPONENTS_PADY;
    }

    @Override
    public ConstantSize getUnrelatedComponentsPadX() {
        return UNRELATED_COMPONENTS_PADX;
    }

    @Override
    public ConstantSize getUnrelatedComponentsPadY() {
        return UNRELATED_COMPONENTS_PADY;
    }

    @Override
    public ConstantSize getNarrowLinePad() {
        return NARROW_LINE_PAD;
    }

    @Override
    public ConstantSize getLinePad() {
        return LINE_PAD;
    }

    @Override
    public ConstantSize getParagraphPad() {
        return PARAGRAPH_PAD;
    }

    @Override
    public ConstantSize getButtonBarPad() {
        return BUTTON_BAR_PAD;
    }

    @Override
    public boolean isLeftToRightButtonOrder() {
        return false;
    }
}
