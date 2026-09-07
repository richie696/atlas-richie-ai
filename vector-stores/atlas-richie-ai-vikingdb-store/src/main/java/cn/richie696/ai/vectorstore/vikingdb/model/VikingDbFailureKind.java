package cn.richie696.ai.vectorstore.vikingdb.model;

/** Stable, caller-facing classification of a VikingDB SDK failure. */
public enum VikingDbFailureKind {
    PERMISSION_DENIED,
    RESOURCE_NOT_READY,
    PROVIDER_FAILURE
}
