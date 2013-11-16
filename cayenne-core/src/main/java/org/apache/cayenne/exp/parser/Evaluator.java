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
package org.apache.cayenne.exp.parser;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.util.ConversionUtil;

/**
 * Performs argument conversions for a calling binary expression, so that the
 * expression could eval the arguments of the same type.
 * 
 * @since 3.2
 */
abstract class Evaluator {

    private static final ConcurrentMap<Class<?>, Evaluator> evaluators;
    private static final Evaluator NULL_LHS_EVALUATOR;
    private static final Evaluator DEFAULT_EVALUATOR;
    private static final Evaluator PERSISTENT_EVALUATOR;
    private static final Evaluator BIG_DECIMAL_EVALUATOR;
    private static final Evaluator COMPAREABLE_EVALUATOR;

    /**
     * A decorator of an evaluator that presumes non-null 'lhs' argument and
     * allows for null 'rhs'.
     */
    static class NonNullLhsEvaluator extends Evaluator {
        final Evaluator delegate;

        NonNullLhsEvaluator(Evaluator delegate) {
            this.delegate = delegate;
        }

        @Override
        int compare(Object lhs, Object rhs) {

            if (rhs == null) {
                return 1;
            } else {
                return delegate.compare(lhs, rhs);
            }
        }

        @Override
        boolean eq(Object lhs, Object rhs) {
            return rhs == null ? false : delegate.eq(lhs, rhs);
        }
    }

    static {
        evaluators = new ConcurrentHashMap<Class<?>, Evaluator>();

        NULL_LHS_EVALUATOR = new Evaluator() {
            @Override
            int compare(Object lhs, Object rhs) {
                throw new UnsupportedOperationException("Unsupported");
            }

            @Override
            boolean eq(Object lhs, Object rhs) {
                return rhs == null;
            }
        };

        DEFAULT_EVALUATOR = new NonNullLhsEvaluator(new Evaluator() {
            @Override
            boolean eq(Object lhs, Object rhs) {
                return lhs.equals(rhs);
            }

            @Override
            int compare(Object lhs, Object rhs) {
                throw new UnsupportedOperationException("Unsupported");
            }
        });

        PERSISTENT_EVALUATOR = new NonNullLhsEvaluator(new Evaluator() {

            @Override
            int compare(Object lhs, Object rhs) {
                throw new UnsupportedOperationException("Unsupported");
            }

            @Override
            boolean eq(Object lhs, Object rhs) {

                Persistent lhsPersistent = (Persistent) lhs;

                if (rhs instanceof Persistent) {
                    return lhsPersistent.getObjectId().equals(((Persistent) rhs).getObjectId());
                }

                if (rhs instanceof ObjectId) {
                    return lhsPersistent.getObjectId().equals(rhs);
                }

                if (rhs instanceof Map) {
                    return lhsPersistent.getObjectId().getIdSnapshot().equals(rhs);
                }

                if (lhsPersistent.getObjectId().getIdSnapshot().size() != 1) {
                    // the only options left below are for the single key IDs
                    return false;
                }

                if (rhs instanceof Number) {

                    // only care about whole numbers
                    if (rhs instanceof Integer) {
                        return Cayenne.longPKForObject(lhsPersistent) == ((Number) rhs).longValue();
                    }

                    if (rhs instanceof Long) {
                        return Cayenne.longPKForObject(lhsPersistent) == ((Number) rhs).longValue();
                    }
                }

                return Cayenne.pkForObject(lhsPersistent).equals(rhs);
            }
        });

        BIG_DECIMAL_EVALUATOR = new NonNullLhsEvaluator(new Evaluator() {

            @Override
            int compare(Object lhs, Object rhs) {
                return ((BigDecimal) lhs).compareTo(ConversionUtil.toBigDecimal(rhs));
            }

            @Override
            boolean eq(Object lhs, Object rhs) {

                // BigDecimals must be compared using compareTo (
                // see CAY-280 and BigDecimal.equals JavaDoc)

                return compare(lhs, rhs) == 0;
            }
        });

        COMPAREABLE_EVALUATOR = new NonNullLhsEvaluator(new Evaluator() {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            int compare(Object lhs, Object rhs) {
                return ((Comparable) lhs).compareTo(ConversionUtil.toComparable(rhs));
            }

            @Override
            boolean eq(Object lhs, Object rhs) {
                return lhs.equals(rhs);
            }
        });
    }

    static <T> Evaluator evaluator(Object lhs) {

        if (lhs == null) {
            return NULL_LHS_EVALUATOR;
        }

        Class<?> lhsType = lhs.getClass();

        Evaluator e = evaluators.get(lhsType);

        if (e == null) {
            Evaluator created = compileEvaluator(lhsType);
            Evaluator existing = evaluators.putIfAbsent(lhsType, created);
            e = existing != null ? existing : created;
        }

        return e;
    }

    private static Evaluator compileEvaluator(Class<?> lhsType) {

        Evaluator ev = findInHierarchy(lhsType);
        if (ev != null) {
            return ev;
        }

        // check known interfaces
        if (Persistent.class.isAssignableFrom(lhsType)) {
            return PERSISTENT_EVALUATOR;
        }

        if (BigDecimal.class.isAssignableFrom(lhsType)) {
            return BIG_DECIMAL_EVALUATOR;
        }

        if (Comparable.class.isAssignableFrom(lhsType)) {
            return COMPAREABLE_EVALUATOR;
        }

        // nothing we recognize... return default
        return DEFAULT_EVALUATOR;
    }

    private static Evaluator findInHierarchy(Class<?> lhsType) {

        if (Object.class.equals(lhsType)) {
            return null;
        }

        Evaluator ev = evaluators.get(lhsType);
        return (ev != null) ? ev : findInHierarchy(lhsType.getSuperclass());
    }

    abstract boolean eq(Object lhs, Object rhs);

    abstract int compare(Object lhs, Object rhs);
}
