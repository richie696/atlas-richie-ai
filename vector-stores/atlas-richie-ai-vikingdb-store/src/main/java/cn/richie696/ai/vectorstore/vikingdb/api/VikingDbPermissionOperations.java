package cn.richie696.ai.vectorstore.vikingdb.api;

import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbPermissionRequirement;

import java.util.List;

/**
 * Exposes all IAM requirements supported by the bound VikingDB Store before business operations
 * are attempted. This API performs no hidden write probes.
 */
public interface VikingDbPermissionOperations {
    List<VikingDbPermissionRequirement> describePermissionRequirements();
}
