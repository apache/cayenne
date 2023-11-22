/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.value.json;

/**
 * @since 4.2
 */
final class JsonTokenizer {

    private static final char[] NULL_LOWER  = {'n', 'u', 'l', 'l'};
    private static final char[] NULL_UPPER  = {'N', 'U', 'L', 'L'};
    private static final char[] TRUE_LOWER  = {'t', 'r', 'u', 'e'};
    private static final char[] TRUE_UPPER  = {'T', 'R', 'U', 'E'};
    private static final char[] FALSE_LOWER = {'f', 'a', 'l', 's', 'e'};
    private static final char[] FALSE_UPPER = {'F', 'A', 'L', 'S', 'E'};

    private final char[] data;
    private int position;

    private State[] states = new State[32];
    private int currentState;

    JsonTokenizer(String json) {
        data = json.toCharArray();
        position = 0;
        currentState = 0;
        states[currentState] = State.NONE;
    }

    /**
     * read next value from the stream
     * @return next Token for the value
     */
    JsonToken nextToken() {
        while (position < data.length) {
            // skip whitespace
            skipWhitespace();
            if (position == data.length) {
                break;
            }
            JsonToken token = nextValue();
            // only string could be used as an object member name
            if (states[currentState] == State.OBJECT_MEMBER_NAME) {
                if (token.type != TokenType.OBJECT_END
                        && token.type != TokenType.OBJECT_START
                        && token.type != TokenType.STRING) {
                    throw new MalformedJsonException("Unexpected '" + token.toString() + "' at " + position);
                } else if (states[currentState] == State.OBJECT_MEMBER_NAME
                        && token.type == TokenType.STRING) {
                    states[currentState] = State.OBJECT_MEMBER_DELIMITER;
                }
            } else if(token.type != TokenType.ARRAY_START
                    && states[currentState] == State.ARRAY_VALUE) {
                states[currentState] = State.ARRAY_DELIMITER;
            }
            return token;
        }

        if(states[currentState] != State.NONE) {
            switch (states[currentState]) {
                case ARRAY_VALUE:
                    throw new MalformedJsonException("']' expected");
                case ARRAY_DELIMITER:
                    throw new MalformedJsonException("Next array value expected, ',' found");
                case OBJECT_MEMBER_DELIMITER:
                    throw new MalformedJsonException("Next object member value expected, ',' found");
                case OBJECT_MEMBER_NAME:
                    throw new MalformedJsonException("Next object member name expected, ',' found");
                case OBJECT_MEMBER_VALUE:
                    throw new MalformedJsonException("'}' expected");
            }
        }

        return new JsonToken(TokenType.NONE, position, position);
    }

    private void pushState(State state) {
        // grow states array if needed, don't use array list purely for the performance
        currentState++;
        if(currentState >= states.length) {
            State[] newStates = new State[states.length << 2];
            System.arraycopy(states, 0, newStates, 0, states.length);
            states = newStates;
        }
        states[currentState] = state;
    }

    private void popState() {
        currentState--;
    }

    private void skipWhitespace() {
        int length = data.length;
        while(position < length
                && (data[position] == ' '
                    || data[position] == '\t'
                    || data[position] == '\r'
                    || data[position] == '\n'
                    || data[position] == '\f')) {
            position++;
        }
    }

    private JsonToken nextValue() {
        if(position >= data.length) {
            throw new MalformedJsonException("Unexpected end of document");
        }

        switch (data[position]) {
            case '{':
                return startObject();
            case '[':
                return startArray();
            case ']':
                return arrayEnd();
            case '}':
                return objectEnd();
            case ':':
                if(states[currentState] != State.OBJECT_MEMBER_DELIMITER) {
                    throw new MalformedJsonException("Unexpected ':' at " + position);
                }
                states[currentState] = State.OBJECT_MEMBER_VALUE;
                position++;
                skipWhitespace();
                return nextValue();
            case ',':
                if(states[currentState] == State.OBJECT_MEMBER_VALUE) {
                    states[currentState] = State.OBJECT_MEMBER_NAME;
                } else if(states[currentState] == State.ARRAY_DELIMITER) {
                    states[currentState] = State.ARRAY_VALUE;
                } else {
                    throw new MalformedJsonException("Unexpected ',' at " + position);
                }
                position++;
                skipWhitespace();
                return nextValue();
            case '\"':
                return stringValue();
            case 'n':
            case 'N':
                return nullValue();
            case 't':
            case 'T':
                return trueValue();
            case 'f':
            case 'F':
                return falseValue();
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
                return numericValue();
            default:
                throw new MalformedJsonException("Unexpected symbol '" + data[position] + "' at " + position);
        }
    }

