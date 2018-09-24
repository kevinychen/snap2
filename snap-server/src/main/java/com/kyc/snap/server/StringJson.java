package com.kyc.snap.server;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Data;

@Data
public class StringJson {

    @JsonValue
    private final String value;
}
