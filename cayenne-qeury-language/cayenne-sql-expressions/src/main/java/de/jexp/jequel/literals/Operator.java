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
    IS_NOT("is not", "isNot"),
    IS,

    IN,
    BETWEEN,

    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    BY("/");

    private final String sqlOperator;
    private final String javaOperator;

    Operator() {
        this(null, null);
    }

    Operator(String sqlOperator) {
        this(sqlOperator, null);
    }
    Operator(String sqlOperator, String javaOperator) {
        this.sqlOperator = sqlOperator;
        this.javaOperator = javaOperator;
    }

    public String getSqlKeyword() {
        if (sqlOperator != null) {
            return sqlOperator;
        }

        return name().toLowerCase();
    }

    public String javaName() {
        if (javaOperator != null) {
            return javaOperator;
        }

        return name().toLowerCase();
    }
}
