package com.itm.space.backendresources.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.service.UserService;
import org.apache.catalina.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@ExtendWith(SpringExtension.class)
@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {SecurityConfig.class, UserController.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void shouldCreateUser() throws Exception {
        String requestBody = """
    {
        "username": "testuser",
        "email": "testuser@example.com",
        "password": "password",
        "firstName": "Test",
        "lastName": "User"
    }
    """;

        // Настройка поведения void метода
        Mockito.doNothing().when(userService).createUser(Mockito.any());

        // Выполнение запроса
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk()); // Проверяем статус

        // Убедитесь, что createUser был вызван
        Mockito.verify(userService, Mockito.times(1)).createUser(Mockito.any());
    }



    @Test
    @WithMockUser(username = "moderator", roles = {"MODERATOR"})
    void shouldReturnUserById() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse("FirstName", "LastName", "email@example.com", List.of("ROLE_USER"), List.of("Group1"));

        Mockito.when(userService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(response.getFirstName()))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }
}

