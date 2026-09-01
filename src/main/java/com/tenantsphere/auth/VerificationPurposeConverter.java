package com.tenantsphere.auth;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VerificationPurposeConverter
        implements AttributeConverter<VerificationPurpose, String> {

    @Override
    public String convertToDatabaseColumn(VerificationPurpose attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public VerificationPurpose convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VerificationPurpose.fromValue(dbData);
    }
}
