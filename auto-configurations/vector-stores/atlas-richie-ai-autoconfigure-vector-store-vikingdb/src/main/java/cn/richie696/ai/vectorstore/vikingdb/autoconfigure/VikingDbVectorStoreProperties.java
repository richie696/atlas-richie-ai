/*
 * Copyright (c) 2026 Richie (https://www.github.com/richie696)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.richie696.ai.vectorstore.vikingdb.autoconfigure;

import lombok.Data;
import com.volcengine.vikingdb.model.FieldForCreateVikingdbCollectionInput;
import lombok.EqualsAndHashCode;
import org.springframework.ai.vectorstore.properties.CommonVectorStoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * VikingDB 向量库业务配置（collection / 维度 / initializeSchema / projectName 等）。
 *
 * <p>继承 {@link CommonVectorStoreProperties} 复用 Spring AI 标准
 * {@code initializeSchema} 字段；扩展字段只放 VikingDB 专属项。</p>
 *
 * <p>配置前缀：{@code spring.ai.vectorstore.vikingdb.*}</p>
 *
 * @author richie696
 * @since 0.1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(VikingDbVectorStoreProperties.CONFIG_PREFIX)
public class VikingDbVectorStoreProperties extends CommonVectorStoreProperties {

    public static final String CONFIG_PREFIX = "spring.ai.vectorstore.vikingdb";

    /** VikingDB Collection 名（默认 {@code vector_store}） */
    private String collectionName = "vector_store";

    /** VikingDB Index 名；默认与 collection 同名。 */
    private String indexName = "vector_store";

    /** Embedding 向量维度（默认 1536 — OpenAI text-embedding-ada-002） */
    private int embeddingDimension = 1536;

    /** VikingDB Project 名；多 project 账号必填，未填走 VikingDB 默认 project */
    private String projectName;

    /** Collection / Index 描述 */
    private String description;

    /** Index 分片数；null 表示走 VikingDB 默认值 */
    private Integer shardCount;

    /** scalar 索引字段名列表（用于 {@code SearchByVectorRequest.filter} 等值过滤） */
    private List<String> scalarIndex;

    /**
     * Spring AI metadata 键到 VikingDB 集合字段类型的映射。VikingDB 集合 schema 是显式的，
     * 因此携带未声明 metadata 键的文档会在 upsert 时被拒绝，而不是静默丢弃。
     * 此处不支持 VECTOR 类型的 metadata —— vector 字段需要 per-field dimension，
     * 本 schema 无法承载；请单独声明 vector 索引。
     */
    private Map<String, FieldForCreateVikingdbCollectionInput.FieldTypeEnum> metadataFields = Map.of();
}
