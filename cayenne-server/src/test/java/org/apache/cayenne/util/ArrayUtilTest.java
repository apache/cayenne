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

package org.apache.cayenne.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class ArrayUtilTest {

    @Test
    public void sliceIntArray() {
        int[] array = {1, 2, 3, 4, 5, 6, 7};

        int[][] result = ArrayUtil.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new int[]{1, 2}, result[0]);
        assertArrayEquals(new int[]{3, 4}, result[1]);
        assertArrayEquals(new int[]{5, 6}, result[2]);
        assertArrayEquals(new int[]{7},    result[3]);

        int[][] result2 = ArrayUtil.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new int[]{1, 2, 3, 4}, result2[0]);
        assertArrayEquals(new int[]{5, 6, 7}, result2[1]);

        int[][] result3 = ArrayUtil.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0]);

        int[][] result4 = ArrayUtil.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0]);

        int[] array2 = {1, 2, 3, 4, 5, 6, 7, 8};
        int[][] result5 = ArrayUtil.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new int[]{1, 2, 3, 4}, result5[0]);
        assertArrayEquals(new int[]{5, 6, 7, 8}, result5[1]);
    }

    @Test
    public void sliceLongArray() {
        long[] array = {1, 2, 3, 4, 5, 6, 7};

        long[][] result = ArrayUtil.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new long[]{1, 2}, result[0]);
        assertArrayEquals(new long[]{3, 4}, result[1]);
        assertArrayEquals(new long[]{5, 6}, result[2]);
        assertArrayEquals(new long[]{7},    result[3]);

        long[][] result2 = ArrayUtil.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new long[]{1, 2, 3, 4}, result2[0]);
        assertArrayEquals(new long[]{5, 6, 7}, result2[1]);

        long[][] result3 = ArrayUtil.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0]);

        long[][] result4 = ArrayUtil.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0]);

        long[] array2 = {1, 2, 3, 4, 5, 6, 7, 8};
        long[][] result5 = ArrayUtil.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new long[]{1, 2, 3, 4}, result5[0]);
        assertArrayEquals(new long[]{5, 6, 7, 8}, result5[1]);
    }

    @Test
    public void sliceFloatArray() {
        float[] array = {1, 2, 3, 4, 5, 6, 7};

        float[][] result = ArrayUtil.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new float[]{1, 2}, result[0], 0.000001f);
        assertArrayEquals(new float[]{3, 4}, result[1], 0.000001f);
        assertArrayEquals(new float[]{5, 6}, result[2], 0.000001f);
        assertArrayEquals(new float[]{7},    result[3], 0.000001f);

        float[][] result2 = ArrayUtil.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new float[]{1, 2, 3, 4}, result2[0], 0.000001f);
        assertArrayEquals(new float[]{5, 6, 7}, result2[1], 0.000001f);

        float[][] result3 = ArrayUtil.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0], 0.000001f);

        float[][] result4 = ArrayUtil.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0], 0.000001f);

        float[] array2 = {1, 2, 3, 4, 5, 6, 7, 8};
        float[][] result5 = ArrayUtil.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new float[]{1, 2, 3, 4}, result5[0], 0.000001f);
        assertArrayEquals(new float[]{5, 6, 7, 8}, result5[1], 0.000001f);
    }

    @Test
    public void sliceDoubleArray() {
        double[] array = {1, 2, 3, 4, 5, 6, 7};

        double[][] result = ArrayUtil.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new double[]{1, 2}, result[0], 0.000001);
        assertArrayEquals(new double[]{3, 4}, result[1], 0.000001);
        assertArrayEquals(new double[]{5, 6}, result[2], 0.000001);
        assertArrayEquals(new double[]{7},    result[3], 0.000001);

        double[][] result2 = ArrayUtil.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new double[]{1, 2, 3, 4}, result2[0], 0.000001);
        assertArrayEquals(new double[]{5, 6, 7}, result2[1], 0.000001);

        double[][] result3 = ArrayUtil.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0], 0.000001);

        double[][] result4 = ArrayUtil.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0], 0.000001);

        double[] array2 = {1, 2, 3, 4, 5, 6, 7, 8};
        double[][] result5 = ArrayUtil.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new double[]{1, 2, 3, 4}, result5[0], 0.000001);
        assertArrayEquals(new double[]{5, 6, 7, 8}, result5[1], 0.000001);
    }

    @Test
    public void sliceCharArray() {
        char[] array = {1, 2, 3, 4, 5, 6, 7};

        char[][] result = ArrayUtil.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new char[]{1, 2}, result[0]);
        assertArrayEquals(new char[]{3, 4}, result[1]);
        assertArrayEquals(new char[]{5, 6}, result[2]);
        assertArrayEquals(new char[]{7},    result[3]);

        char[][] result2 = ArrayUtil.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new char[]{1, 2, 3, 4}, result2[0]);
        assertArrayEquals(new char[]{5, 6, 7}, result2[1]);

        char[][] result3 = ArrayUtil.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0]);

        char[][] result4 = ArrayUtil.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0]);

        char[] array2 = {1, 2, 3, 4, 5, 6, 7, 8};
        char[][] result5 = ArrayUtil.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new char[]{1, 2, 3, 4}, result5[0]);
        assertArrayEquals(new char[]{5, 6, 7, 8}, result5[1]);
    }

    @Test
    public void sliceShortArray() {
        short[] array = {1, 2, 3, 4, 5, 6, 7};

        short[][] result = ArrayUtil.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new short[]{1, 2}, result[0]);
        assertArrayEquals(new short[]{3, 4}, result[1]);
        assertArrayEquals(new short[]{5, 6}, result[2]);
        assertArrayEquals(new short[]{7},    result[3]);

        short[][] result2 = ArrayUtil.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new short[]{1, 2, 3, 4}, result2[0]);
        assertArrayEquals(new short[]{5, 6, 7}, result2[1]);

        short[][] result3 = ArrayUtil.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0]);

        short[][] result4 = ArrayUtil.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0]);

        short[] array2 = {1, 2, 3, 4, 5, 6, 7, 8};
        short[][] result5 = ArrayUtil.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new short[]{1, 2, 3, 4}, result5[0]);
        assertArrayEquals(new short[]{5, 6, 7, 8}, result5[1]);
    }

    @Test
    public void sliceByteArray() {
        byte[] array = {1, 2, 3, 4, 5, 6, 7};

        byte[][] result = ArrayUtil.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new byte[]{1, 2}, result[0]);
        assertArrayEquals(new byte[]{3, 4}, result[1]);
        assertArrayEquals(new byte[]{5, 6}, result[2]);
        assertArrayEquals(new byte[]{7},    result[3]);

        byte[][] result2 = ArrayUtil.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new byte[]{1, 2, 3, 4}, result2[0]);
        assertArrayEquals(new byte[]{5, 6, 7}, result2[1]);

        byte[][] result3 = ArrayUtil.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0]);

        byte[][] result4 = ArrayUtil.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0]);

        byte[] array2 = {1, 2, 3, 4, 5, 6, 7, 8};
        byte[][] result5 = ArrayUtil.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new byte[]{1, 2, 3, 4}, result5[0]);
        assertArrayEquals(new byte[]{5, 6, 7, 8}, result5[1]);
    }

    @Test
    public void sliceBooleanArray() {
        boolean[] array = {true, true, false, true, false, false, true};

        boolean[][] result = ArrayUtil.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new boolean[]{true, true}, result[0]);
        assertArrayEquals(new boolean[]{false, true}, result[1]);
        assertArrayEquals(new boolean[]{false, false}, result[2]);
        assertArrayEquals(new boolean[]{true},    result[3]);

        boolean[][] result2 = ArrayUtil.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new boolean[]{true, true, false, true}, result2[0]);
        assertArrayEquals(new boolean[]{false, false, true}, result2[1]);

        boolean[][] result3 = ArrayUtil.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0]);

        boolean[][] result4 = ArrayUtil.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0]);

        boolean[] array2 = {true, true, false, true, false, false, true, false};
        boolean[][] result5 = ArrayUtil.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new boolean[]{true, true, false, true}, result5[0]);
        assertArrayEquals(new boolean[]{false, false, true, false}, result5[1]);
    }

    @Test
    public void sliceObjectArray() {
        Object[] array = {1, 2, 3, 4, 5, 6, 7};

        Object[][] result = ArrayUtil.sliceArray(array, 2);

        assertEquals(4, result.length);
        assertArrayEquals(new Object[]{1, 2}, result[0]);
        assertArrayEquals(new Object[]{3, 4}, result[1]);
        assertArrayEquals(new Object[]{5, 6}, result[2]);
        assertArrayEquals(new Object[]{7},    result[3]);

        Object[][] result2 = ArrayUtil.sliceArray(array, 4);

        assertEquals(2, result2.length);
        assertArrayEquals(new Object[]{1, 2, 3, 4}, result2[0]);
        assertArrayEquals(new Object[]{5, 6, 7}, result2[1]);

        Object[][] result3 = ArrayUtil.sliceArray(array, 7);

        assertEquals(1, result3.length);
        assertArrayEquals(array, result3[0]);

        Object[][] result4 = ArrayUtil.sliceArray(array, 10);

        assertEquals(1, result4.length);
        assertArrayEquals(array, result4[0]);

        Object[] array2 = {1, 2, 3, 4, 5, 6, 7, 8};
        Object[][] result5 = ArrayUtil.sliceArray(array2, 4);

        assertEquals(2, result5.length);
        assertArrayEquals(new Object[]{1, 2, 3, 4}, result5[0]);
        assertArrayEquals(new Object[]{5, 6, 7, 8}, result5[1]);
    }
}