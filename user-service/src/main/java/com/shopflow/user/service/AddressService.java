package com.shopflow.user.service;

import com.shopflow.user.dto.AddressRequest;
import com.shopflow.user.dto.AddressResponse;
import com.shopflow.user.entity.Address;
import com.shopflow.user.entity.User;
import com.shopflow.user.repository.AddressRepository;
import com.shopflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** A user's address book. Every operation is scoped to the caller, so one user can never touch another's addresses. */
@Service
@RequiredArgsConstructor
public class AddressService {

    static final int MAX_ADDRESSES = 10;

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<AddressResponse> list(String email) {
        UUID userId = userId(email);
        return addressRepository.findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                .map(AddressResponse::from).toList();
    }

    @Transactional
    public AddressResponse create(String email, AddressRequest request) {
        UUID userId = userId(email);
        if (addressRepository.countByUserId(userId) >= MAX_ADDRESSES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You can save at most " + MAX_ADDRESSES + " addresses — delete one first");
        }

        boolean first = addressRepository.countByUserId(userId) == 0;
        boolean makeDefault = first || Boolean.TRUE.equals(request.getDefaultAddress());
        if (makeDefault) {
            clearDefault(userId);
        }

        Address address = Address.builder().userId(userId).build();
        apply(address, request);
        address.setDefaultAddress(makeDefault);
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(String email, UUID id, AddressRequest request) {
        UUID userId = userId(email);
        Address address = find(id, userId);

        apply(address, request);
        if (Boolean.TRUE.equals(request.getDefaultAddress()) && !address.isDefaultAddress()) {
            clearDefault(userId);
            address.setDefaultAddress(true);
        }
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public void delete(String email, UUID id) {
        UUID userId = userId(email);
        Address address = find(id, userId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);

        if (wasDefault) {
            // Promote the newest remaining address so the user always has a default
            addressRepository.findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                    .filter(a -> !a.getId().equals(id))
                    .findFirst()
                    .ifPresent(a -> {
                        a.setDefaultAddress(true);
                        addressRepository.save(a);
                    });
        }
    }

    private void clearDefault(UUID userId) {
        for (Address a : addressRepository.findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId)) {
            if (a.isDefaultAddress()) {
                a.setDefaultAddress(false);
                addressRepository.save(a);
            }
        }
    }

    private Address find(UUID id, UUID userId) {
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
    }

    private UUID userId(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private static void apply(Address address, AddressRequest r) {
        address.setFullName(r.getFullName().trim());
        address.setLine1(r.getLine1().trim());
        address.setLine2(blankToNull(r.getLine2()));
        address.setCity(r.getCity().trim());
        address.setState(r.getState().trim());
        address.setPostalCode(r.getPostalCode().trim());
        address.setCountry(r.getCountry().trim().toUpperCase(Locale.ROOT));
        address.setPhone(blankToNull(r.getPhone()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
