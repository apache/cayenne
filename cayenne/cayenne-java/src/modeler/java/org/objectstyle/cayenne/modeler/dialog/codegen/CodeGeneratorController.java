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

import java.awt.Component;

import javax.swing.JOptionPane;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.objectstyle.cayenne.gen.DefaultClassGenerator;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.swing.BindingBuilder;

/**
 * A controller for the class generator dialog.
 * 
 * @author Andrus Adamchik
 */
public class CodeGeneratorController extends CodeGeneratorControllerBase {

    protected CodeGeneratorDialog view;

    protected EntitiesTabController entitySelector;
    protected GeneratorTabController generatorSelector;

    public CodeGeneratorController(CayenneController parent, DataMap dataMap) {
        super(parent, dataMap);

        this.entitySelector = new EntitiesTabController(this);
        this.generatorSelector = new GeneratorTabController(this);
    }

    public Component getView() {
        return view;
    }

    public void startup() {
        // show dialog even on empty DataMap, as custom generation may still take
        // advantage of it

        view = new CodeGeneratorDialog(generatorSelector.getView(), entitySelector
                .getView());
        initBindings();

        view.pack();
        view.setModal(true);
        centerView();
        makeCloseableOnEscape();
        view.show();
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCancelButton(), "cancelAction()");
        builder.bindToAction(view.getGenerateButton(), "generateAction()");
        builder.bindToAction(this, "entitySelectedAction()", SELECTED_PROPERTY);
        builder.bindToAction(
                generatorSelector,
                "generatorSelectedAction()",
                GeneratorTabController.GENERATOR_PROPERTY);

        generatorSelectedAction();
    }

    public void generatorSelectedAction() {
        GeneratorController controller = generatorSelector.getGeneratorController();
        validate(controller);

        Predicate predicate = controller != null
                ? controller.getDefaultEntityFilter()
                : PredicateUtils.falsePredicate();

        updateSelection(predicate);
        entitySelector.entitySelectedAction();
    }

    public void entitySelectedAction() {
        int size = getSelectedEntitiesSize();
        String label;

        if (size == 0) {
            label = "No entities selected";
        }
        else if (size == 1) {
            label = "One entity selected";
        }
        else {
            label = size + " entities selected";
        }

        view.getEntityCount().setText(label);
    }

    public void cancelAction() {
        view.dispose();
    }

    public void generateAction() {
        DefaultClassGenerator generator = generatorSelector.getGenerator();

        if (generator != null) {
            try {
                generator.execute();
                JOptionPane.showMessageDialog(
                        (Component) this.getView(),
                        "Class generation finished");
            }
            catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        (Component) this.getView(),
                        "Error generating classes - " + e.getMessage());
            }
        }

        view.dispose();
    }
}
