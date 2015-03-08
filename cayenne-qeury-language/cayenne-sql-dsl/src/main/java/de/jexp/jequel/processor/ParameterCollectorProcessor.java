package de.jexp.jequel.processor;

import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.ParamExpression;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParameterCollectorProcessor extends AbstractExpressionProcessor<List<ParamExpression>> {
    private final List<ParamExpression> paramExpressions = new LinkedList<ParamExpression>();
    private final List<ParamExpression> namedExpressions = new LinkedList<ParamExpression>();

    public List<ParamExpression> getResult() {
        List<ParamExpression> result = new ArrayList<ParamExpression>(paramExpressions);
        result.addAll(namedExpressions);
        return result;
    }

    public List<ParamExpression> getParamExpressions() {
        return paramExpressions;
    }

    public List<ParamExpression> getNamedExpressions() {
        return namedExpressions;
    }

    protected void doProcess(Expression expression) {
        if (expression instanceof ParamExpression) {
            ParamExpression paramExpression = (ParamExpression) expression;
            if (paramExpression.isNamedExpression()) {
                namedExpressions.add(paramExpression);
            } else {
                paramExpressions.add(paramExpression);
            }
        }
    }

}
