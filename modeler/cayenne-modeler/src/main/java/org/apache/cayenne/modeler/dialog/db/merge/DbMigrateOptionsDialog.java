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

package org.apache.cayenne.modeler.dialog.db.merge;

import java.util.Collection;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DbActionOptionsDialog;

public class DbMigrateOptionsDialog extends DbActionOptionsDialog {
    
    public DbMigrateOptionsDialog(Collection<String> catalogs, Collection<String> schemas,
                                  String defaultCatalog, String defaultSchema) {
        super(Application.getFrame(), "Migrate DB Schema: Select Catalog and Schema",
                catalogs, schemas, defaultCatalog, defaultSchema);
    }

    @Override
    protected void initForm(DefaultFormBuilder builder) {
    }
}
