package cn.richie696.ai.vectorstore.vikingdb.model;

import java.util.List;
import java.util.Map;

/**
 * Fetch result preserving both found records and provider-reported missing ids.
 */
public record VikingDbFetchResponse(String requestId, List<VikingDbFetchedRecord> records,
                                    List<Object> idsNotExist) {
    public VikingDbFetchResponse {
        records = records == null ? List.of() : List.copyOf(records);
        idsNotExist = idsNotExist == null ? List.of() : List.copyOf(idsNotExist);
    }

    public record VikingDbFetchedRecord(Object id, Map<String, Object> fields,
                                        List<Float> denseVector, Map<String, Float> sparseVector) {
        public VikingDbFetchedRecord {
            fields = fields == null ? Map.of() : Map.copyOf(fields);
            denseVector = denseVector == null ? List.of() : List.copyOf(denseVector);
            sparseVector = sparseVector == null ? Map.of() : Map.copyOf(sparseVector);
        }
    }
}
