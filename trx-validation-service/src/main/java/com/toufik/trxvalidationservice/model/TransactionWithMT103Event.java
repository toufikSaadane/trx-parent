package com.toufik.trxvalidationservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class TransactionWithMT103Event {
    @JsonProperty("transaction")
    private Transaction transaction;

    @JsonProperty("mt103Content")
    private String mt103Content;
}