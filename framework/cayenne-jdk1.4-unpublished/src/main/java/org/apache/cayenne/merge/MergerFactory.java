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
package org.apache.cayenne.merge;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * A way to create {@link MergerToken} that work for a specific {@link DbAdapter}
 * 
 * @see DbAdapter#mergerFactory()
 */
public class MergerFactory {

    public MergerToken createCreateTable(MergeDirection direction, DbEntity entity) {
        return new CreateTable(direction, entity);
    }

    public MergerToken createDropTable(MergeDirection direction, DbEntity entity) {
        return new DropTable(direction, entity);
    }

    public MergerToken createAddColumn(MergeDirection direction, DbEntity entity, DbAttribute column) {
        return new AddColumn(direction, entity, column);
    }

    public MergerToken createSetNotNull(MergeDirection direction, DbEntity entity, DbAttribute column) {
        return new SetNotNull(direction, entity, column);
    }

    public MergerToken createSetAllowNull(MergeDirection direction, DbEntity entity, DbAttribute column) {
        return new SetAllowNull(direction, entity, column);
    }

    public MergerToken createDropColum(MergeDirection direction, DbEntity entity, DbAttribute column) {
        return new DropColumn(direction, entity, column);
    }
    
    public MergerToken createSetColumnType(MergeDirection direction, DbEntity entity, DbAttribute columnOriginal, DbAttribute columnNew) {
        return new SetColumnType(direction, entity, columnOriginal, columnNew);
    }

}
