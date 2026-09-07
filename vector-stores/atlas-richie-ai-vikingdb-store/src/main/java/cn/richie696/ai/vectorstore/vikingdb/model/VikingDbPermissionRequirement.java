package cn.richie696.ai.vectorstore.vikingdb.model;

/**
 * A non-secret, provider-documented IAM requirement that can be shown before an operation runs.
 *
 * <p>This is a declaration, not a claim that the current AK/SK has been authorized. VikingDB does
 * not expose a safe permission-simulation API to the data-plane SDK; mutating permissions are
 * verified by the real operation and normalized to {@link VikingDbFailureKind#PERMISSION_DENIED}.</p>
 */
public record VikingDbPermissionRequirement(
        VikingDbPermissionOperation operation,
        String iamActionHint,
        String recommendedManagedPolicy) {

    public static VikingDbPermissionRequirement of(VikingDbPermissionOperation operation) {
        return new VikingDbPermissionRequirement(operation, operation.iamActionHint(), operation.managedPolicy());
    }
}
