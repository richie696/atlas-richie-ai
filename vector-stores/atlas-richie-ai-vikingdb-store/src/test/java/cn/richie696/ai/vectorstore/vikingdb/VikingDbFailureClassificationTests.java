package cn.richie696.ai.vectorstore.vikingdb;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbFailureKind;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbPermissionOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VikingDbFailureClassificationTests {
    @Test
    void mapsPermissionDeniedToOperationScopedRemediation() {
        VikingDbVectorStoreException exception = new VikingDbVectorStoreException(
                "upsertData batch [0,1)", "docs", new IllegalStateException("AccessDenied: permission is denied"));

        assertThat(exception.getFailureKind()).isEqualTo(VikingDbFailureKind.PERMISSION_DENIED);
        assertThat(exception.getPermissionOperation()).isEqualTo(VikingDbPermissionOperation.DOCUMENT_UPSERT);
        assertThat(exception.getRecommendedManagedPolicy()).isEqualTo("VikingdbFullAccess");
        assertThat(exception.getIamActionHint()).isEqualTo("vikingdb:*");
        assertThat(exception.getRemediationMessage()).contains("VikingdbFullAccess");
        assertThat(exception.isRetryable()).isFalse();
    }

    @Test
    void classifiesAsyncIndexReadinessAsRetryableRatherThanPermissionDenied() {
        VikingDbVectorStoreException exception = new VikingDbVectorStoreException(
                "searchByVector", "docs", new IllegalStateException("Index is not ready"));

        assertThat(exception.getFailureKind()).isEqualTo(VikingDbFailureKind.RESOURCE_NOT_READY);
        assertThat(exception.getPermissionOperation()).isEqualTo(VikingDbPermissionOperation.VECTOR_SEARCH);
        assertThat(exception.isRetryable()).isTrue();
    }
}
