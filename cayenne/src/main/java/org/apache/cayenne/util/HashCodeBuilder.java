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
 * Assists in implementing {@link Object#hashCode()} methods. The code is based on
 * HashCodeBuilder from commons-lang 2.1.
 * 
 * @since 3.0
 */
public class HashCodeBuilder {

    /**
     * Constant to use in building the hashCode.
     */
    private final int iConstant;
    
    /**
     * Running total of the hashCode.
     */
    private int iTotal = 0;

    /**
     * <p>
     * Uses two hard coded choices for the constants needed to build a
     * <code>hashCode</code>.
     * </p>
     */
    public HashCodeBuilder() {
        iConstant = 37;
        iTotal = 17;
    }

    /**
     * <p>
     * Two randomly chosen, non-zero, odd numbers must be passed in. Ideally these should
     * be different for each class, however this is not vital.
     * </p>
     * <p>
     * Prime numbers are preferred, especially for the multiplier.
     * </p>
     * 
     * @param initialNonZeroOddNumber a non-zero, odd number used as the initial value
     * @param multiplierNonZeroOddNumber a non-zero, odd number used as the multiplier
     * @throws IllegalArgumentException if the number is zero or even
     */
    public HashCodeBuilder(int initialNonZeroOddNumber, int multiplierNonZeroOddNumber) {
        if (initialNonZeroOddNumber == 0) {
            throw new IllegalArgumentException(
                    "HashCodeBuilder requires a non zero initial value");
        }
        if (initialNonZeroOddNumber % 2 == 0) {
            throw new IllegalArgumentException(
                    "HashCodeBuilder requires an odd initial value");
        }
        if (multiplierNonZeroOddNumber == 0) {
            throw new IllegalArgumentException(
                    "HashCodeBuilder requires a non zero multiplier");
        }
        if (multiplierNonZeroOddNumber % 2 == 0) {
            throw new IllegalArgumentException(
                    "HashCodeBuilder requires an odd multiplier");
        }
        iConstant = multiplierNonZeroOddNumber;
        iTotal = initialNonZeroOddNumber;
    }

    /**
     * <p>
     * Adds the result of super.hashCode() to this builder.
     * </p>
     * 
     * @param superHashCode the result of calling <code>super.hashCode()</code>
     * @return this HashCodeBuilder, used to chain calls.
     * @since 2.0
     */
    public HashCodeBuilder appendSuper(int superHashCode) {
        iTotal = iTotal * iConstant + superHashCode;
        return this;
    }

    // -------------------------------------------------------------------------

    /**
     * <p>
     * Append a <code>hashCode</code> for an <code>Object</code>.
     * </p>
     * 
     * @param object the Object to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(Object object) {
        if (object == null) {
            iTotal = iTotal * iConstant;

        }
        else {
            if (object.getClass().isArray() == false) {
                // the simple case, not an array, just the element
                iTotal = iTotal * iConstant + object.hashCode();

            }
            else {
                // 'Switch' on type of array, to dispatch to the correct handler
                // This handles multi dimensional arrays
                if (object instanceof long[]) {
                    append((long[]) object);
                }
                else if (object instanceof int[]) {
                    append((int[]) object);
                }
                else if (object instanceof short[]) {
                    append((short[]) object);
                }
                else if (object instanceof char[]) {
                    append((char[]) object);
                }
                else if (object instanceof byte[]) {
                    append((byte[]) object);
                }
                else if (object instanceof double[]) {
                    append((double[]) object);
                }
                else if (object instanceof float[]) {
                    append((float[]) object);
                }
                else if (object instanceof boolean[]) {
                    append((boolean[]) object);
                }
                else {
                    // Not an array of primitives
                    append((Object[]) object);
                }
            }
        }
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>long</code>.
     * </p>
     * 
     * @param value the long to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(long value) {
        iTotal = iTotal * iConstant + ((int) (value ^ (value >> 32)));
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for an <code>int</code>.
     * </p>
     * 
     * @param value the int to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(int value) {
        iTotal = iTotal * iConstant + value;
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>short</code>.
     * </p>
     * 
     * @param value the short to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(short value) {
        iTotal = iTotal * iConstant + value;
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>char</code>.
     * </p>
     * 
     * @param value the char to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(char value) {
        iTotal = iTotal * iConstant + value;
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>byte</code>.
     * </p>
     * 
     * @param value the byte to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(byte value) {
        iTotal = iTotal * iConstant + value;
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>double</code>.
     * </p>
     * 
     * @param value the double to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(double value) {
        return append(Double.doubleToLongBits(value));
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>float</code>.
     * </p>
     * 
     * @param value the float to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(float value) {
        iTotal = iTotal * iConstant + Float.floatToIntBits(value);
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>boolean</code>.
     * </p>
     * <p>
     * This adds <code>iConstant * 1</code> to the <code>hashCode</code> and not a
     * <code>1231</code> or <code>1237</code> as done in java.lang.Boolean. This is in
     * accordance with the <i>Effective Java</i> design.
     * </p>
     * 
     * @param value the boolean to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(boolean value) {
        iTotal = iTotal * iConstant + (value ? 0 : 1);
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for an <code>Object</code> array.
     * </p>
     * 
     * @param array the array to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(Object[] array) {
        if (array == null) {
            iTotal = iTotal * iConstant;
        }
        else {
            for (int i = 0; i < array.length; i++) {
                append(array[i]);
            }
        }
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>long</code> array.
     * </p>
     * 
     * @param array the array to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(long[] array) {
        if (array == null) {
            iTotal = iTotal * iConstant;
        }
        else {
            for (int i = 0; i < array.length; i++) {
                append(array[i]);
            }
        }
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for an <code>int</code> array.
     * </p>
     * 
     * @param array the array to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(int[] array) {
        if (array == null) {
            iTotal = iTotal * iConstant;
        }
        else {
            for (int i = 0; i < array.length; i++) {
                append(array[i]);
            }
        }
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>short</code> array.
     * </p>
     * 
     * @param array the array to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(short[] array) {
        if (array == null) {
            iTotal = iTotal * iConstant;
        }
        else {
            for (int i = 0; i < array.length; i++) {
                append(array[i]);
            }
        }
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>char</code> array.
     * </p>
     * 
     * @param array the array to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(char[] array) {
        if (array == null) {
            iTotal = iTotal * iConstant;
        }
        else {
            for (int i = 0; i < array.length; i++) {
                append(array[i]);
            }
        }
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>byte</code> array.
     * </p>
     * 
     * @param array the array to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(byte[] array) {
        if (array == null) {
            iTotal = iTotal * iConstant;
        }
        else {
            for (int i = 0; i < array.length; i++) {
                append(array[i]);
            }
        }
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>double</code> array.
     * </p>
     * 
     * @param array the array to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(double[] array) {
        if (array == null) {
            iTotal = iTotal * iConstant;
        }
        else {
            for (int i = 0; i < array.length; i++) {
                append(array[i]);
            }
        }
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>float</code> array.
     * </p>
     * 
     * @param array the array to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(float[] array) {
        if (array == null) {
            iTotal = iTotal * iConstant;
        }
        else {
            for (int i = 0; i < array.length; i++) {
                append(array[i]);
            }
        }
        return this;
    }

    /**
     * <p>
     * Append a <code>hashCode</code> for a <code>boolean</code> array.
     * </p>
     * 
     * @param array the array to add to the <code>hashCode</code>
     * @return this
     */
    public HashCodeBuilder append(boolean[] array) {
        if (array == null) {
            iTotal = iTotal * iConstant;
        }
        else {
            for (int i = 0; i < array.length; i++) {
                append(array[i]);
            }
        }
        return this;
    }

    /**
     * <p>
     * Return the computed <code>hashCode</code>.
     * </p>
     * 
     * @return <code>hashCode</code> based on the fields appended
     */
    public int toHashCode() {
        return iTotal;
    }

}
