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

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 * @since 4.0
 */
public class DataFactory {

    private Random random = ValueProvider.RANDOM;

    /**
     * @return A random first name
     */
    public String getFirstName() {
        return ValueProvider.FIRST_NAMES.randomValue();
    }

    /**
     * @return a combination of first and last name values in one string
     */
    public String getName() {
        return getFirstName() + " " + getLastName();
    }

    /**
     * @return A random last name
     */
    public String getLastName() {
        return ValueProvider.LAST_NAMES.randomValue();
    }

    /**
     * @return A random street name
     */
    public String getStreetName() {
        return ValueProvider.STREET_NAMES.randomValue();
    }

    /**
     * @return A random street suffix
     */
    public String getStreetSuffix() {
        return ValueProvider.ADDRESS_SUFFIXES.randomValue();
    }

    /**
     * @return City as a string
     */
    public String getCity() {
        return ValueProvider.CITIES.randomValue();
    }

    /**
     * Generates an address value consisting of house number, street name and
     * street suffix. i.e. <code>543 Larkhill Road</code>
     *
     * @return Address as a string
     */
    public String getAddress() {
        int num = 404 + random.nextInt(1400);
        return num + " " + getStreetName() + " " + getStreetSuffix();
    }

    /**
     * Generates line 2 for a street address (usually an Apt. or Suite #).
     * Returns default value if the probabilty test fails.
     *
     * @return Street address line 2
     */
    public String getAddressLine2() {
        int test = random.nextInt(100);
        if (test < 50) {
            return "Apt #" + 100 + random.nextInt(1000);
        } else {
            return "Suite #" + 100 + random.nextInt(1000);
        }
    }

    /**
     * Creates a random birthdate within the range of 1955 to 1985
     *
     * @return Date representing a birthdate
     */
    public Date getBirthDate() {
        Date base = new Date(0);
        return getDate(base, -365 * 15, 365 * 15);
    }

    /**
     * Returns a random int value.
     *
     * @return random number
     */
    public int getNumber() {
        return getNumberBetween(Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Returns a random number between 0 and max
     *
     * @param max
     *            Maximum value of result
     * @return random number no more than max
     */
    public int getNumberUpTo(int max) {
        return getNumberBetween(0, max);
    }

    /**
     * Returns a number betwen min and max
     *
     * @param min
     *            minimum value of result
     * @param max
     *            maximum value of result
     * @return Random number within range
     */
    public int getNumberBetween(int min, int max) {

        if (max < min) {
            throw new IllegalArgumentException(String.format(
                    "Minimum must be less than minimum (min=%d, max=%d)", min,
                    max));
        }

        return min + random.nextInt(max - min);
    }

    /**
     * Builds a date from the year, month, day values passed in
     *
     * @param year
     *            The year of the final {@link Date} result
     * @param month
     *            The month of the final {@link Date} result (from 1-12)
     * @param day
     *            The day of the final {@link Date} result
     * @return Date representing the passed in values.
     */
    public Date getDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, day, 0, 0, 0);
        return cal.getTime();
    }

    /**
     * Returns a random date which is in the range <code>baseData</code> +
     * <code>minDaysFromData</code> to <code>baseData</code> +
     * <code>maxDaysFromData</code>. This method does not alter the time
     * component and the time is set to the time value of the base date.
     *
     * @param baseDate
     *            Date to start from
     * @param minDaysFromDate
     *            minimum number of days from the baseDate the result can be
     * @param maxDaysFromDate
     *            maximum number of days from the baseDate the result can be
     * @return A random date
     */
    public Date getDate(Date baseDate, int minDaysFromDate, int maxDaysFromDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(baseDate);
        int diff = minDaysFromDate
                + (random.nextInt(maxDaysFromDate - minDaysFromDate));
        cal.add(Calendar.DATE, diff);
        return cal.getTime();
    }

    /**
     * Returns a random date between two dates. This method will alter the time
     * component of the dates
     *
     * @param minDate
     *            Minimum date that can be returned
     * @param maxDate
     *            Maximum date that can be returned
     * @return random date between these two dates.
     */
    public Date getDateBetween(Date minDate, Date maxDate) {
        // this can break if seconds is an int
        long seconds = (maxDate.getTime() - minDate.getTime()) / 1000;
        seconds = (long) (random.nextDouble() * seconds);
        Date result = new Date();
        result.setTime(minDate.getTime() + (seconds * 1000));
        return result;
    }

    /**
     * Returns random text made up of english words of length
     * <code>length</code>
     *
     * @param length
     *            length of returned string
     *
     * @return string made up of actual words with length <code>length</code>
     */
    public String getRandomText(int length) {
        return getRandomText(length, length);
    }

