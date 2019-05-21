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
package org.apache.cayenne.datafactory;

import java.util.Random;

/**
 * @since 4.0
 */
public abstract class DictionaryValueProvider<T> implements ValueProvider<T> {

    private Random random = ValueProvider.RANDOM;

    public DictionaryValueProvider(Random random) {
        this.random = random;
    }

    /**
     * Returns a random item from an array of items
     *
     * @return Item from the array
     */
    public T randomValue() {
        return randomValue(100, null);
    }

    /**
     * Returns a random item from an array of items or null depending on the
     * probability parameter. The probability determines the chance (in %) of
     * returning an item from the array versus null.
     *
     * @param probability
     *            chance (in %, 100 being guaranteed) of returning an item from
     *            the array
     * @return Item from the array or the default value
     */
    public T randomValue(int probability) {
        return randomValue(probability, null);
    }

    /**
     * Returns a random item from an array of items or the defaultItem depending
     * on the probability parameter. The probability determines the chance (in
     * %) of returning an item from the array versus the default value.
     *
     * @param probability
     *            chance (in %, 100 being guaranteed) of returning an item from
     *            the array
     * @param defaultItem
     *            value to return if the probability test fails
     * @return Item from the array or the default value
     */
    public T randomValue(int probability, T defaultItem) {
        if (values() == null) {
            throw new IllegalArgumentException("Item array cannot be null");
        }
        if (values().length == 0) {
            throw new IllegalArgumentException("Item array cannot be empty");
        }
        return chance(probability) ? values()[random.nextInt(values().length)] : defaultItem;
    }

    /**
     * Gives you a true/false based on a probability with a random number
     * generator. Can be used to optionally add elements.
     *
     * <pre>
     * if (DataFactory.chance(70)) {
     * 	// 70% chance of this code being executed
     * }
     * </pre>
     *
     * @param chance
     *            % chance of returning true
     * @return
     */
    public boolean chance(int chance) {
        return random.nextInt(100) < chance;
    }

    @Override
    public void setRandom(Random random) {
        this.random = random;
    }

    protected abstract T[] values();

}
