package de.jexp.jequel.literals;

public enum Operator implements SqlKeyword {
    LE("<="),
    GE(">="),
    NE("!="),
    LT("<"),
    GT(">"),
    EQ("="),
    LIKE,
    AND,
    OR,
    IS_NOT,
    IS,
    IN,
    BETWEEN,
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    BY("/");

    private final String sqlOperator;

    Operator() {
        this(null);
    }

    Operator(String sqlOperator) {
        this.sqlOperator = sqlOperator;
    }

    public String getSqlKeyword() {
        return sqlOperator;
    }
}
