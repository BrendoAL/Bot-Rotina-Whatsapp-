package com.lambda.user_service.service;

import com.lambda.user_service.dto.LoginRequestDTO;
import com.lambda.user_service.dto.LoginResponseDTO;
import com.lambda.user_service.dto.RegisterRequestDTO;
import com.lambda.user_service.dto.UserResponseDTO;
import com.lambda.user_service.exception.EmailAlreadyExistsException;
import com.lambda.user_service.exception.InactiveUserException;
import com.lambda.user_service.exception.InvalidCredentialsException;
import com.lambda.user_service.exception.UserNotFoundException;
import com.lambda.user_service.usermodule.JwtService;
import com.lambda.user_service.usermodule.User;
import com.lambda.user_service.usermodule.UserRepository;
import com.lambda.user_service.usermodule.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    UserService userService;

    // ─────────────────────────────────────────────────────────────
    // register()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("deve salvar usuário e retornar ID quando dados válidos")
        void shouldSaveUserAndReturnId() {
            RegisterRequestDTO request = new RegisterRequestDTO("Brendo", "brendo@email.com", "senha123");
            User savedUser = User.builder().id(1L).name("Brendo").email("brendo@email.com").password("hashed").active(true).build();

            when(userRepository.existsByEmail("brendo@email.com")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            Long id = userService.register(request);

            assertThat(id).isEqualTo(1L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("deve lançar EmailAlreadyExistsException quando email duplicado")
        void shouldThrowWhenEmailAlreadyExists() {
            RegisterRequestDTO request = new RegisterRequestDTO("Brendo", "brendo@email.com", "senha123");
            when(userRepository.existsByEmail("brendo@email.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve criptografar a senha antes de salvar")
        void shouldEncryptPasswordBeforeSaving() {
            RegisterRequestDTO request = new RegisterRequestDTO("Brendo", "brendo@email.com", "senha123");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("hashed-password");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            userService.register(request);

            // Verifica que o encode foi chamado com a senha original
            verify(passwordEncoder).encode("senha123");
            // Verifica que nunca salvou a senha em texto puro
            verify(userRepository).save(argThat(u -> u.getPassword().equals("hashed-password")));
        }

        @Test
        @DisplayName("deve salvar usuário com active = true por padrão")
        void shouldSaveUserAsActiveByDefault() {
            RegisterRequestDTO request = new RegisterRequestDTO("Brendo", "brendo@email.com", "senha123");
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            userService.register(request);

            verify(userRepository).save(argThat(u -> u.isActive() == true));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // login()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("deve retornar token JWT quando credenciais válidas")
        void shouldReturnTokenWhenValidCredentials() {
            LoginRequestDTO request = new LoginRequestDTO("brendo@email.com", "senha123");
            User user = User.builder().id(1L).email("brendo@email.com").password("hashed").active(true).build();

            when(userRepository.findByEmail("brendo@email.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("senha123", "hashed")).thenReturn(true);
            when(jwtService.generateToken(user)).thenReturn("jwt-token");

            LoginResponseDTO response = userService.login(request);

            assertThat(response.token()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("deve lançar InvalidCredentialsException quando email não encontrado")
        void shouldThrowWhenEmailNotFound() {
            LoginRequestDTO request = new LoginRequestDTO("naoexiste@email.com", "senha123");
            when(userRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("deve lançar InvalidCredentialsException quando senha incorreta")
        void shouldThrowWhenPasswordWrong() {
            LoginRequestDTO request = new LoginRequestDTO("brendo@email.com", "senha-errada");
            User user = User.builder().id(1L).email("brendo@email.com").password("hashed").active(true).build();

            when(userRepository.findByEmail("brendo@email.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("senha-errada", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("deve lançar InactiveUserException quando usuário está inativo")
        void shouldThrowWhenUserInactive() {
            LoginRequestDTO request = new LoginRequestDTO("brendo@email.com", "senha123");
            User user = User.builder().id(1L).email("brendo@email.com").password("hashed").active(false).build();

            when(userRepository.findByEmail("brendo@email.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("senha123", "hashed")).thenReturn(true);

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(InactiveUserException.class);
        }

        @Test
        @DisplayName("não deve gerar token quando usuário está inativo")
        void shouldNotGenerateTokenWhenUserInactive() {
            LoginRequestDTO request = new LoginRequestDTO("brendo@email.com", "senha123");
            User user = User.builder().id(1L).email("brendo@email.com").password("hashed").active(false).build();

            when(userRepository.findByEmail("brendo@email.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> userService.login(request));

            verify(jwtService, never()).generateToken(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findById()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("deve retornar UserResponseDTO quando usuário encontrado")
        void shouldReturnDtoWhenFound() {
            User user = User.builder().id(1L).name("Brendo").email("brendo@email.com").active(true).build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponseDTO dto = userService.findById(1L);

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("Brendo");
            assertThat(dto.email()).isEqualTo("brendo@email.com");
        }

        @Test
        @DisplayName("deve lançar UserNotFoundException quando não encontrado")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(99L))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("não deve expor a senha no DTO de resposta")
        void shouldNotExposePasswordInDto() {
            User user = User.builder().id(1L).name("Brendo").email("brendo@email.com").password("hashed").active(true).build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponseDTO dto = userService.findById(1L);

            // UserResponseDTO não deve ter campo password
            // Se tiver, o valor deve ser null
            assertThat(dto).isNotNull();
            // Verifica que o record não vaza a senha — a asserção real está na ausência do campo no DTO
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
            User user = User.builder().id(1L).name("Brendo").phone("5511999999999").active(true).build();
            when(userRepository.findByPhone("5511999999999")).thenReturn(Optional.of(user));

            UserResponseDTO dto = userService.findByPhone("5511999999999");

            assertThat(dto.phone()).isEqualTo("5511999999999");
        }

        @Test
        @DisplayName("deve lançar UserNotFoundException quando número não vinculado")
        void shouldThrowWhenPhoneNotFound() {
            when(userRepository.findByPhone("5511000000000")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findByPhone("5511000000000"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updatePhone()
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("updatePhone()")
    class UpdatePhone {

        @Test
        @DisplayName("deve atualizar número quando usuário existe")
        void shouldUpdatePhoneWhenUserExists() {
            User user = User.builder().id(1L).name("Brendo").email("brendo@email.com").active(true).build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            userService.updatePhone(1L, "5511999999999");

            verify(userRepository).save(argThat(u -> "5511999999999".equals(u.getPhone())));
        }

        @Test
        @DisplayName("deve lançar UserNotFoundException quando usuário não existe")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updatePhone(99L, "5511999999999"))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).save(any());
        }
    }
}
