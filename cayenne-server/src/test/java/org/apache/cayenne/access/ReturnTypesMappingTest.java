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
package org.apache.cayenne.access;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.DateTestEntity;
import org.apache.cayenne.testdo.testmap.ReturnTypesMap1;
import org.apache.cayenne.testdo.testmap.ReturnTypesMap2;
import org.apache.cayenne.testdo.testmap.ReturnTypesMapLobs1;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Test Types mapping for selected columns
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ReturnTypesMappingTest extends ServerCase {

    @Inject
    private DataContext context;
    
    @Inject
    private DBHelper dbHelper;
    
    @Inject
    private UnitDbAdapter unitDbAdapter;
    
    @Override
    protected void setUpAfterInjection() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            dbHelper.deleteAll("TYPES_MAPPING_LOBS_TEST1");
            dbHelper.deleteAll("TYPES_MAPPING_TEST2");
        }
        dbHelper.deleteAll("TYPES_MAPPING_TEST1");
        dbHelper.deleteAll("DATE_TEST");
    }

    /*
     * TODO: olga: We need divided TYPES_MAPPING_TES2 to 2 schemas with lobs columns and not lobs columns 
     */
    
    public void testBIGINT() throws Exception {
        String columnName = "BIGINT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Long bigintValue = 5326457654783454355l;
        test.setBigintColumn(bigintValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Long.class, columnValue.getClass());
        assertEquals(bigintValue, columnValue);
    }

    public void testBIGINT2() throws Exception {
       ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);
    
        Long bigintValue = 5326457654783454355l;
        test.setBigintColumn(bigintValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Long columnValue = testRead.getBigintColumn();
        assertNotNull(columnValue);
        assertEquals(Long.class, columnValue.getClass());
        assertEquals(bigintValue, columnValue);
    }

    public void testBINARY() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            String columnName = "BINARY_COLUMN";
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] binaryValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setBinaryColumn(binaryValue);
            context.commitChanges();
    
            NamedQuery q = new NamedQuery("SelectReturnTypesMap2");
            DataRow testRead = (DataRow) context.performQuery(q).get(0);
            Object columnValue = testRead.get(columnName);
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(binaryValue, (byte[]) columnValue));
        }
    }

    public void testBINARY2() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] binaryValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setBinaryColumn(binaryValue);
            context.commitChanges();
    
            SelectQuery q = new SelectQuery(ReturnTypesMap2.class);
            ReturnTypesMap2 testRead = (ReturnTypesMap2) context.performQuery(q).get(0);
            byte[] columnValue = testRead.getBinaryColumn();
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(binaryValue, columnValue));
        }
    }

    public void testBIT() throws Exception {
        String columnName = "BIT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Boolean bitValue = true;
        test.setBitColumn(bitValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertTrue(Boolean.class.equals(columnValue.getClass())
                || Short.class.equals(columnValue.getClass()));
        assertTrue(bitValue.equals(columnValue) || ((Number) columnValue).intValue() == 1);
    }

    public void testBIT2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Boolean bitValue = true;
        test.setBitColumn(bitValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Boolean columnValue = testRead.getBitColumn();
        assertNotNull(columnValue);
        assertEquals(Boolean.class, columnValue.getClass());
        assertEquals(bitValue, columnValue);
    }

    public void testBLOB() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            String columnName = "BLOB_COLUMN";
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] blobValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setBlobColumn(blobValue);
            context.commitChanges();
    
            NamedQuery q = new NamedQuery("SelectReturnTypesMap2");
            DataRow testRead = (DataRow) context.performQuery(q).get(0);
            Object columnValue = testRead.get(columnName);
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(blobValue, (byte[]) columnValue));
        }
    }

    public void testBLOB2() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] blobValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setBlobColumn(blobValue);
            context.commitChanges();
    
            SelectQuery q = new SelectQuery(ReturnTypesMap2.class);
            ReturnTypesMap2 testRead = (ReturnTypesMap2) context.performQuery(q).get(0);
            byte[] columnValue = testRead.getBlobColumn();
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(blobValue, columnValue));
        }
    }

    public void testBOOLEAN() throws Exception {
        String columnName = "BOOLEAN_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Boolean booleanValue = true;
        test.setBooleanColumn(booleanValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertTrue(Boolean.class.equals(columnValue.getClass())
                || Short.class.equals(columnValue.getClass()));
        assertTrue(booleanValue.equals(columnValue)
                || ((Number) columnValue).intValue() == 1);
    }

    public void testBOOLEAN2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Boolean booleanValue = true;
        test.setBooleanColumn(booleanValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Boolean columnValue = testRead.getBooleanColumn();
        assertNotNull(columnValue);
        assertEquals(Boolean.class, columnValue.getClass());
        assertEquals(booleanValue, columnValue);
    }

    public void testCHAR() throws Exception {
        String columnName = "CHAR_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        String charValue = "Char string for tests!";
        test.setCharColumn(charValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(charValue, columnValue);
    }

    public void testCHAR2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        String charValue = "Char string for tests!";
        test.setCharColumn(charValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        String columnValue = testRead.getCharColumn();
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(charValue, columnValue);
    }

    public void testCLOB() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            String columnName = "CLOB_COLUMN";
            ReturnTypesMapLobs1 test = context.newObject(ReturnTypesMapLobs1.class);
    
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < 10000; i++) {
                buffer.append("CLOB very large string for tests!!!!\n");
            }
            String clobValue = buffer.toString();
            test.setClobColumn(clobValue);
            context.commitChanges();
    
            NamedQuery q = new NamedQuery("SelectReturnTypesLobsMap1");
            DataRow testRead = (DataRow) context.performQuery(q).get(0);
            Object columnValue = testRead.get(columnName);
            if (columnValue == null && testRead.containsKey(columnName.toLowerCase())) {
                columnValue = testRead.get(columnName.toLowerCase());
            }
            assertNotNull(columnValue);
            assertEquals(String.class, columnValue.getClass());
            assertEquals(clobValue, columnValue);
        }
    }

    public void testCLOB2() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            ReturnTypesMapLobs1 test = context.newObject(ReturnTypesMapLobs1.class);
    
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < 10000; i++) {
                buffer.append("CLOB very large string for tests!!!!\n");
            }
            String clobValue = buffer.toString();
            test.setClobColumn(clobValue);
            context.commitChanges();
    
            SelectQuery q = new SelectQuery(ReturnTypesMapLobs1.class);
            ReturnTypesMapLobs1 testRead = (ReturnTypesMapLobs1) context.performQuery(q).get(0);
            String columnValue = testRead.getClobColumn();
            assertNotNull(columnValue);
            assertEquals(String.class, columnValue.getClass());
            assertEquals(clobValue, columnValue);
        }
    }

    public void testDATE() throws Exception {
        String columnName = "DATE_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2002, 1, 1);
        Date dateValue = cal.getTime();
        test.setDateColumn(dateValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Date.class, columnValue.getClass());
        assertEquals(dateValue.toString(), columnValue.toString());
    }

    public void testDATE2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2002, 1, 1);
        Date dateValue = cal.getTime();
        test.setDateColumn(dateValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Date columnValue = testRead.getDateColumn();
        assertNotNull(columnValue);
        assertEquals(Date.class, columnValue.getClass());
        assertEquals(dateValue.toString(), columnValue.toString());
    }

    public void testDECIMAL() throws Exception {
        String columnName = "DECIMAL_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        BigDecimal decimalValue = new BigDecimal("578438.57843");
        test.setDecimalColumn(decimalValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(BigDecimal.class, columnValue.getClass());
        assertEquals(decimalValue, columnValue);
    }

    public void testDECIMAL2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        BigDecimal decimalValue = new BigDecimal("578438.57843");
        test.setDecimalColumn(decimalValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        BigDecimal columnValue = testRead.getDecimalColumn();
        assertNotNull(columnValue);
        assertEquals(BigDecimal.class, columnValue.getClass());
        assertEquals(decimalValue, columnValue);
    }

    public void testDOUBLE() throws Exception {
        String columnName = "DOUBLE_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Double doubleValue = 3298.4349783d;
        test.setDoubleColumn(doubleValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Double.class, columnValue.getClass());
        assertEquals(doubleValue, columnValue);
    }

    public void testDOUBLE2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Double doubleValue = 3298.4349783d;
        test.setDoubleColumn(doubleValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Double columnValue = testRead.getDoubleColumn();
        assertNotNull(columnValue);
        assertEquals(Double.class, columnValue.getClass());
        assertEquals(doubleValue, columnValue);
    }

    public void testFLOAT() throws Exception {
        String columnName = "FLOAT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Float floatValue = 375.437f;
        test.setFloatColumn(floatValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertTrue(Float.class.equals(columnValue.getClass())
                || Double.class.equals(columnValue.getClass()));
        assertEquals(floatValue.floatValue(), ((Number)columnValue).floatValue());
    }

    public void testFLOAT2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Float floatValue = 375.437f;
        test.setFloatColumn(floatValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Float columnValue = testRead.getFloatColumn();
        assertNotNull(columnValue);
        assertEquals(Float.class, columnValue.getClass());
        assertEquals(floatValue, columnValue);
    }

    public void testINTEGER() throws Exception {
        String columnName = "INTEGER_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Integer integerValue = 54235;
        test.setIntegerColumn(integerValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Integer.class, columnValue.getClass());
        assertEquals(integerValue, columnValue);
    }

    public void testINTEGER2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Integer integerValue = 54235;
        test.setIntegerColumn(integerValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Integer columnValue = testRead.getIntegerColumn();
        assertNotNull(columnValue);
        assertEquals(Integer.class, columnValue.getClass());
        assertEquals(integerValue, columnValue);
    }

    public void testLONGVARBINARY() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            String columnName = "LONGVARBINARY_COLUMN";
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] longvarbinaryValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setLongvarbinaryColumn(longvarbinaryValue);
            context.commitChanges();
    
            NamedQuery q = new NamedQuery("SelectReturnTypesMap2");
            DataRow testRead = (DataRow) context.performQuery(q).get(0);
            Object columnValue = testRead.get(columnName);
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(longvarbinaryValue, (byte[]) columnValue));
        }
    }

    public void testLONGVARBINARY2() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] longvarbinaryValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setLongvarbinaryColumn(longvarbinaryValue);
            context.commitChanges();
    
            SelectQuery q = new SelectQuery(ReturnTypesMap2.class);
            ReturnTypesMap2 testRead = (ReturnTypesMap2) context.performQuery(q).get(0);
            byte[] columnValue = testRead.getLongvarbinaryColumn();
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(longvarbinaryValue, columnValue));
        }
    }

    public void testLONGVARCHAR() throws Exception {
        String columnName = "LONGVARCHAR_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < 500; i++) {
            buffer.append("LONGVARCHAR large string for tests!!!!\n");
        }
        String longvarcharValue = buffer.toString();
        test.setLongvarcharColumn(longvarcharValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(longvarcharValue, columnValue);
    }

    public void testLONGVARCHAR2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < 500; i++) {
            buffer.append("LONGVARCHAR large string for tests!!!!\n");
        }
        String longvarcharValue = buffer.toString();
        test.setLongvarcharColumn(longvarcharValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        String columnValue = testRead.getLongvarcharColumn();
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(longvarcharValue, columnValue);
    }

    public void testNUMERIC() throws Exception {
        String columnName = "NUMERIC_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        BigDecimal numericValue = new BigDecimal("578438.57843");
        test.setNumericColumn(numericValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(BigDecimal.class, columnValue.getClass());
        assertEquals(numericValue, columnValue);
    }

    public void testNUMERIC2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        BigDecimal numericValue = new BigDecimal("578438.57843");
        test.setNumericColumn(numericValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        BigDecimal columnValue = testRead.getNumericColumn();
        assertNotNull(columnValue);
        assertEquals(BigDecimal.class, columnValue.getClass());
        assertEquals(numericValue, columnValue);
    }

    public void testREAL() throws Exception {
        String columnName = "REAL_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Float realValue = 5788.57843f;
        test.setRealColumn(realValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);

        // MySQL can treat REAL as either DOUBLE or FLOAT depending on the
        // engine settings
        if (unitDbAdapter.realAsDouble()) {
            assertEquals(Double.class, columnValue.getClass());
            assertEquals(Double.valueOf(realValue), (Double) columnValue, 0.0001);
        } else {
            assertEquals(Float.class, columnValue.getClass());
            assertEquals(realValue, columnValue);
        }
    }

    public void testREAL2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Float realValue = 5788.57843f;
        test.setRealColumn(realValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Float columnValue = testRead.getRealColumn();
        assertNotNull(columnValue);
        assertEquals(Float.class, columnValue.getClass());
        assertEquals(realValue, columnValue);
    }

    public void testSMALLINT() throws Exception {
        String columnName = "SMALLINT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Short smallintValue = 32564;
        test.setSmallintColumn(smallintValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Short.class, columnValue.getClass());
        assertEquals(smallintValue, columnValue);
    }

    public void testSMALLINT2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Short smallintValue = 32564;
        test.setSmallintColumn(smallintValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Short columnValue = testRead.getSmallintColumn();
        assertNotNull(columnValue);
        assertEquals(Short.class, columnValue.getClass());
        assertEquals(smallintValue, columnValue);
    }

    public void testTIME() throws Exception {
        String columnName = "TIME_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 1, 1, 1, 20, 30);
        Date timeValue = new Time(cal.getTime().getTime());
        test.setTimeColumn(timeValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Date.class, columnValue.getClass());
        assertEquals(timeValue.toString(), new Time(((Date) columnValue).getTime())
                .toString());
    }

    public void testTIME2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 1, 1, 1, 20, 30);
        Date timeValue = new Time(cal.getTime().getTime());
        test.setTimeColumn(timeValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Date columnValue = testRead.getTimeColumn();
        assertNotNull(columnValue);
        assertEquals(Date.class, columnValue.getClass());
        assertEquals(timeValue.toString(), new Time(columnValue.getTime()).toString());
    }

    public void testSQLTemplateTime() throws Exception {
        DateTestEntity test = (DateTestEntity) context.newObject("DateTestEntity");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 1, 1, 1, 20, 30);

        // most databases fail millisecond accuracy
        // cal.set(Calendar.MILLISECOND, 55);

        Time now = new Time(cal.getTime().getTime());
        test.setTimeColumn(now);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectDateTest");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Date columnValue = (Date) testRead.get("TIME_COLUMN");
        assertNotNull(testRead.toString(), columnValue);
        assertEquals(now.toString(), new Time(columnValue.getTime()).toString());
    }

    public void testTIMESTAMP() throws Exception {
        String columnName = "TIMESTAMP_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Date timestampValue = Calendar.getInstance().getTime();
        test.setTimestampColumn(timestampValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Date.class, columnValue.getClass());
        assertEquals(timestampValue.toString(), columnValue.toString());
    }

    public void testTIMESTAMP2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Date timestampValue = Calendar.getInstance().getTime();
        test.setTimestampColumn(timestampValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Date columnValue = testRead.getTimestampColumn();
        assertNotNull(columnValue);
        assertEquals(Date.class, columnValue.getClass());
        assertEquals(timestampValue.toString(), columnValue.toString());
    }

    public void testTINYINT() throws Exception {
        String columnName = "TINYINT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Byte tinyintValue = 89;
        test.setTinyintColumn(tinyintValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertTrue(Short.class.equals(columnValue.getClass()));
        assertEquals(tinyintValue.intValue(), ((Number)columnValue).intValue());
    }

    public void testTINYINT2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Byte tinyintValue = 89;
        test.setTinyintColumn(tinyintValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        Byte columnValue = testRead.getTinyintColumn();
        assertNotNull(columnValue);
        assertEquals(Byte.class, columnValue.getClass());
        assertEquals(tinyintValue, columnValue);
    }

    public void testVARBINARY() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            String columnName = "VARBINARY_COLUMN";
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] varbinaryValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setVarbinaryColumn(varbinaryValue);
            context.commitChanges();
    
            NamedQuery q = new NamedQuery("SelectReturnTypesMap2");
            DataRow testRead = (DataRow) context.performQuery(q).get(0);
            Object columnValue = testRead.get(columnName);
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(varbinaryValue, (byte[]) columnValue));
        }
    }

    public void testVARBINARY2() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] varbinaryValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setVarbinaryColumn(varbinaryValue);
            context.commitChanges();
    
            SelectQuery q = new SelectQuery(ReturnTypesMap2.class);
            ReturnTypesMap2 testRead = (ReturnTypesMap2) context.performQuery(q).get(0);
            byte[] columnValue = testRead.getVarbinaryColumn();
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(varbinaryValue, columnValue));
        }
    }

    public void testVARCHAR() throws Exception {
        String columnName = "VARCHAR_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        String varcharValue = "VARChar string for tests!";
        test.setVarcharColumn(varcharValue);
        context.commitChanges();

        NamedQuery q = new NamedQuery("SelectReturnTypesMap1");
        DataRow testRead = (DataRow) context.performQuery(q).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(varcharValue, columnValue);
    }

    public void testVARCHAR2() throws Exception {
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        String varcharValue = "VARChar string for tests!";
        test.setVarcharColumn(varcharValue);
        context.commitChanges();

        SelectQuery q = new SelectQuery(ReturnTypesMap1.class);
        ReturnTypesMap1 testRead = (ReturnTypesMap1) context.performQuery(q).get(0);
        String columnValue = testRead.getVarcharColumn();
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(varcharValue, columnValue);
    }
}
