package com.lambda.activity_service.activitymodule;

import com.lambda.activity_service.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.url:http://user-service:8081}")
    private String userServiceUrl;

    public void validateUser(Long userId) {
        try {
            restTemplate.getForObject(userServiceUrl + "/api/users/" + userId, Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException(userId);
        }
    }
}
