/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog.codegen;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.objectstyle.cayenne.swing.control.ActionLink;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class CustomModePanel extends GeneratorControllerPanel {

    protected JComboBox generationMode;
    protected JComboBox subclassTemplate;
    protected JComboBox superclassTemplate;
    protected JCheckBox pairs;
    protected JComboBox generatorVersion;
    protected JCheckBox overwrite;
    protected JCheckBox usePackagePath;
    protected JTextField outputPattern;

    protected ActionLink manageTemplatesLink;

    public CustomModePanel() {

        this.generationMode = new JComboBox();
        this.superclassTemplate = new JComboBox();
        this.subclassTemplate = new JComboBox();
        this.pairs = new JCheckBox();
        this.generatorVersion = new JComboBox();
        this.overwrite = new JCheckBox();
        this.usePackagePath = new JCheckBox();
        this.outputPattern = new JTextField();
        this.manageTemplatesLink = new ActionLink("Customize Templates...");
        manageTemplatesLink.setFont(manageTemplatesLink.getFont().deriveFont(10f));

        pairs.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                superclassTemplate.setEnabled(pairs.isSelected());
            }
        });

        // assemble

        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:70dlu, 3dlu, 150dlu, 3dlu, pref",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"));
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.addLabel("Output Directory:", cc.xy(1, 1));
        builder.add(outputFolder, cc.xy(3, 1));
        builder.add(selectOutputFolder, cc.xy(5, 1));

        builder.addLabel("Superclass Package:", cc.xy(1, 3));
        builder.add(superclassPackage, cc.xy(3, 3));

        builder.addLabel("Generation Mode:", cc.xy(1, 5));
        builder.add(generationMode, cc.xy(3, 5));

        builder.addLabel("Generator Version:", cc.xy(1, 7));
        builder.add(generatorVersion, cc.xy(3, 7));

        builder.addLabel("Subclass Template:", cc.xy(1, 9));
        builder.add(subclassTemplate, cc.xy(3, 9));

        builder.addLabel("Superclass Template:", cc.xy(1, 11));
        builder.add(superclassTemplate, cc.xy(3, 11));

        builder.addLabel("Output Pattern:", cc.xy(1, 13));
        builder.add(outputPattern, cc.xy(3, 13));

        builder.addLabel("Make Pairs:", cc.xy(1, 15));
        builder.add(pairs, cc.xy(3, 15));

        builder.addLabel("Overwrite Subclasses:", cc.xy(1, 17));
        builder.add(overwrite, cc.xy(3, 17));

        builder.addLabel("Use Package Path:", cc.xy(1, 19));
        builder.add(usePackagePath, cc.xy(3, 19));

        JPanel links = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        links.add(manageTemplatesLink);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
        add(links, BorderLayout.SOUTH);
    }

    public JComboBox getGenerationMode() {
        return generationMode;
    }

    public ActionLink getManageTemplatesLink() {
        return manageTemplatesLink;
    }

    public JComboBox getSubclassTemplate() {
        return subclassTemplate;
    }

    public JComboBox getSuperclassTemplate() {
        return superclassTemplate;
    }

    public JComboBox getGeneratorVersion() {
        return generatorVersion;
    }

    public JCheckBox getOverwrite() {
        return overwrite;
    }

    public JCheckBox getPairs() {
        return pairs;
    }

    public JCheckBox getUsePackagePath() {
        return usePackagePath;
    }

    
    public JTextField getOutputPattern() {
        return outputPattern;
    }
}
