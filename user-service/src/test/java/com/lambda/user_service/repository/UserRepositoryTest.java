package com.lambda.user_service.repository;

import com.lambda.user_service.usermodule.User;
import com.lambda.user_service.usermodule.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(
                User.builder()
                        .name("Brendo")
                        .email("brendo@email.com")
                        .password("hashed-password")
                        .phone("5511999999999")
                        .active(true)
                        .build()
        );
    }

    // ─────────────────────────────────────────────────────────────
    // existsByEmail()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("existsByEmail()")
    class ExistsByEmail {

        @Test
        @DisplayName("deve retornar true quando email existe")
        void shouldReturnTrueWhenEmailExists() {
            assertThat(userRepository.existsByEmail("brendo@email.com")).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando email não existe")
        void shouldReturnFalseWhenEmailNotExists() {
            assertThat(userRepository.existsByEmail("outro@email.com")).isFalse();
        }

        @Test
        @DisplayName("deve ser case-sensitive")
        void shouldBeCaseSensitive() {
            // Email no banco: brendo@email.com — busca com maiúscula deve respeitar a constraint
            assertThat(userRepository.existsByEmail("BRENDO@EMAIL.COM")).isFalse();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByEmail()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByEmail()")
    class FindByEmail {

        @Test
        @DisplayName("deve retornar usuário quando email encontrado")
        void shouldReturnUserWhenEmailFound() {
            Optional<User> result = userRepository.findByEmail("brendo@email.com");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Brendo");
        }

        @Test
        @DisplayName("deve retornar Optional vazio quando email não encontrado")
        void shouldReturnEmptyWhenEmailNotFound() {
            Optional<User> result = userRepository.findByEmail("naoexiste@email.com");

            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findByPhone()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findByPhone()")
    class FindByPhone {

        @Test
        @DisplayName("deve retornar usuário quando número encontrado")
        void shouldReturnUserWhenPhoneFound() {
            Optional<User> result = userRepository.findByPhone("5511999999999");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("brendo@email.com");
        }

        @Test
        @DisplayName("deve retornar Optional vazio quando número não encontrado")
        void shouldReturnEmptyWhenPhoneNotFound() {
            Optional<User> result = userRepository.findByPhone("5511000000000");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("deve retornar Optional vazio quando número não existe")
        void shouldReturnEmptyWhenUserHasNoPhone() {
            Optional<User> result = userRepository.findByPhone("0000000000000");
            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Constraint de unicidade
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("constraint unique email")
    class UniqueEmail {

        @Test
        @DisplayName("deve lançar exceção ao salvar dois usuários com o mesmo email")
        void shouldThrowWhenDuplicateEmail() {
            User duplicate = User.builder()
                    .name("Outro")
                    .email("brendo@email.com") // mesmo email
                    .password("hashed")
                    .active(true)
                    .build();

            assertThat(
                    org.junit.jupiter.api.Assertions.assertThrows(
                            Exception.class,
                            () -> userRepository.saveAndFlush(duplicate)
                    )
            ).isNotNull();
        }
    }
}
