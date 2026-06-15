package com.example.javafxclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CircuitBoard(
    Long id,
    String barcode,
    String boardModel,
    String status,
    String defectReason,
    String testedAt
){}
