package com.example.kafka.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Boolean → Y/N 컨버터
 * - true → 'Y'
 * - false → 'N'
 */
@Converter
public class BooleanToYNConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return "N";
        }
        return attribute ? "Y" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return Boolean.FALSE;
        }
        return "Y".equalsIgnoreCase(dbData);
    }
}
