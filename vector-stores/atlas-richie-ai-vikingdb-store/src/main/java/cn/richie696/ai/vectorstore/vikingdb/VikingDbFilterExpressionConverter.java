/*
 * Copyright (c) 2026 Richie (https://www.github.com/richie696)
 * Licensed under Apache-2.0.
 */
package cn.richie696.ai.vectorstore.vikingdb;

import com.google.gson.Gson;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 将 Spring AI 过滤器转换为 VikingDB 原生的 scalar-filter 对象模型。 */
public final class VikingDbFilterExpressionConverter implements FilterExpressionConverter {

    private static final Gson GSON = new Gson();

    /**
     * {@inheritDoc}
     *
     * <p>将原生过滤器表示序列化为 JSON，匹配 VikingDB 控制面 filter 条件的传输格式。</p>
     */
    @Nonnull
    @Override
    public String convertExpression(@Nonnull Filter.Expression expression) {
        return GSON.toJson(convert(expression));
    }

    /**
     * VikingDB 原生形式，可用于 {@code SearchByVectorRequest.filter} 与
     * {@code CreateVikingdbTaskRequest.filterConds}。
     *
     * @param expression Spring AI 过滤器表达式。不能为 null。
     * @return 由 VikingDB 的 {@code op / conds / field / range} 操作符消费的嵌套
     *         {@code Map}/{@code List} 结构。
     * @throws IllegalArgumentException 若表达式含有 VikingDB 无法表达的操作（如 {@code CONTAINS}）。
     */
    public Map<String, Object> convert(Filter.Expression expression) {
        Assert.notNull(expression, "filter expression must not be null");
        return convertExpressionNode(expression);
    }

    /**
     * 按 {@link Filter.Expression#type()} 分发到对应的节点构造器。
     *
     * <p>Spring AI 的 {@code NOT} 通常由 {@code AbstractFilterExpressionConverter} 改写；
     * 此处重复该步骤，因为本转换器直接向调用方暴露原生 Map API。</p>
     */
    private Map<String, Object> convertExpressionNode(Filter.Expression expression) {
        return switch (expression.type()) {
            case AND -> logical("and", expression);
            case OR -> logical("or", expression);
            case EQ, IN -> membership("must", expression);
            case NE, NIN -> membership("must_not", expression);
            case LT -> range(expression, "lt");
            case LTE -> range(expression, "lte");
            case GT -> range(expression, "gt");
            case GTE -> range(expression, "gte");
            // AbstractFilterExpressionConverter normally rewrites NOT. Do it here as
            // well because this converter deliberately exposes the native Map API.
            case NOT -> negate(expression);
            default -> throw new IllegalArgumentException("VikingDB does not support filter operation: " + expression.type());
        };
    }

    /**
     * 构造 {@code and}/{@code or} 逻辑节点，连接左右操作数。
     * VikingDB 要求每个逻辑操作符恰好两个操作数。
     */
    private Map<String, Object> logical(String operation, Filter.Expression expression) {
        return Map.of("op", operation, "conds", List.of(convertOperand(expression.left()), convertOperand(expression.right())));
    }

    /**
     * 构造 {@code must}/{@code must_not} 成员节点。当值操作数为集合（由 {@code IN}/{@code NIN} 产生）
     * 时，每个元素成为独立条件；否则单值被包装为单元素列表。
     */
    private Map<String, Object> membership(String operation, Filter.Expression expression) {
        String field = key(expression.left());
        Object value = value(expression.right());
        List<Object> conditions = value instanceof Collection<?> values ? new ArrayList<>(values) : List.of(value);
        return Map.of("op", operation, "field", field, "conds", conditions);
    }

    /**
     * 为关系操作符（{@code lt}、{@code lte}、{@code gt}、{@code gte}）构造 {@code range} 节点。
     * bound 键即为操作符名本身，匹配 VikingDB 的 range 谓词语法。
     */
    private Map<String, Object> range(Filter.Expression expression, String bound) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("op", "range");
        result.put("field", key(expression.left()));
        result.put(bound, value(expression.right()));
        return result;
    }

    /**
     * 将 Spring AI 的 {@code NOT} 下推为正向谓词，通过翻转内层操作符极性实现。
     * 内层表达式必须是 {@link Filter.Expression}；不支持对 key/value 对取反（匹配 VikingDB 语法）。
     */
    private Map<String, Object> negate(Filter.Expression expression) {
        if (!(expression.left() instanceof Filter.Expression nested)) {
            throw new IllegalArgumentException("VikingDB NOT requires an expression operand");
        }
        return switch (nested.type()) {
            case EQ, IN -> membership("must_not", nested);
            case NE, NIN -> membership("must", nested);
            case LT -> range(nested, "gte");
            case LTE -> range(nested, "gt");
            case GT -> range(nested, "lte");
            case GTE -> range(nested, "lt");
            default -> throw new IllegalArgumentException("VikingDB cannot negate filter operation: " + nested.type());
        };
    }

    /**
     * 解析逻辑操作数：解包 filter group 并递归处理嵌套表达式。
     * 任何其他类型都是 Spring AI 过滤器层的 bug。
     */
    private Map<String, Object> convertOperand(Filter.Operand operand) {
        if (operand instanceof Filter.Group group) return convertExpressionNode(group.content());
        if (operand instanceof Filter.Expression expression) return convertExpressionNode(expression);
        throw new IllegalArgumentException("Expected a filter expression, got: " + operand);
    }

    /** 从 {@link Filter.Key} 提取字段名；其他操作数形态视为 bug。 */
    private static String key(Filter.Operand operand) {
        if (operand instanceof Filter.Key key) return key.key();
        throw new IllegalArgumentException("Expected a filter key, got: " + operand);
    }

    /** 从 {@link Filter.Value} 提取比较值；其他操作数形态视为 bug。 */
    private static Object value(Filter.Operand operand) {
        if (operand instanceof Filter.Value value) return value.value();
        throw new IllegalArgumentException("Expected a filter value, got: " + operand);
    }
}