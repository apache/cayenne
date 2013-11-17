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

package org.apache.cayenne.dba.oracle;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 */
class Oracle8LOBBatchAction extends OracleLOBBatchAction {

    Oracle8LOBBatchAction(BatchQuery query, JdbcAdapter adapter) {
        super(query, adapter);
    }

    /**
     * Override the Oracle writeBlob() method to be compatible with Oracle8 drivers.
     */
    @Override
    protected void writeBlob(Blob blob, byte[] value) {
        // Fix for CAY-1307.  For Oracle8, get the method found by reflection in
        // OracleAdapter.  (Code taken from Cayenne 2.)
        Method getBinaryStreamMethod = Oracle8Adapter.getOutputStreamFromBlobMethod();
        try {
            OutputStream out = (OutputStream) getBinaryStreamMethod.invoke(blob, (Object[]) null);
            try {
                out.write(value);
                out.flush();
            }
            finally {
                out.close();
            }
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error processing BLOB.", Util
                    .unwindException(e));
        }
    }

    /**
     * Override the Oracle writeClob() method to be compatible with Oracle8 drivers.
     */
    @Override
    protected void writeClob(Clob clob, char[] value) {
        Method getWriterMethod = Oracle8Adapter.getWriterFromClobMethod();
        try {
            Writer out = (Writer) getWriterMethod.invoke(clob, (Object[]) null);
            try {
                out.write(value);
                out.flush();
            }
            finally {
                out.close();
            }

        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error processing CLOB.", Util
                    .unwindException(e));
        }
    }

    /**
     * Override the Oracle writeClob() method to be compatible with Oracle8 drivers.
     */
    @Override
    protected void writeClob(Clob clob, String value) {
        Method getWriterMethod = Oracle8Adapter.getWriterFromClobMethod();
        try {
            Writer out = (Writer) getWriterMethod.invoke(clob, (Object[]) null);
            try {
                out.write(value);
                out.flush();
            }
            finally {
                out.close();
            }
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error processing CLOB.", Util
                    .unwindException(e));
        }
    }
}
