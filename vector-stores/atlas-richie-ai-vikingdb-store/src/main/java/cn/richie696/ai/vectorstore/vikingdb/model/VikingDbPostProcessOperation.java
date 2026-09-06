package cn.richie696.ai.vectorstore.vikingdb.model;

import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlled representation of a VikingDB server-side post-process operation.
 */
public final class VikingDbPostProcessOperation {
    public enum Type {
        SCORE_FUSION("score_fusion"),
        STRING_CONTAIN("string_contain"),
        STRING_MATCH("string_match"),
        ENUM_FREQ_LIMITER("enum_freq_limiter");

        private final String wireValue;

        Type(String wireValue) {
            this.wireValue = wireValue;
        }

        public String wireValue() {
            return wireValue;
        }
    }

    private final Type type;
    private final Map<String, Object> parameters;

    private VikingDbPostProcessOperation(Type type, Map<String, Object> parameters) {
        this.type = type;
        Map<String, Object> copy = new LinkedHashMap<>(parameters == null ? Map.of() : parameters);
        copy.remove("op");
        this.parameters = Map.copyOf(copy);
    }

    public static VikingDbPostProcessOperation of(Type type, Map<String, Object> parameters) {
        Assert.notNull(type, "post-process type must not be null");
        return new VikingDbPostProcessOperation(type, parameters);
    }

    public Type type() {
        return type;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

    public Map<String, Object> toNativeMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("op", type.wireValue());
        result.putAll(parameters);
        return Map.copyOf(result);
    }
}
