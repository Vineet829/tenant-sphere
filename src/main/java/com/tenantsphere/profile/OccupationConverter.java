package com.tenantsphere.profile;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OccupationConverter implements AttributeConverter<Occupation, String> {

    @Override
    public String convertToDatabaseColumn(Occupation attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public Occupation convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Occupation.fromValue(dbData);
    }
}
