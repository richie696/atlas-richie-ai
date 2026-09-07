package cn.richie696.ai.vectorstore.vikingdb;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbFailureKind;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbPermissionOperation;
import com.volcengine.vikingdb.runtime.exception.VectorApiException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VikingDB 操作失败异常，保留操作名与集合名以便诊断。
 *
 * <p>封装数据面与控制面 SDK 异常，让调用方可以用统一类型处理所有 VikingDB 失败路径。
 * 异常消息按日志可 grep 的格式构造：始终以 {@code "VikingDB <operation>"} 开头。</p>
 */
public class VikingDbVectorStoreException extends RuntimeException {
    private static final Pattern PROVIDER_CODE = Pattern.compile("\\\"(?:code|Code)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern REQUEST_ID = Pattern.compile("\\\"(?:request_id|RequestId)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private final String operation;
    private final String collection;
    private final String index;
    private final String providerCode;
    private final String requestId;
    private final VikingDbFailureKind failureKind;
    private final VikingDbPermissionOperation permissionOperation;

    /**
     * @param operation  失败的 SDK 调用短标签（如 {@code "upsertData"}、{@code "searchByVector"}、
     *                   {@code "schema initialization"}）。内嵌到异常消息中，
     *                   作为日志过滤的稳定标识。
     * @param collection 操作目标的集合名；若失败与具体集合无关则为 {@code null}
     *                   （例如 {@code assertSuccess} 中的空响应断言）。
     * @param cause      底层 SDK 异常；若失败为纯合成异常（例如校验抛出的
     *                   {@code IllegalStateException}）则为 {@code null}。cause 作为
     *                   {@link #getCause()} 保留以维持调用栈；其 message 可用时内联进
     *                   包装消息，否则使用字面量 {@code "no cause available"}。
     */
    public VikingDbVectorStoreException(String operation, String collection, Throwable cause) {
        this(operation, collection, null, null, null, cause);
    }

    public VikingDbVectorStoreException(String operation, String collection, String index,
                                        String providerCode, String requestId, Throwable cause) {
        super("VikingDB " + operation + (collection == null ? "" : " failed for collection '" + collection + "'")
                + ": " + (cause == null ? "no cause available" : cause.getMessage()), cause);
        this.operation = operation;
        this.collection = collection;
        this.index = index;
        String providerMessage = cause == null ? "" : String.valueOf(cause);
        this.providerCode = providerCode == null ? extract(PROVIDER_CODE, providerMessage) : providerCode;
        this.requestId = requestId == null ? extract(REQUEST_ID, providerMessage) : requestId;
        this.failureKind = classify(providerMessage);
        this.permissionOperation = VikingDbPermissionOperation.fromSdkOperation(operation);
    }

    public String getOperation() {
        return operation;
    }

    public String getCollection() {
        return collection;
    }

    public String getIndex() {
        return index;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getRequestId() {
        return requestId;
    }

    /**
     * Lets callers distinguish IAM remediation from a transient asynchronous-resource state and
     * generic provider failures without parsing vendor text.
     */
    public VikingDbFailureKind getFailureKind() {
        return failureKind;
    }

    /** Logical endpoint-level permission requirement for this failed operation. */
    public VikingDbPermissionOperation getPermissionOperation() {
        return permissionOperation;
    }

    /** True only when the provider reports that an accepted resource is not searchable yet. */
    public boolean isRetryable() {
        return failureKind == VikingDbFailureKind.RESOURCE_NOT_READY;
    }

    /** Documented IAM action pattern to show in a permission-remediation UI. */
    public String getIamActionHint() {
        return permissionOperation.iamActionHint();
    }

    /** Managed-policy fallback when a project/resource-scoped custom policy is not available. */
    public String getRecommendedManagedPolicy() {
        return permissionOperation.managedPolicy();
    }

    /** A display-safe remediation message; credentials and raw request payloads are never included. */
    public String getRemediationMessage() {
        if (failureKind == VikingDbFailureKind.PERMISSION_DENIED) {
            return "Grant " + getIamActionHint() + " for the target VikingDB project/resource, or attach "
                    + getRecommendedManagedPolicy() + ".";
        }
        if (failureKind == VikingDbFailureKind.RESOURCE_NOT_READY) {
            return "The VikingDB resource was accepted but is not ready for data-plane access; retry after its asynchronous build completes.";
        }
        return "Inspect the provider code and request ID, then verify the target project, resource and service state.";
    }

    private static VikingDbFailureKind classify(String providerMessage) {
        String normalized = providerMessage.toLowerCase(Locale.ROOT);
        if (normalized.contains("index is not ready") || normalized.contains("resource not ready")
                || normalized.contains("initializing") || normalized.contains("constructing")) {
            return VikingDbFailureKind.RESOURCE_NOT_READY;
        }
        if (normalized.contains("accessdenied") || normalized.contains("permissiondenied")
                || normalized.contains("no permission") || normalized.contains("unauthorized")
                || normalized.contains("forbidden") || normalized.contains("not authorized")
                || normalized.contains("permission is denied")) {
            return VikingDbFailureKind.PERMISSION_DENIED;
        }
        return VikingDbFailureKind.PROVIDER_FAILURE;
    }

    private static String extract(Pattern pattern, String source) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }
}
