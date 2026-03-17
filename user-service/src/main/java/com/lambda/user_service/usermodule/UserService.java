package com.lambda.user_service.usermodule;

import com.lambda.user_service.dto.*;
import com.lambda.user_service.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public Long register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException(dto.email());
        }

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .active(true)
                .build();

        return userRepository.save(user).getId();
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new InactiveUserException();
        }

        return new LoginResponseDTO(jwtService.generateToken(user));
    }

    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toDTO(user);
    }

    public UserResponseDTO findByPhone(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException(phone));
        return toDTO(user);
    }

    public void updatePhone(Long id, String phone) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        user.setPhone(phone);
        userRepository.save(user);
    }

    private UserResponseDTO toDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.isActive()
        );
    }
}
