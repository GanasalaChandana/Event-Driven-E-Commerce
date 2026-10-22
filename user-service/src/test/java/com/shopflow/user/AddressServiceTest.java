package com.shopflow.user;

import com.shopflow.user.dto.AddressRequest;
import com.shopflow.user.dto.AddressResponse;
import com.shopflow.user.entity.Address;
import com.shopflow.user.entity.User;
import com.shopflow.user.repository.AddressRepository;
import com.shopflow.user.repository.UserRepository;
import com.shopflow.user.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses small in-memory repositories so the default-address rules are tested end to end. */
class AddressServiceTest {

    private static final String EMAIL = "jane@test.com";
    private final UUID userId = UUID.randomUUID();
    private final List<Address> store = new ArrayList<>();
    private AddressService service;
    private int clock;

    @BeforeEach
    void setUp() {
        AddressRepository addresses = (AddressRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{AddressRepository.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "save" -> {
                            Address a = (Address) args[0];
                            if (a.getId() == null) {
                                a.setId(UUID.randomUUID());
                                a.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(clock++));
                                store.add(a);
                            }
                            return a;
                        }
                        case "delete" -> {
                            store.remove(args[0]);
                            return null;
                        }
                        case "countByUserId" -> {
                            return store.stream().filter(a -> a.getUserId().equals(args[0])).count();
                        }
                        case "findByIdAndUserId" -> {
                            return store.stream()
                                    .filter(a -> a.getId().equals(args[0]) && a.getUserId().equals(args[1]))
                                    .findFirst();
                        }
                        case "findAllByUserIdOrderByDefaultAddressDescCreatedAtDesc" -> {
                            return store.stream().filter(a -> a.getUserId().equals(args[0]))
                                    .sorted(Comparator.comparing(Address::isDefaultAddress).reversed()
                                            .thenComparing(Comparator.comparing(Address::getCreatedAt).reversed()))
                                    .toList();
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
                });

        UserRepository users = (UserRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{UserRepository.class}, (proxy, method, args) -> {
                    if (method.getName().equals("findByEmailIgnoreCase")) {
                        return EMAIL.equalsIgnoreCase((String) args[0])
                                ? Optional.of(User.builder().id(userId).email(EMAIL).build())
                                : Optional.empty();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        service = new AddressService(addresses, users);
    }

    private static AddressRequest request(String line1, Boolean makeDefault) {
        AddressRequest r = new AddressRequest();
        r.setFullName(" Jane Doe ");
        r.setLine1(line1);
        r.setLine2("  ");
        r.setCity("Irving");
        r.setState("TX");
        r.setPostalCode("75038");
        r.setCountry("us");
        r.setDefaultAddress(makeDefault);
        return r;
    }

    @Test
    void firstAddressBecomesDefaultAndIsCleaned() {
        AddressResponse saved = service.create(EMAIL, request("1 Main St", false));

        assertThat(saved.isDefaultAddress()).isTrue();
        assertThat(saved.getFullName()).isEqualTo("Jane Doe");
        assertThat(saved.getLine2()).isNull();
        assertThat(saved.getCountry()).isEqualTo("US");
    }

    @Test
    void newDefaultReplacesTheOldOne() {
        service.create(EMAIL, request("1 Main St", null));
        AddressResponse second = service.create(EMAIL, request("2 Oak Ave", true));

        List<AddressResponse> all = service.list(EMAIL);
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getId()).isEqualTo(second.getId());
        assertThat(all.stream().filter(AddressResponse::isDefaultAddress)).hasSize(1);
    }

    @Test
    void deletingTheDefaultPromotesAnotherAddress() {
        AddressResponse first = service.create(EMAIL, request("1 Main St", null));
        service.create(EMAIL, request("2 Oak Ave", false));

        service.delete(EMAIL, first.getId());

        List<AddressResponse> all = service.list(EMAIL);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).isDefaultAddress()).isTrue();
    }

    @Test
    void cannotSaveMoreThanTheLimit() {
        for (int i = 0; i < 10; i++) {
            service.create(EMAIL, request(i + " Main St", null));
        }
        assertThatThrownBy(() -> service.create(EMAIL, request("11 Main St", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at most 10");
    }

    @Test
    void cannotTouchAnotherUsersAddress() {
        Address foreign = Address.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).build();
        store.add(foreign);

        assertThatThrownBy(() -> service.delete(EMAIL, foreign.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
        assertThatThrownBy(() -> service.update(EMAIL, foreign.getId(), request("1 Main St", null)))
                .isInstanceOf(ResponseStatusException.class);
    }
}
