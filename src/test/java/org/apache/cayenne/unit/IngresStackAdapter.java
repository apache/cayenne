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

package org.apache.cayenne.unit;

import java.sql.Connection;
import java.sql.Types;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

public class IngresStackAdapter extends AccessStackAdapter {

    public IngresStackAdapter(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public boolean supportsBinaryPK() {
        return false;
    }
    
    /**
     * Ingres doesn't support LONGVARCHAR comparisions ('like', '=', etc.)
     */
    @Override
    public void willCreateTables(Connection con, DataMap map) {
        DbEntity paintingInfo = map.getDbEntity("PAINTING_INFO");

        if (paintingInfo != null) {
            DbAttribute textReview = (DbAttribute) paintingInfo
                    .getAttribute("TEXT_REVIEW");
            textReview.setType(Types.VARCHAR);
            textReview.setMaxLength(255);
        }
    }
}
