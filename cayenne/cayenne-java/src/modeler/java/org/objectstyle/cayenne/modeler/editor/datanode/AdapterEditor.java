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
package org.objectstyle.cayenne.modeler.editor.datanode;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.dba.AutoAdapter;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataNodeDisplayListener;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.modeler.util.ModelerDbAdapter;
import org.objectstyle.cayenne.swing.BindingBuilder;
import org.objectstyle.cayenne.swing.ObjectBinding;

/**
 * @author Andrus Adamchik
 */
public class AdapterEditor extends CayenneController {

    protected AdapterView view;
    protected DataNode node;
    protected ObjectBinding adapterNameBinding;

    public AdapterEditor(CayenneController parent) {
        super(parent);

        this.view = new AdapterView();
        initController();
    }

    protected void initController() {
        // init bindings
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        adapterNameBinding = builder.bindToTextField(
                view.getCustomAdapter(),
                "adapterName");

        // init listeners
        ((ProjectController) getParent())
                .addDataNodeDisplayListener(new DataNodeDisplayListener() {

                    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
                        refreshView(e.getDataNode());
                    }
                });

        getView().addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent e) {
                refreshView(node != null ? node : ((ProjectController) getParent())
                        .getCurrentDataNode());
            }
        });
    }

    protected void refreshView(DataNode node) {
        this.node = node;

        if (node == null) {
            getView().setVisible(false);
            return;
        }

        adapterNameBinding.updateView();
    }

    public Component getView() {
        return view;
    }

    public String getAdapterName() {
        if (node == null) {
            return null;
        }

        DbAdapter adapter = node.getAdapter();

        // TODO, Andrus, 11/3/2005 - to simplify this logic, it would be nice to
        // consistently load CustomDbAdapter... this would require an ability to set a
        // load delegate in OpenProjectAction
        if (adapter == null) {
            return null;
        }
        else if (adapter instanceof ModelerDbAdapter) {
            return ((ModelerDbAdapter) adapter).getAdapterClassName();
        }
        // don't do "instanceof" here, as we maybe dealing with a custom subclass...
        else if (adapter.getClass() == AutoAdapter.class) {
            return null;
        }
        else {
            return adapter.getClass().getName();
        }
    }

    public void setAdapterName(String name) {
        if (node == null) {
            return;
        }

        ModelerDbAdapter adapter = new ModelerDbAdapter(name, node.getDataSource());
        adapter.validate();
        node.setAdapter(adapter);
        
        DataNodeEvent e = new DataNodeEvent(AdapterEditor.this, node);
        ((ProjectController) getParent()).fireDataNodeEvent(e);
    }
}
