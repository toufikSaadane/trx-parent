package com.toufik.trxalertservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class TransactionWithMT103Event {
    @JsonProperty("transaction")
    private Transaction transaction;

    @JsonProperty("mt103Content")
    private String mt103Content;
}