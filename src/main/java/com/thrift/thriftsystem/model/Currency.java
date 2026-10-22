package com.thrift.thriftsystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "currencies")
public class Currency {

    @Id private String id;

    @Indexed(unique = true)
    private String code;

    private String name;
    private String symbol;

    private BigDecimal exchangeRate;

    @Builder.Default private boolean active = true;

    @CreatedDate private LocalDateTime createdDate;

    @LastModifiedDate private LocalDateTime updatedDate;
}