    private JsonToken objectEnd() {
        if(states[currentState] == State.OBJECT_MEMBER_VALUE
                || states[currentState] == State.OBJECT_MEMBER_NAME) {
            popState();
        } else {
            throw new MalformedJsonException("Unexpected '}' at " + position);
        }
        return new JsonToken(TokenType.OBJECT_END, position, position++);
    }

    private JsonToken arrayEnd() {
        if(states[currentState] == State.ARRAY_DELIMITER || states[currentState] == State.ARRAY_VALUE) {
            popState();
        } else {
            throw new MalformedJsonException("Unexpected ']' at " + position);
        }
        return new JsonToken(TokenType.ARRAY_END, position, position++);
    }

    private JsonToken startObject() {
        checkValueState('{');
        pushState(State.OBJECT_MEMBER_NAME);
        return new JsonToken(TokenType.OBJECT_START, position, position++);
    }

    private JsonToken startArray() {
        checkValueState('[');
        pushState(State.ARRAY_VALUE);
        return new JsonToken(TokenType.ARRAY_START, position, position++);
    }

    private JsonToken numericValue() {
        checkValueState(data[position]);
        /*
         * number
         *     integer fraction exponent
         *
         * integer
         *     digit
         *     onenine digits
         *     '-' digit
         *     '-' onenine digits
         *
         * digits
         *     digit
         *     digit digits
         *
         * digit
         *     '0'
         *     onenine
         *
         * onenine
         *
         *
         * fraction
         *     ""
         *     '.' digits
         *
         * exponent
         *     ""
         *     'E' sign digits
         *     'e' sign digits
         *
         * sign
         *     ""
         *     '+'
         *     '-'
         */

        int startPosition = position;
        NumberState state = NumberState.NONE;

        while (state != NumberState.DONE) {
            switch (data[position]) {
                case '0':
                    if(state == NumberState.NONE
                            || state == NumberState.MINUS) {
                        state = NumberState.ZERO;
                    } else if(state == NumberState.EXPONENT) {
                        state = NumberState.EXP_SIGN;
                    } else if(state != NumberState.DIGITS
                            && state != NumberState.FRACTION
                            && state != NumberState.EXP_SIGN) {
                        throw new MalformedJsonException("Wrong number format at position " + position);
                    }
                    break;
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if(state == NumberState.NONE
                            || state == NumberState.MINUS) {
                        state = NumberState.DIGITS;
                    }
                    if(state == NumberState.EXPONENT) {
                        state = NumberState.EXP_SIGN;
                    }
                    break;
                case '-':
                    if(state == NumberState.NONE) {
                        state = NumberState.MINUS;
                    } else if(state == NumberState.EXPONENT) {
                        state = NumberState.EXP_SIGN;
                    } else {
                        throw new MalformedJsonException("Wrong number format at position " + position);
                    }
                    break;
                case '+':
                    if(state != NumberState.EXPONENT) {
                        throw new MalformedJsonException("Wrong number format at position " + position);
                    }
                    state = NumberState.EXP_SIGN;
                    break;
                case '.':
                    if(state != NumberState.ZERO
                            && state != NumberState.DIGITS) {
                        throw new MalformedJsonException("Wrong number format at position " + position);
                    }
                    state = NumberState.FRACTION;
                    break;
                case 'e':
                case 'E':
                    if(state != NumberState.NONE
                            && state != NumberState.ZERO
                            && state != NumberState.DIGITS
                            && state != NumberState.FRACTION) {
                        throw new MalformedJsonException("Wrong number format at position " + position);
                    }
                    state = NumberState.EXPONENT;
                    break;

                case '}':
                case ']':
                case ':':
                case ',':
                case ' ':
                case '\t':
                case '\f':
                case '\r':
                case '\n':
                    state = NumberState.DONE;
                    position--; // this char should be consumed by outer call
                    break;
                default:
                    throw new MalformedJsonException("Wrong number format at position " + position);
            }

            if(position >= data.length - 1) {
                if(state == NumberState.DIGITS
                        || state == NumberState.ZERO
                        || state == NumberState.FRACTION
                        || state == NumberState.EXP_SIGN) {
                    state = NumberState.DONE;
                }
                if(state != NumberState.DONE) {
                    throw new MalformedJsonException("Wrong number format at position " + position);
                }
            }
            position++;
        }

        return new JsonToken(TokenType.NUMBER, startPosition, position);
    }

    private JsonToken nullValue() {
        checkValueState('n');
        return keyword(TokenType.NULL, NULL_LOWER, NULL_UPPER);
    }

    private JsonToken trueValue() {
        checkValueState('t');
        return keyword(TokenType.TRUE, TRUE_LOWER, TRUE_UPPER);
    }

