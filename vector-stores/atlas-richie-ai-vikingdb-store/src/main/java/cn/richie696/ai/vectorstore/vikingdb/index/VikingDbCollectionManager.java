package cn.richie696.ai.vectorstore.vikingdb.index;

import cn.richie696.ai.vectorstore.vikingdb.VikingDbVectorStoreException;
import cn.richie696.ai.vectorstore.vikingdb.api.VikingDbCollectionOperations;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbCollectionDefinition;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbCollectionField;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbCollectionInfo;
import cn.richie696.ai.vectorstore.vikingdb.model.VikingDbResourceRef;
import com.volcengine.ApiException;
import com.volcengine.vikingdb.VikingdbApi;
import com.volcengine.vikingdb.model.CreateVikingdbCollectionRequest;
import com.volcengine.vikingdb.model.DeleteVikingdbCollectionRequest;
import com.volcengine.vikingdb.model.FieldForCreateVikingdbCollectionInput;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Minimal control-plane collection facade; schema mutation remains explicit.
 */
public final class VikingDbCollectionManager implements VikingDbCollectionOperations {
    private final VikingdbApi controlPlane;

    public VikingDbCollectionManager(VikingdbApi controlPlane) {
        this.controlPlane = Objects.requireNonNull(controlPlane, "controlPlane must not be null");
    }

    @Override
    public VikingDbCollectionInfo getCollection(VikingDbResourceRef target) {
        Objects.requireNonNull(target, "target must not be null");
        try {
            var request = new com.volcengine.vikingdb.model.GetVikingdbCollectionRequest()
                    .collectionName(target.collectionName());
            if (target.projectName() != null) request.projectName(target.projectName());
            var response = controlPlane.getVikingdbCollection(request);
            var fields = new LinkedHashMap<String, VikingDbCollectionField>();
            if (response.getFields() != null) response.getFields().forEach(field -> fields.put(field.getFieldName(),
                    new VikingDbCollectionField(field.getFieldName(),
                            field.getFieldType() == null ? null : field.getFieldType().getValue(),
                            field.getDim(), field.isIsPrimaryKey())));
            return new VikingDbCollectionInfo(target, response.getDescription(), fields,
                    response.getIndexNames(), response.isEnableKeywordsSearch());
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("getCollection", target.collectionName(), ex);
        }
    }

    @Override
    public VikingDbCollectionInfo createCollection(VikingDbCollectionDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        VikingDbResourceRef target = Objects.requireNonNull(definition.target(), "collection target must not be null");
        try {
            CreateVikingdbCollectionRequest request = new CreateVikingdbCollectionRequest()
                    .collectionName(target.collectionName()).description(definition.description())
                    .delProtection(definition.deletionProtection());
            if (target.projectName() != null) request.projectName(target.projectName());
            for (VikingDbCollectionField field : definition.fields()) {
                FieldForCreateVikingdbCollectionInput value = new FieldForCreateVikingdbCollectionInput()
                        .fieldName(field.name())
                        .fieldType(FieldForCreateVikingdbCollectionInput.FieldTypeEnum.fromValue(field.type()));
                if (field.dimension() != null) value.dim(field.dimension());
                if (field.primaryKey() != null) value.isPrimaryKey(field.primaryKey());
                request.addFieldsItem(value);
            }
            controlPlane.createVikingdbCollection(request);
            return getCollection(target);
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("createCollection", target.collectionName(), ex);
        }
    }

    @Override
    public void deleteCollection(VikingDbResourceRef target) {
        Objects.requireNonNull(target, "target must not be null");
        try {
            DeleteVikingdbCollectionRequest request = new DeleteVikingdbCollectionRequest()
                    .collectionName(target.collectionName());
            if (target.projectName() != null) request.projectName(target.projectName());
            controlPlane.deleteVikingdbCollection(request);
        } catch (ApiException ex) {
            throw new VikingDbVectorStoreException("deleteCollection", target.collectionName(), ex);
        }
    }
}
