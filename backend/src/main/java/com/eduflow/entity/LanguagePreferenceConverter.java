package com.eduflow.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class LanguagePreferenceConverter implements AttributeConverter<LanguagePreference, String> {

    @Override
    public String convertToDatabaseColumn(LanguagePreference language) {
        return language == null ? null : language.getCode();
    }

    @Override
    public LanguagePreference convertToEntityAttribute(String code) {
        return code == null ? null : LanguagePreference.fromCode(code);
    }
}
