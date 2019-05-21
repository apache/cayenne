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

/**
 * @since 4.1
 */
public final class ArrayUtil {

    public static int[][] sliceArray(int[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new int[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        int[][] result = new int[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new int[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    public static long[][] sliceArray(long[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new long[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        long[][] result = new long[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new long[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    public static float[][] sliceArray(float[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new float[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        float[][] result = new float[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new float[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    public static double[][] sliceArray(double[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new double[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        double[][] result = new double[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new double[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    public static short[][] sliceArray(short[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new short[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        short[][] result = new short[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new short[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    public static char[][] sliceArray(char[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new char[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        char[][] result = new char[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new char[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    public static boolean[][] sliceArray(boolean[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new boolean[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        boolean[][] result = new boolean[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new boolean[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    public static byte[][] sliceArray(byte[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new byte[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        byte[][] result = new byte[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new byte[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }

    public static Object[][] sliceArray(Object[] array, int batchSize) {
        if(array == null) {
            return null;
        }
        int length = array.length;

        if(length <= batchSize) {
            return new Object[][]{array};
        }

        int batches = length / batchSize;
        if(length % batchSize > 0) {
            batches++;
        }

        Object[][] result = new Object[batches][];
        int offset = 0;
        for(int i=0; i<batches; i++) {
            int nextSize = i < batches - 1 ? batchSize : length - offset;
            result[i] = new Object[nextSize];
            System.arraycopy(array, offset, result[i], 0, nextSize);
            offset += nextSize;
        }
        return result;
    }
}
