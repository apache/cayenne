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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.MappedSelect;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.return_types.ReturnTypesMap1;
import org.apache.cayenne.testdo.return_types.ReturnTypesMap2;
import org.apache.cayenne.testdo.return_types.ReturnTypesMapLobs1;
import org.apache.cayenne.unit.PostgresUnitDbAdapter;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

/**
 * Test Types mapping for selected columns
 */
@UseServerRuntime(CayenneProjects.RETURN_TYPES_PROJECT)
public class ReturnTypesMappingIT extends ServerCase {

    @Inject
    private DataContext context;
    
    @Inject
    private UnitDbAdapter unitDbAdapter;

    /*
     * TODO: olga: We need divided TYPES_MAPPING_TES2 to 2 schemas with lobs columns and not lobs columns 
     */

    @Test
    public void testBIGINT() throws Exception {
        String columnName = "BIGINT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Long bigintValue = 5326457654783454355l;
        test.setBigintColumn(bigintValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Long.class, columnValue.getClass());
        assertEquals(bigintValue, columnValue);
    }

    @Test
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

    @Test
    public void testBINARY() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            String columnName = "BINARY_COLUMN";
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] binaryValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setBinaryColumn(binaryValue);
            context.commitChanges();
			
            DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap2")).get(0);
            Object columnValue = testRead.get(columnName);
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(binaryValue, (byte[]) columnValue));
        }
    }

    @Test
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

    @Test
    public void testBIT() throws Exception {
        String columnName = "BIT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Boolean bitValue = true;
        test.setBitColumn(bitValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertTrue(Boolean.class.equals(columnValue.getClass())
                || Short.class.equals(columnValue.getClass()));
        assertTrue(bitValue.equals(columnValue) || ((Number) columnValue).intValue() == 1);
    }

    @Test
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

    @Test
    public void testBLOB() throws Exception {
        assumeTrue("In postresql blob_column has OID type, but in JAVA it converts into long not into byte.",
                !(unitDbAdapter instanceof PostgresUnitDbAdapter));

        if (unitDbAdapter.supportsLobs()) {
            String columnName = "BLOB_COLUMN";
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] blobValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setBlobColumn(blobValue);
            context.commitChanges();
			
            DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap2")).get(0);
            Object columnValue = testRead.get(columnName);
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(blobValue, (byte[]) columnValue));
        }
    }

    @Test
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

    @Test
    public void testBOOLEAN() throws Exception {
        String columnName = "BOOLEAN_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Boolean booleanValue = true;
        test.setBooleanColumn(booleanValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertTrue(Boolean.class.equals(columnValue.getClass())
                || Short.class.equals(columnValue.getClass()));
        assertTrue(booleanValue.equals(columnValue)
                || ((Number) columnValue).intValue() == 1);
    }

    @Test
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

    @Test
    public void testCHAR() throws Exception {
        String columnName = "CHAR_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        String charValue = "Char string for tests!";
        test.setCharColumn(charValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(charValue, columnValue);
    }

    @Test
    public void testNCHAR() throws Exception {
        String columnName = "NCHAR_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        String charValue = "درخت‌های جستجوی متوازن، نیازی ندارد که به صورت!";
        test.setNCharColumn(charValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(charValue, columnValue);
    }

    @Test
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

    @Test
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
			
            DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesLobsMap1")).get(0);
            Object columnValue = testRead.get(columnName);
            if (columnValue == null && testRead.containsKey(columnName.toLowerCase())) {
                columnValue = testRead.get(columnName.toLowerCase());
            }
            assertNotNull(columnValue);
            assertEquals(String.class, columnValue.getClass());
            assertEquals(clobValue, columnValue);
        }
    }

    @Test
    public void testNCLOB() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            String columnName = "NCLOB_COLUMN";
            ReturnTypesMapLobs1 test = context.newObject(ReturnTypesMapLobs1.class);

            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                buffer.append("رودالف بیر و دد مک‌کرِیت درخت بی را زمانی که در شرکت بوئینگ [۱]، مشغول به کار بودند ابداع نمودند، اما حرف B واقعاً\" از کجا آمده؟ داگلاس کامر یک سری از احتمالات را پیشنهاد کرد:\n" +
                        "\"Balanced,\" \"Broad,\" یا \"Bushy\" ممکن است استفاده شده‌باشند [چون همهٔ برگ‌ها در یک سطح قرار دارند]. دیگران اظهار داشتند که حرف \"B\" از کلمهٔ بوئینگ گرفته شده است [به این دلیل که پدیدآوردنده درسال 1972 در آزمایشگاه‌های تحقیقاتی علمی شرکت بوئینگ کار می‌کرد]. با این وجود پنداشتن درخت بی به عنوان درخت \"بِیِر\" نیز درخور است.[۲]");
            }
            String clobValue = buffer.toString();
            test.setNClobColumn(clobValue);
            context.commitChanges();
			
            DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesLobsMap1")).get(0);
            Object columnValue = testRead.get(columnName);
            if (columnValue == null && testRead.containsKey(columnName.toLowerCase())) {
                columnValue = testRead.get(columnName.toLowerCase());
            }
            assertNotNull(columnValue);
            assertEquals(String.class, columnValue.getClass());
            assertEquals(clobValue, columnValue);
        }
    }

    @Test
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

    @Test
    public void testDATE() throws Exception {
        String columnName = "DATE_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2002, 1, 1);
        Date dateValue = cal.getTime();
        test.setDateColumn(dateValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Date.class, columnValue.getClass());
        assertEquals(dateValue.toString(), columnValue.toString());
    }

    @Test
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

    @Test
    public void testDECIMAL() throws Exception {
        String columnName = "DECIMAL_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        BigDecimal decimalValue = new BigDecimal("578438.57843");
        test.setDecimalColumn(decimalValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(BigDecimal.class, columnValue.getClass());
        assertEquals(decimalValue, columnValue);
    }

    @Test
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

    @Test
    public void testDOUBLE() throws Exception {
        String columnName = "DOUBLE_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Double doubleValue = 3298.4349783d;
        test.setDoubleColumn(doubleValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Double.class, columnValue.getClass());
        assertEquals(doubleValue, columnValue);
    }

    @Test
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

    @Test
    public void testFLOAT() throws Exception {
        String columnName = "FLOAT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Float floatValue = 375.437f;
        test.setFloatColumn(floatValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertTrue(Float.class.equals(columnValue.getClass())
                || Double.class.equals(columnValue.getClass()));
        assertEquals(floatValue.floatValue(), ((Number)columnValue).floatValue(), 0);
    }

    @Test
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

    @Test
    public void testINTEGER() throws Exception {
        String columnName = "INTEGER_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Integer integerValue = 54235;
        test.setIntegerColumn(integerValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Integer.class, columnValue.getClass());
        assertEquals(integerValue, columnValue);
    }

    @Test
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

    @Test
    public void testLONGVARBINARY() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            String columnName = "LONGVARBINARY_COLUMN";
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] longvarbinaryValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setLongvarbinaryColumn(longvarbinaryValue);
            context.commitChanges();
			
            DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap2")).get(0);
            Object columnValue = testRead.get(columnName);
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(longvarbinaryValue, (byte[]) columnValue));
        }
    }

    @Test
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

    @Test
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
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(longvarcharValue, columnValue);
    }

    @Test
    public void testLONGNVARCHAR() throws Exception {
        String columnName = "LONGNVARCHAR_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < 500; i++) {
            buffer.append("ی متوازن، نیازی ندارد که ب large string for tests!!!!\n");
        }
        String longnvarcharValue = buffer.toString();
        test.setLongnvarcharColumn(longnvarcharValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(longnvarcharValue, columnValue);
    }

    @Test
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

    @Test
    public void testNUMERIC() throws Exception {
        String columnName = "NUMERIC_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        BigDecimal numericValue = new BigDecimal("578438.57843");
        test.setNumericColumn(numericValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(BigDecimal.class, columnValue.getClass());
        assertEquals(numericValue, columnValue);
    }

    @Test
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

    @Test
    public void testREAL() throws Exception {
        String columnName = "REAL_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Float realValue = 5788.57843f;
        test.setRealColumn(realValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
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

    @Test
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

    @Test
    public void testSMALLINT() throws Exception {
        String columnName = "SMALLINT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Short smallintValue = 32564;
        test.setSmallintColumn(smallintValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Short.class, columnValue.getClass());
        assertEquals(smallintValue, columnValue);
    }

    @Test
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

    @Test
    public void testTIME() throws Exception {
        String columnName = "TIME_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2003, 1, 1, 1, 20, 30);
        Date timeValue = new Time(cal.getTime().getTime());
        test.setTimeColumn(timeValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Date.class, columnValue.getClass());
        assertEquals(timeValue.toString(), new Time(((Date) columnValue).getTime())
                .toString());
    }

    @Test
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

    @Test
    public void testTIMESTAMP() throws Exception {
        String columnName = "TIMESTAMP_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Date timestampValue = Calendar.getInstance().getTime();
        test.setTimestampColumn(timestampValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(Date.class, columnValue.getClass());
        
        // some DB's, noteably MySQL, strip the milliseconds from timestamps,
        // so comparing within 1 second precision
        long delta = timestampValue.getTime() - ((Date) columnValue).getTime();
        assertTrue(delta < 1000);
    }

    @Test
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
        
        // some DB's, noteably MySQL, strip the milliseconds from timestamps,
        // so comparing within 1 second precision
        long delta = timestampValue.getTime() - ((Date) columnValue).getTime();
        assertTrue(delta < 1000);    }

    @Test
    public void testTINYINT() throws Exception {
        String columnName = "TINYINT_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        Byte tinyintValue = 89;
        test.setTinyintColumn(tinyintValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertTrue(Short.class.equals(columnValue.getClass()));
        assertEquals(tinyintValue.intValue(), ((Number)columnValue).intValue());
    }

    @Test
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

    @Test
    public void testVARBINARY() throws Exception {
        if (unitDbAdapter.supportsLobs()) {
            String columnName = "VARBINARY_COLUMN";
            ReturnTypesMap2 test = context.newObject(ReturnTypesMap2.class);
    
            byte[] varbinaryValue = {
                    3, 4, 5, -6, 7, 0, 2, 9, 45, 64, 3, 127, -128, -60
            };
            test.setVarbinaryColumn(varbinaryValue);
            context.commitChanges();
			
            DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap2")).get(0);
            Object columnValue = testRead.get(columnName);
            assertNotNull(columnValue);
            assertEquals(byte[].class, columnValue.getClass());
            assertTrue(Arrays.equals(varbinaryValue, (byte[]) columnValue));
        }
    }

    @Test
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

    @Test
    public void testVARCHAR() throws Exception {
        String columnName = "VARCHAR_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        String varcharValue = "VARChar string for tests!";
        test.setVarcharColumn(varcharValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(varcharValue, columnValue);
    }

    @Test
    public void testNVARCHAR() throws Exception {
        String columnName = "NVARCHAR_COLUMN";
        ReturnTypesMap1 test = context.newObject(ReturnTypesMap1.class);

        String varcharValue = "ی متوازن، نیازی ندارد که ب";
        test.setNVarcharColumn(varcharValue);
        context.commitChanges();
		
        DataRow testRead = (DataRow) context.performQuery(MappedSelect.query("SelectReturnTypesMap1")).get(0);
        Object columnValue = testRead.get(columnName);
        assertNotNull(columnValue);
        assertEquals(String.class, columnValue.getClass());
        assertEquals(varcharValue, columnValue);
    }

    @Test
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
