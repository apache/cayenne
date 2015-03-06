package de.jexp.jequel.literals;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 18.10.2007 00:47:09
 */
public enum UnaryOperator implements SqlKeyword {
    NOT,
    SUM,
    COUNT,
    ROUND,
    AVG,
    EXISTS,
    NOT_EXISTS,
    MIN,
    MAX,
    TO_NUMBER, NVL;

    private final String sqlOperator;

    UnaryOperator() {
        this(null);
    }

    UnaryOperator(final String sqlOperator) {
        this.sqlOperator = sqlOperator;
    }

    public String getSqlKeyword() {
        return sqlOperator;
    }
}