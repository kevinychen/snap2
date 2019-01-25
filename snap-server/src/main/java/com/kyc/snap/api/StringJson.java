package com.kyc.snap.api;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Data;

@Data
public class StringJson {

    @JsonValue
    private final String value;
}
