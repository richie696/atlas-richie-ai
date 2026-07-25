package cn.richie696.ai.vectorstore.vikingdb;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VikingDbFilterExpressionConverterTests {

    private final VikingDbFilterExpressionConverter converter = new VikingDbFilterExpressionConverter();

    @Test
    void convertsEqualityToVikingDbMust() {
        Map<String, Object> actual = converter.convert(new Filter.Expression(Filter.ExpressionType.EQ,
                new Filter.Key("tenant"), new Filter.Value("acme")));

        assertThat(actual).isEqualTo(Map.of("op", "must", "field", "tenant", "conds", List.of("acme")));
    }

    @Test
    void convertsRangesAndLogicalExpressions() {
        Filter.Expression expression = new Filter.Expression(Filter.ExpressionType.AND,
                new Filter.Expression(Filter.ExpressionType.GTE, new Filter.Key("price"), new Filter.Value(10)),
                new Filter.Expression(Filter.ExpressionType.IN, new Filter.Key("region"), new Filter.Value(List.of("cn", "sg"))));

        assertThat(converter.convert(expression)).isEqualTo(Map.of("op", "and", "conds", List.of(
                Map.of("op", "range", "field", "price", "gte", 10),
                Map.of("op", "must", "field", "region", "conds", List.of("cn", "sg")))));
    }

    @Test
    void serializesTheNativeObjectWithoutDoubleQuotedKeys() {
        String actual = converter.convertExpression(new Filter.Expression(Filter.ExpressionType.EQ,
                new Filter.Key("quoted\"field"), new Filter.Value("value")));

        assertThat(actual).contains("\"op\":\"must\"").doesNotContain("\\\"\\\"");
    }
}
