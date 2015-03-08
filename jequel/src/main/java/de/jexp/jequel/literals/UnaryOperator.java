package de.jexp.jequel.literals;

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
    TO_NUMBER;

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