    private JsonToken falseValue() {
        checkValueState('f');
        return keyword(TokenType.FALSE, FALSE_LOWER, FALSE_UPPER);
    }

    private JsonToken keyword(TokenType type, char[] keywordLower, char[] keywordUpper) {
        int length = keywordLower.length;

        if(data.length < position + length) {
            throw new MalformedJsonException("unknown value at position " + position);
        }

        for(int i = 0; i< length; i++) {
            if(data[position + i] != keywordLower[i]
                    && data[position + i] != keywordUpper[i]) {
                throw new MalformedJsonException("unknown value at position " + position + "(" + new String(keywordLower) + " expected)");
            }
        }

        position += length;
        if(data.length > position && isLiteral(data[position])) {
            throw new MalformedJsonException("unknown value at position " + position);
        }
        return new JsonToken(type, position - length, position);
    }

    private void checkValueState(char unexpected) {
        State state = states[currentState];
        if(state == State.ARRAY_DELIMITER
                || state == State.OBJECT_MEMBER_NAME
                || state == State.OBJECT_MEMBER_DELIMITER) {
            throw new MalformedJsonException("Unexpected '" + unexpected + "' at " + position);
        }
    }

    private boolean isLiteral(char c) {
        switch (c) {
            case '}':
            case ']':
            case ',':
            case ' ':
            case '\t':
            case '\f':
            case '\r':
            case '\n':
                return false;
            default:
                return true;
        }
    }

    private JsonToken stringValue() {
        int startPosition = ++position; // skip open quote
        while(position < data.length) {
            switch (data[position]) {
                // escape
                case '\\':
                    /*
                     * escape
                     *     '"'
                     *     '\'
                     *     '/'
                     *     'b'
                     *     'f'
                     *     'n'
                     *     'r'
                     *     't'
                     *     'u' hex hex hex hex
                     */
                    switch (data[++position]) {
                        case '"':
                        case '\\':
                        case '/':
                        case 'b':
                        case 'f':
                        case 'n':
                        case 'r':
                        case 't':
                            position++;
                            continue;
                        case 'u':
                            position++;
                            for(int i=0; i<4; i++) {
                                char next = data[position + i];
                                if ((next < '0' || next > '9')
                                        && (next < 'a' || next > 'f')
                                        && (next < 'A' || next > 'F')
                                ) {
                                    throw new MalformedJsonException("Unknown escape sequence "
                                            + String.valueOf(data, position - 1, 4) + " at position " + position);
                                }
                            }
                            position += 4;
                            continue;
                        default:
                            throw new MalformedJsonException("Unknown escape sequence "
                                    + String.valueOf(data, position - 1, 1) + " at position " + position);
                    }
                case '"':
                    return new JsonToken(TokenType.STRING, startPosition, position++);
            }
            position++;
        }
        throw new MalformedJsonException("Unexpected end of string literal");
    }

    enum State {
        NONE,
        OBJECT_MEMBER_NAME,
        OBJECT_MEMBER_DELIMITER,
        OBJECT_MEMBER_VALUE,
        ARRAY_VALUE,
        ARRAY_DELIMITER
    }

    public enum TokenType {
        NONE,
        ARRAY_START,
        ARRAY_END,
        OBJECT_START,
        OBJECT_END,
        STRING,
        NUMBER,
        TRUE,
        FALSE,
        NULL
    }

    enum NumberState {
        NONE,
        ZERO,
        DIGITS,
        MINUS,
        FRACTION,
        EXPONENT,
        EXP_SIGN,
        DONE
    }

    class JsonToken implements Comparable<JsonToken> {
        final TokenType type;
        final int from;
        final int to;

        JsonToken(TokenType type, int from, int to) {
            this.type = type;
            this.from = from;
            this.to = to;
        }

        public TokenType getType() {
            return type;
        }

        char[] getData() {
            return data;
        }

        int length() {
            return to - from;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            JsonToken token = (JsonToken) o;
            if(type != token.type) {
                return false;
            }
            if(length() != token.length()) {
                return false;
            }
            for(int i=from, j=token.from; i<to && j<token.to; i++, j++) {
                if(data[i] != token.getData()[j]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            for(int i=from; i<to; i++) {
                result = 31 * result + data[i];
            }
            return result;
        }

        @Override
        public int compareTo(JsonToken o) {
            if(type != o.type) {
                return type.ordinal() - o.type.ordinal();
            }

            int diff = length() - o.length();
            if(diff != 0) {
                return diff;
            }

            for(int i=from, j=o.from; i<to && j<o.to; i++, j++) {
                diff = data[i] - o.getData()[j];
                if(diff != 0) {
                    return diff;
                }
            }

            return 0;
        }

        @Override
        public String toString() {
            return new String(data, from, length());
        }
    }

}
