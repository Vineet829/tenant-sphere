package com.tenantsphere.apartment;

import com.tenantsphere.apartment.dto.ApartmentResponse;
import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.ObjectLabel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/apartments")
public class ApartmentController {

    private final ApartmentRepository apartmentRepository;

    public ApartmentController(ApartmentRepository apartmentRepository) {
        this.apartmentRepository = apartmentRepository;
    }

    @GetMapping("/my-apartment/")
    @ObjectLabel("apartment")
    public ApartmentResponse myApartment(@AuthenticationPrincipal AppUserDetails principal) {
        return apartmentRepository
                .findFirstByTenantPkidOrderByPkidAsc(principal.getUser().getPkid())
                .map(ApartmentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found."));
    }
}
