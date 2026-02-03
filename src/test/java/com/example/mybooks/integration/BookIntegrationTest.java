package com.example.mybooks.integration;

import com.example.mybooks.dto.CreateBookRequest;
import com.example.mybooks.dto.CreateUserRequest;
import com.example.mybooks.dto.LoginRequest;
import com.example.mybooks.model.ReadingStatus;
import com.example.mybooks.model.Role;
import com.example.mybooks.model.User;
import com.example.mybooks.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Lab 14 - Task 2.1: Test Book API Integration
 *
 * Integration tests for book CRUD operations
 * Tests access control: users can only access their own books
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Book API Integration Tests")
class BookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User otherUser;
    private CreateBookRequest bookRequest;

    @BeforeEach
    void setUp() {
        // Clear database
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setPassword(passwordEncoder.encode("Test123!"));
        testUser.setRole(Role.USER);
        userRepository.save(testUser);

        // Create other user
        otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword(passwordEncoder.encode("Test123!"));
        otherUser.setRole(Role.USER);
        userRepository.save(otherUser);

        // Setup book request
        bookRequest = new CreateBookRequest();
        bookRequest.setTitle("Test Book");
        bookRequest.setAuthor("Test Author");
        bookRequest.setIsbn("1234567890");
        bookRequest.setPublicationYear(2024);
        bookRequest.setGenre("Fiction");
        bookRequest.setReadingStatus(ReadingStatus.READING);
        bookRequest.setRating(5);
        bookRequest.setNotes("Great book!");
    }

    @Test
    @DisplayName("Should create book when authenticated")
    void shouldCreateBookWhenAuthenticated() throws Exception {
        // Login first
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("Test123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Create book
        mockMvc.perform(post("/api/books")
                        .cookie(loginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.author").value("Test Author"))
                .andExpect(jsonPath("$.readingStatus").value("READING"));
    }

    @Test
    @DisplayName("Should deny book creation when not authenticated")
    void shouldDenyBookCreationWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should get user's own books")
    void shouldGetUsersOwnBooks() throws Exception {
        // Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("Test123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Create a book
        mockMvc.perform(post("/api/books")
                        .cookie(loginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated());

        // Get books
        mockMvc.perform(get("/api/books")
                        .cookie(loginResult.getResponse().getCookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Test Book"));
    }

    @Test
    @DisplayName("Should NOT see other user's books")
    void shouldNotSeeOtherUsersBooks() throws Exception {
        // User 1 login and create book
        LoginRequest user1Login = new LoginRequest();
        user1Login.setEmail("test@test.com");
        user1Login.setPassword("Test123!");

        MvcResult user1LoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Login)))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(post("/api/books")
                        .cookie(user1LoginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated());

        // User 2 login
        LoginRequest user2Login = new LoginRequest();
        user2Login.setEmail("other@test.com");
        user2Login.setPassword("Test123!");

        MvcResult user2LoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Login)))
                .andExpect(status().isOk())
                .andReturn();

        // User 2 should NOT see User 1's books
        mockMvc.perform(get("/api/books")
                        .cookie(user2LoginResult.getResponse().getCookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Should update own book")
    void shouldUpdateOwnBook() throws Exception {
        // Login and create book
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("Test123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult createResult = mockMvc.perform(post("/api/books")
                        .cookie(loginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long bookId = objectMapper.readTree(responseBody).get("id").asLong();

        // Update book
        bookRequest.setTitle("Updated Title");
        bookRequest.setReadingStatus(ReadingStatus.COMPLETED);

        mockMvc.perform(put("/api/books/" + bookId)
                        .cookie(loginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.readingStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("Should NOT update other user's book")
    void shouldNotUpdateOtherUsersBook() throws Exception {
        // User 1 creates book
        LoginRequest user1Login = new LoginRequest();
        user1Login.setEmail("test@test.com");
        user1Login.setPassword("Test123!");

        MvcResult user1LoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Login)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult createResult = mockMvc.perform(post("/api/books")
                        .cookie(user1LoginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long bookId = objectMapper.readTree(responseBody).get("id").asLong();

        // User 2 tries to update
        LoginRequest user2Login = new LoginRequest();
        user2Login.setEmail("other@test.com");
        user2Login.setPassword("Test123!");

        MvcResult user2LoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Login)))
                .andExpect(status().isOk())
                .andReturn();

        bookRequest.setTitle("Hacked!");

        mockMvc.perform(put("/api/books/" + bookId)
                        .cookie(user2LoginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should delete own book")
    void shouldDeleteOwnBook() throws Exception {
        // Login and create book
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("Test123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult createResult = mockMvc.perform(post("/api/books")
                        .cookie(loginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long bookId = objectMapper.readTree(responseBody).get("id").asLong();

        // Delete book
        mockMvc.perform(delete("/api/books/" + bookId)
                        .cookie(loginResult.getResponse().getCookies()))
                .andExpect(status().isNoContent());  // 204

        // Verify book is deleted
        mockMvc.perform(get("/api/books")
                        .cookie(loginResult.getResponse().getCookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Should NOT delete other user's book")
    void shouldNotDeleteOtherUsersBook() throws Exception {
        // User 1 creates book
        LoginRequest user1Login = new LoginRequest();
        user1Login.setEmail("test@test.com");
        user1Login.setPassword("Test123!");

        MvcResult user1LoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Login)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult createResult = mockMvc.perform(post("/api/books")
                        .cookie(user1LoginResult.getResponse().getCookies())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long bookId = objectMapper.readTree(responseBody).get("id").asLong();

        // User 2 tries to delete
        LoginRequest user2Login = new LoginRequest();
        user2Login.setEmail("other@test.com");
        user2Login.setPassword("Test123!");

        MvcResult user2LoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Login)))
                .andExpect(status().isOk())
                .andReturn();

        mockMvc.perform(delete("/api/books/" + bookId)
                        .cookie(user2LoginResult.getResponse().getCookies()))
                .andExpect(status().isForbidden());
    }
}