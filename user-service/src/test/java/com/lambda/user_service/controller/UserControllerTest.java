package com.lambda.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambda.user_service.config.SecurityConfig;
import com.lambda.user_service.dto.*;
import com.lambda.user_service.exception.*;
import com.lambda.user_service.usermodule.UserController;
import com.lambda.user_service.usermodule.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean UserService userService;

    @Nested @DisplayName("POST /api/users/register")
    class Register {

        @Test @DisplayName("deve retornar 201 com ID quando dados válidos")
        void shouldReturn201WithId() throws Exception {
            when(userService.register(any())).thenReturn(42L);

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequestDTO("Brendo", "brendo@email.com", "senha123"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(42L));
        }

        @Test
        @DisplayName("deve retornar 409 quando email já existe")
        void shouldReturn409WhenEmailAlreadyExists() throws Exception {
            RegisterRequestDTO request = new RegisterRequestDTO("Brendo", "brendo@email.com", "senha123");
            when(userService.register(any())).thenThrow(new EmailAlreadyExistsException("brendo@email.com"));

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("deve retornar 400 quando nome está em branco")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            RegisterRequestDTO request = new RegisterRequestDTO("", "brendo@email.com", "senha123");

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando email é inválido")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            RegisterRequestDTO request = new RegisterRequestDTO("Brendo", "email-invalido", "senha123");

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando senha tem menos de 6 caracteres")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            RegisterRequestDTO request = new RegisterRequestDTO("Brendo", "brendo@email.com", "123");

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/users/login
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/users/login")
    class Login {

        @Test
        @DisplayName("deve retornar 200 com token JWT quando credenciais válidas")
        void shouldReturn200WithTokenWhenValidCredentials() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("brendo@email.com", "senha123");
            LoginResponseDTO response = new LoginResponseDTO("jwt-token-aqui");
            when(userService.login(any(LoginRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("deve retornar 401 quando credenciais inválidas")
        void shouldReturn401WhenInvalidCredentials() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("brendo@email.com", "senha-errada");
            when(userService.login(any())).thenThrow(new com.lambda.user_service.exception.InvalidCredentialsException());

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 401 quando usuário está inativo")
        void shouldReturn401WhenUserIsInactive() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("brendo@email.com", "senha123");
            when(userService.login(any())).thenThrow(new com.lambda.user_service.exception.InactiveUserException());

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("deve retornar 400 quando email não enviado")
        void shouldReturn400WhenEmailMissing() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO(null, "senha123");

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/users/{id}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetById {

        @Test
        @DisplayName("deve retornar 200 com dados do usuário quando encontrado")
        void shouldReturn200WhenUserFound() throws Exception {
            UserResponseDTO response = new UserResponseDTO(1L, "Brendo", "brendo@email.com", null, true);
            when(userService.findById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Brendo"))
                    .andExpect(jsonPath("$.email").value("brendo@email.com"))
                    .andExpect(jsonPath("$.password").doesNotExist()); // nunca expor senha
        }

        @Test
        @DisplayName("deve retornar 404 quando usuário não encontrado")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(userService.findById(99L)).thenThrow(new UserNotFoundException(99L));

            mockMvc.perform(get("/api/users/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/users/phone/{phone}
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/users/phone/{phone}")
    class GetByPhone {

        @Test
        @DisplayName("deve retornar 200 com usuário quando número encontrado")
        void shouldReturn200WhenPhoneFound() throws Exception {
            UserResponseDTO response = new UserResponseDTO(1L, "Brendo", "brendo@email.com", "5511999999999", true);
            when(userService.findByPhone("5511999999999")).thenReturn(response);

            mockMvc.perform(get("/api/users/phone/5511999999999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phone").value("5511999999999"));
        }

        @Test
        @DisplayName("deve retornar 404 quando número não está vinculado a nenhum usuário")
        void shouldReturn404WhenPhoneNotFound() throws Exception {
            when(userService.findByPhone("5511000000000")).thenThrow(new UserNotFoundException("5511000000000"));

            mockMvc.perform(get("/api/users/phone/5511000000000"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PUT /api/users/{id}/phone
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /api/users/{id}/phone")
    class UpdatePhone {

        @Test
        @DisplayName("deve retornar 200 quando número atualizado com sucesso")
        void shouldReturn200WhenPhoneUpdated() throws Exception {
            UpdatePhoneRequestDTO request = new UpdatePhoneRequestDTO("5511999999999");
            doNothing().when(userService).updatePhone(eq(1L), eq("5511999999999"));

            mockMvc.perform(put("/api/users/1/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 404 quando usuário não existe")
        void shouldReturn404WhenUserNotFound() throws Exception {
            UpdatePhoneRequestDTO request = new UpdatePhoneRequestDTO("5511999999999");
            doThrow(new UserNotFoundException(99L)).when(userService).updatePhone(eq(99L), any());

            mockMvc.perform(put("/api/users/99/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 400 quando número de telefone está em branco")
        void shouldReturn400WhenPhoneIsBlank() throws Exception {
            UpdatePhoneRequestDTO request = new UpdatePhoneRequestDTO("");

            mockMvc.perform(put("/api/users/1/phone")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
