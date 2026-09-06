package cn.richie696.ai.vectorstore.vikingdb.internal;

import org.springframework.ai.vectorstore.filter.Filter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Extracts field names from Spring AI filter trees.
 */
final class VikingDbFilterExpressionFields {
    private VikingDbFilterExpressionFields() {
    }

    static Set<String> collect(Filter.Expression expression) {
        Set<String> result = new LinkedHashSet<>();
        collectNode(expression, result);
        return result;
    }

    private static void collectNode(Filter.Expression expression, Set<String> result) {
        if (expression == null) return;
        collectOperand(expression.left(), result);
        collectOperand(expression.right(), result);
    }

    private static void collectOperand(Filter.Operand operand, Set<String> result) {
        if (operand instanceof Filter.Key key) result.add(key.key());
        if (operand instanceof Filter.Expression expression) collectNode(expression, result);
        if (operand instanceof Filter.Group group) collectNode(group.content(), result);
    }
}
