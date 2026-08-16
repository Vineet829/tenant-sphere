package com.tenantsphere.issue;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IssueStatusConverter implements AttributeConverter<IssueStatus, String> {

    @Override
    public String convertToDatabaseColumn(IssueStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public IssueStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : IssueStatus.fromValue(dbData);
    }
}
