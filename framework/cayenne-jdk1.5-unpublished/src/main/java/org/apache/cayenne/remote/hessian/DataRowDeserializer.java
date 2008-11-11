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

package org.apache.cayenne.remote.hessian;

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractMapDeserializer;

/**
 * Client side deserilaizer of DataRows.
 * 
 * @since 1.2
 */
class DataRowDeserializer extends AbstractMapDeserializer {

    protected Field versionField;

    DataRowDeserializer() {
        try {
            versionField = DataRow.class.getDeclaredField("version");
        }
        catch (Exception e) {
            throw new CayenneRuntimeException(
                    "Error building deserializer for DataRow",
                    e);
        }

        versionField.setAccessible(true);
    }

    @Override
    public Class<?> getType() {
        return DataRow.class;
    }

    @Override
    public Object readMap(AbstractHessianInput in) throws IOException {

        int size = in.readInt();
        DataRow row = new DataRow(size);
        try {
            versionField.set(row, new Long(in.readLong()));
        }
        catch (Exception e) {
            throw new IOException("Error reading 'version' field");
        }

        row.setReplacesVersion(in.readLong());
        in.addRef(row);

        while (!in.isEnd()) {
            row.put((String) in.readObject(), in.readObject());
        }

        in.readEnd();

        return row;
    }
}
