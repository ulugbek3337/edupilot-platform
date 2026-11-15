package com.eduflow.entity;

import java.util.Arrays;

public enum LanguagePreference {
    UZ("uz"),
    RU("ru");

    private final String code;

    LanguagePreference(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static LanguagePreference fromCode(String code) {
        return Arrays.stream(values())
                .filter(language -> language.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported language: " + code));
    }
}
