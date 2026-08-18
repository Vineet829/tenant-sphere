package com.tenantsphere.apartment.dto;

import com.tenantsphere.apartment.Apartment;
import java.time.Instant;
import java.util.UUID;

public record ApartmentResponse(
        UUID id, Instant createdAt, String unitNumber, String building, Integer floor) {

    public static ApartmentResponse from(Apartment apartment) {
        return new ApartmentResponse(
                apartment.getId(),
                apartment.getCreatedAt(),
                apartment.getUnitNumber(),
                apartment.getBuilding(),
                apartment.getFloor());
    }
}
