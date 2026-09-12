package com.tenantsphere.apartment;

import com.tenantsphere.apartment.dto.ApartmentResponse;
import com.tenantsphere.apartment.dto.CreateApartmentRequest;
import com.tenantsphere.auth.AppUserDetails;
import com.tenantsphere.common.ObjectLabel;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/apartments")
public class ApartmentController {

    private final ApartmentRepository apartmentRepository;
    private final ApartmentService apartmentService;

    public ApartmentController(
            ApartmentRepository apartmentRepository, ApartmentService apartmentService) {
        this.apartmentRepository = apartmentRepository;
        this.apartmentService = apartmentService;
    }

    @PostMapping("/add/")
    @ObjectLabel("apartment")
    @ResponseStatus(HttpStatus.CREATED)
    public ApartmentResponse add(
            @Valid @RequestBody CreateApartmentRequest request,
            @AuthenticationPrincipal AppUserDetails principal) {
        return ApartmentResponse.from(apartmentService.create(principal.getUser(), request));
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
