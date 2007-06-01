/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.modeler.editor;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.event.QueryDisplayEvent;
import org.objectstyle.cayenne.modeler.event.QueryDisplayListener;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * A tabbed view for configuring a select query.
 * 
 * @author Andrei Adamchik
 */
public class SelectQueryTabbedView extends JTabbedPane {

    protected ProjectController mediator;
    protected SelectQueryMainTab mainTab;
    protected SelectQueryPrefetchTab prefetchTab;
    protected SelectQueryOrderingTab orderingTab;
    protected int lastSelectionIndex;

    public SelectQueryTabbedView(ProjectController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    private void initView() {
        setTabPlacement(JTabbedPane.TOP);

        this.mainTab = new SelectQueryMainTab(mediator);
        addTab("General", new JScrollPane(mainTab));

        this.orderingTab = new SelectQueryOrderingTab(mediator);
        addTab("Orderings", new JScrollPane(orderingTab));

        this.prefetchTab = new SelectQueryPrefetchTab(mediator);
        addTab("Prefetches", new JScrollPane(prefetchTab));
    }

    private void initController() {
        mediator.addQueryDisplayListener(new QueryDisplayListener() {

            public void currentQueryChanged(QueryDisplayEvent e) {
                initFromModel();
            }
        });

        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                lastSelectionIndex = getSelectedIndex();
                updateTabs();
            }
        });
    }

    void initFromModel() {
        if (!(mediator.getCurrentQuery() instanceof SelectQuery)) {
            setVisible(false);
            return;
        }

        // if no root, reset tabs to show the first panel..
        if (mediator.getCurrentQuery().getRoot() == null) {
            lastSelectionIndex = 0;
        }

        // tab did not change - force update
        if (getSelectedIndex() == lastSelectionIndex) {
            updateTabs();
        }
        // change tab, this will update newly displayed tab...
        else {
            setSelectedIndex(lastSelectionIndex);
        }

        setVisible(true);
    }

    void updateTabs() {
        switch (lastSelectionIndex) {
            case 0:
                mainTab.initFromModel();
                break;
            case 1:
                orderingTab.initFromModel();
                break;
            case 2:
                prefetchTab.initFromModel();
                break;
        }
    }
}