    /**
     * Returns random text made up of english words
     *
     * @param minLength
     *            minimum length of returned string
     * @param maxLength
     *            maximum length of returned string
     * @return string of length between min and max length
     */
    public String getRandomText(int minLength, int maxLength) {
        validateMinMaxParams(minLength, maxLength);

        StringBuilder sb = new StringBuilder(maxLength);
        int length = minLength;
        if (maxLength != minLength) {
            length = length + random.nextInt(maxLength - minLength);
        }
        while (length > 0) {
            if (sb.length() != 0) {
                sb.append(" ");
                length--;
            }
            String word = getRandomWord();
            sb.append(word);
            length = length - word.length();
        }

        if (sb.length() < maxLength) {
            return sb.toString();
        } else {
            return sb.substring(0, maxLength);
        }
    }

    private void validateMinMaxParams(int minLength, int maxLength) {
        if (minLength < 0) {
            throw new IllegalArgumentException("Minimum length must be a non-negative number");
        }

        if (maxLength < 0) {
            throw new IllegalArgumentException("Maximum length must be a non-negative number");
        }

        if (maxLength < minLength) {
            throw new IllegalArgumentException(
                    String.format(
                            "Minimum length must be less than maximum length (min=%d, max=%d)",
                            minLength, maxLength));
        }
    }

    /**
     * @return a random character
     */
    public char getRandomChar() {
        return (char) (random.nextInt(26) + 'a');
    }

    /**
     * Return a string containing <code>length</code> random characters
     *
     * @param length
     *            number of characters to use in the string
     * @return A string containing <code>length</code> random characters
     */
    public String getRandomChars(int length) {
        return getRandomChars(length, length);
    }

    /**
     * Return a string containing between <code>length</code> random characters
     *
     * @param maxLength max number of characters to use in the string
     * @param minLength min number of characters to use in the string
     * @return A string containing <code>length</code> random characters
     */
    public String getRandomChars(int minLength, int maxLength) {
        validateMinMaxParams(minLength, maxLength);
        StringBuilder sb = new StringBuilder(maxLength);

        int length = minLength;
        if (maxLength != minLength) {
            length = length + random.nextInt(maxLength - minLength);
        }
        while (length > 0) {
            sb.append(getRandomChar());
            length--;
        }
        return sb.toString();
    }

    /**
     * Returns a word of a length between 1 and 10 characters.
     *
     * @return A work of max length 10
     */
    public String getRandomWord() {
        return ValueProvider.WORDS.randomValue();
    }

    /**
     *
     * @param chance
     *            Chance of a suffix being returned
     * @return
     */
    public String getSuffix(int chance) {
        return ValueProvider.suffixes.randomValue(chance);
    }

    /**
     * Return a person prefix or null if the odds are too low.
     *
     * @param chance
     *            Odds of a prefix being returned
     * @return Prefix string
     */
    public String getPrefix(int chance) {
        return ValueProvider.prefixes.randomValue(chance);
    }

    /**
     * Returns a string containing a set of numbers with a fixed number of
     * digits
     *
     * @param digits
     *            number of digits in the final number
     * @return Random number as a string with a fixed length
     */
    public String getNumberText(int digits) {
        StringBuilder result = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            result.append(random.nextInt(10));
        }
        return result.toString();
    }

    /**
     * Generates an email address
     *
     * @return an email address
     */
    public String getEmailAddress() {
        return getLogin() + "@" + ValueProvider.EMAIL_HOSTS.randomValue() + "." + ValueProvider.TLDS.randomValue();
    }

    public String getLogin() {
        int test = random.nextInt(100);

        String login;
        if (test < 20) {
            login = getFirstName().charAt(0) + getLastName();
        } else if (test < 40) {
            login = getFirstName() + "." + getLastName();
        } else if (test < 45) {
            login = getFirstName() + "_" + getLastName();
        } else if (test < 55) {
            login = getLastName();
        } else {
            login = getRandomWord() + getRandomWord();
        }

        if (random.nextInt(100) > 80) {
            login = login + random.nextInt(100);
        }

        return login;
    }

    public boolean chance(int chance) {
        return random.nextInt(100) < chance;
    }

    /**
     * Call randomize with a seed value to reset the random number generator. By
     * using the same seed over different tests, you will should get the same
     * results out for the same data generation calls.
     *
     * @param seed
     *            Seed value to use to generate random numbers
     */
    public void randomize(int seed) {
        random = new Random(seed);
    }
}
