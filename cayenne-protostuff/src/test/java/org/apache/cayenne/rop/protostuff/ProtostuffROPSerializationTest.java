/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.rop.protostuff;

import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.rop.ROPSerializationService;
import org.apache.cayenne.rop.protostuff.persistent.ClientMtTable1;
import org.apache.cayenne.rop.protostuff.persistent.ClientMtTable2;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class ProtostuffROPSerializationTest extends ProtostuffProperties {

    private static final String GLOBAL_ATTRIBUTE1 = "Test table1";
    private static final String GLOBAL_ATTRIBUTE2 = "Test table2";

    private ClientMtTable1 table1;
    private ClientMtTable2 table2;

    private Date oldDate;
    private LocalDate localDate;
    private LocalTime localTime;
    private LocalDateTime localDateTime;

    private ROPSerializationService clientService;
    private ROPSerializationService serverService;

    @Before
    public void setUpData() throws Exception {
        oldDate = new Date();
        localDate = LocalDate.now();
        localTime = LocalTime.now();
        localDateTime = LocalDateTime.now();

        table1 = new ClientMtTable1();
        table1.setGlobalAttribute(GLOBAL_ATTRIBUTE1);
        table1.setOldDateAttribute(oldDate);
        table1.setDateAttribute(localDate);
        table1.setTimeAttribute(localTime);
        table1.setTimestampAttribute(localDateTime);

        table2 = new ClientMtTable2();
        table2.setTable1(table1);
        table2.setGlobalAttribute(GLOBAL_ATTRIBUTE2);

        clientService = new ProtostuffROPSerializationService();
        serverService = new ProtostuffROPSerializationService();
    }

    @Test
    public void testByteArraySerialization() throws Exception {
        // test client to server serialization
        byte[] data = clientService.serialize(table2);
        ClientMtTable2 serverTable2 = serverService.deserialize(data, ClientMtTable2.class);

        assertCorrectness(serverTable2);

        // test server to client serialization
        data = serverService.serialize(table2);
        ClientMtTable2 clientTable2 = clientService.deserialize(data, ClientMtTable2.class);

        assertCorrectness(clientTable2);
    }

    @Test
    public void testStreamSerialization() throws Exception {
        // test client to server serialization
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        clientService.serialize(table2, out);
        out.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ClientMtTable2 serverTable2 = serverService.deserialize(in, ClientMtTable2.class);

        assertCorrectness(serverTable2);

        // test server to client serialization
        out = new ByteArrayOutputStream();
        serverService.serialize(table2, out);
        out.flush();

        in = new ByteArrayInputStream(out.toByteArray());
        ClientMtTable2 clientTable2 = clientService.deserialize(in, ClientMtTable2.class);

        assertCorrectness(clientTable2);
    }

    @Test
    public void testColumnQuerySerialization() throws Exception {
        ColumnSelect<String> select = ObjectSelect.columnQuery(ClientMtTable1.class, ClientMtTable1.GLOBAL_ATTRIBUTE);

        byte[] data = serverService.serialize(select);
        @SuppressWarnings("unchecked")
        ColumnSelect<Integer> clone = (ColumnSelect<Integer>) serverService.deserialize(data, ColumnSelect.class);

        assertEquals(ClientMtTable1.GLOBAL_ATTRIBUTE.getExpression(), clone.getColumns().iterator().next().getExpression());
    }

    private void assertCorrectness(ClientMtTable2 table2) {
        ClientMtTable1 table1 = table2.getTable1();
        assertEquals(GLOBAL_ATTRIBUTE2, table2.getGlobalAttribute());
        assertEquals(GLOBAL_ATTRIBUTE1, table1.getGlobalAttribute());
        assertEquals(oldDate, table1.getOldDateAttribute());
        assertEquals(localDate, table1.getDateAttribute());
        assertEquals(localTime, table1.getTimeAttribute());
        assertEquals(localDateTime, table1.getTimestampAttribute());
    }

}
