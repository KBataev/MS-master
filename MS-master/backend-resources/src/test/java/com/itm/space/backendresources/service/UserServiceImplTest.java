package com.itm.space.backendresources.service;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.api.response.UserResponse;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private Keycloak keycloakClient;
    private UserMapper userMapper;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        keycloakClient = mock(Keycloak.class);
        userMapper = mock(UserMapper.class);
        userService = new UserServiceImpl(keycloakClient, userMapper);
    }

    @Test
    void shouldCreateUserSuccessfully() {
        UserRequest request = new UserRequest("username", "email@example.com", "password", "FirstName", "LastName");

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        Response response = mock(Response.class);
        Response.StatusType statusType = mock(Response.StatusType.class);

        // Используем фиксированный UUID для Location
        UUID userId = UUID.randomUUID();
        URI location = URI.create("http://backend-keycloak-auth:9090/api/users/" + userId.toString());

        when(keycloakClient.realm(any())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);  // Статус "Created"
        when(response.getStatusInfo()).thenReturn(statusType);
        when(statusType.getStatusCode()).thenReturn(201); // Статус "Created"
        when(response.getLocation()).thenReturn(location);  // Мокаем Location

        userService.createUser(request);

        verify(usersResource, times(1)).create(any(UserRepresentation.class));
    }



    @Test
    void shouldThrowExceptionWhenKeycloakFails() {
        UserRequest request = new UserRequest("username", "email@example.com", "password", "FirstName", "LastName");

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);

        when(keycloakClient.realm(any())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class)))
                .thenThrow(new WebApplicationException("Keycloak error"));

        // Act & Assert
        assertThrows(BackendResourcesException.class, () -> userService.createUser(request));

        verify(usersResource, times(1)).create(any(UserRepresentation.class));
    }

    @Test
    void shouldGetUserByIdSuccessfully() {
        UUID userId = UUID.randomUUID();

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setFirstName("FirstName");
        userRepresentation.setLastName("LastName");
        userRepresentation.setEmail("email@example.com");

        List<String> roles = List.of("role1", "role2");
        List<String> groups = List.of("group1", "group2");

        // Мокаем поведение метода маппера
        when(userMapper.userRepresentationToUserResponse(any(), any(), any()))
                .thenReturn(new UserResponse("FirstName", "LastName", "email@example.com", roles, groups));

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        MappingsRepresentation mappingsRepresentation = mock(MappingsRepresentation.class);

        // Мокаем Keycloak API
        when(keycloakClient.realm(any())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(any(String.class))).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);
        when(userResource.roles()).thenReturn(roleMappingResource);

        // Создаем список ролей
        RoleRepresentation role1 = new RoleRepresentation("role1", null, Boolean.FALSE);
        RoleRepresentation role2 = new RoleRepresentation("role2", null, Boolean.FALSE);

        // Мокаем возвращение списка ролей через MappingsRepresentation
        when(mappingsRepresentation.getRealmMappings()).thenReturn(List.of(role1, role2));
        when(roleMappingResource.getAll()).thenReturn(mappingsRepresentation);

        // Act
        UserResponse userResponse = userService.getUserById(userId);

        // Assert
        assertNotNull(userResponse);
        assertEquals("FirstName", userResponse.getFirstName());
        assertEquals("LastName", userResponse.getLastName());
        assertEquals("email@example.com", userResponse.getEmail());
        assertEquals(2, userResponse.getRoles().size());
        assertEquals(2, userResponse.getGroups().size());
    }




    @Test
    void shouldThrowExceptionWhenGettingUserByIdFails() {
        UUID userId = UUID.randomUUID();

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);

        when(keycloakClient.realm(any())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(any(String.class))).thenThrow(new RuntimeException("Keycloak error"));

        // Act & Assert
        assertThrows(BackendResourcesException.class, () -> userService.getUserById(userId));
    }
}
