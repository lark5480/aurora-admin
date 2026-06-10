package com.aurora.admin.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.aurora.admin.entity.Product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private Long id;

    // 使用 IK 智能分词器（粗粒度，适合搜索）
    @Field(type = FieldType.Text, analyzer = "ik_smart", searchAnalyzer = "ik_smart")
    private String name;

    // 使用 IK 最大分词器（细粒度，提高召回率）
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    private String coverImage;

    private BigDecimal price;

    private Integer stock;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createTime;

    public static ProductDocument from(Product product, String categoryName) {
        ProductDocument doc = new ProductDocument();
        doc.setId(product.getId());
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setCategoryId(product.getCategoryId());
        doc.setCategoryName(categoryName);
        doc.setCoverImage(product.getCoverImage());
        doc.setPrice(product.getPrice());
        doc.setStock(product.getStock());
        doc.setStatus(product.getStatus());
        doc.setCreateTime(product.getCreateTime());
        return doc;
    }
}
