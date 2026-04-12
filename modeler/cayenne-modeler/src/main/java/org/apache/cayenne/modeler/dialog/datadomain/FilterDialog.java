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
package org.apache.cayenne.modeler.dialog.datadomain;

import javax.swing.*;

public class FilterDialog extends JPopupMenu {

    private final JCheckBox dbEntity;
    private final JCheckBox objEntity;
    private final JCheckBox embeddable;
    private final JCheckBox procedure;
    private final JCheckBox query;
    private final JCheckBox all;

    public FilterDialog() {
        this.all = new JCheckBox("Show all");
        this.dbEntity = new JCheckBox("DbEntity");
        this.objEntity = new JCheckBox("ObjEntity");
        this.embeddable = new JCheckBox("Embeddable");
        this.procedure = new JCheckBox("Procedure");
        this.query = new JCheckBox("Query");

        add(all);
        addSeparator();
        add(dbEntity);
        add(objEntity);
        add(embeddable);
        add(procedure);
        add(query);
    }

    public JCheckBox getDbEntity() { return dbEntity; }
    public JCheckBox getObjEntity() { return objEntity; }
    public JCheckBox getEmbeddable() { return embeddable; }
    public JCheckBox getProcedure() { return procedure; }
    public JCheckBox getQuery() { return query; }
    public JCheckBox getAll() { return all; }
}
