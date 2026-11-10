package com.paras.restaurant.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * A user's request to become a restaurant owner.
 * The document id is the user's Keycloak id, so a user can only ever have one request
 * (re-applying after a rejection simply replaces it). Timestamps are UTC.
 */
@Document(indexName = "owner-requests")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OwnerRequest {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String username;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Text)
    private String message;

    @Field(type = FieldType.Keyword)
    private OwnerRequestStatus status;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private String reviewedBy;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime reviewedAt;

    @Field(type = FieldType.Text)
    private String rejectionReason;
}
