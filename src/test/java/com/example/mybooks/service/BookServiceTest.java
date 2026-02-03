package com.example.mybooks.service;

import com.example.mybooks.dto.CreateBookRequest;
import com.example.mybooks.model.Book;
import com.example.mybooks.model.ReadingStatus;
import com.example.mybooks.model.Role;
import com.example.mybooks.model.User;
import com.example.mybooks.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Lab 14 - Task 1.2: Test CRUD Operations
 *
 * Unit tests for BookService - testing CRUD operations
 * Test access control: users can only access their own books
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private User testUser;
    private User otherUser;
    private Book testBook;
    private CreateBookRequest createRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setRole(Role.USER);

        // Setup other user (for access control tests)
        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@test.com");
        otherUser.setRole(Role.USER);

        // Setup test book
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        testBook.setAuthor("Test Author");
        testBook.setIsbn("1234567890");
        testBook.setPublicationYear(2024);
        testBook.setGenre("Fiction");
        testBook.setReadingStatus(ReadingStatus.NOT_STARTED);
        testBook.setRating(5);
        testBook.setNotes("Great book!");
        testBook.setUser(testUser);

        // Setup create request
        createRequest = new CreateBookRequest();
        createRequest.setTitle("New Book");
        createRequest.setAuthor("New Author");
        createRequest.setIsbn("0987654321");
        createRequest.setPublicationYear(2025);
        createRequest.setGenre("Science Fiction");
        createRequest.setReadingStatus(ReadingStatus.READING);
        createRequest.setRating(4);
        createRequest.setNotes("Interesting");
    }

    @Test
    @DisplayName("Should create book successfully")
    void shouldCreateBookSuccessfully() {
        // Arrange
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // Act
        Book result = bookService.createBook(createRequest, testUser);

        // Assert
        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        assertEquals(testUser.getId(), result.getUser().getId());
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    @DisplayName("Should set default reading status when not provided")
    void shouldSetDefaultReadingStatusWhenNotProvided() {
        // Arrange
        createRequest.setReadingStatus(null);
        Book bookWithDefaultStatus = new Book();
        bookWithDefaultStatus.setReadingStatus(ReadingStatus.NOT_STARTED);
        bookWithDefaultStatus.setUser(testUser);

        when(bookRepository.save(any(Book.class))).thenReturn(bookWithDefaultStatus);

        // Act
        Book result = bookService.createBook(createRequest, testUser);

        // Assert
        assertEquals(ReadingStatus.NOT_STARTED, result.getReadingStatus());
    }

    @Test
    @DisplayName("Should get user books")
    void shouldGetUserBooks() {
        // Arrange
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByUserId(testUser.getId())).thenReturn(books);

        // Act
        List<Book> result = bookService.getUserBooks(testUser.getId());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
        verify(bookRepository, times(1)).findByUserId(testUser.getId());
    }

    @Test
    @DisplayName("Should get user books by status")
    void shouldGetUserBooksByStatus() {
        // Arrange
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByUserIdAndReadingStatus(testUser.getId(), ReadingStatus.NOT_STARTED))
                .thenReturn(books);

        // Act
        List<Book> result = bookService.getUserBooksByStatus(testUser.getId(), ReadingStatus.NOT_STARTED);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(ReadingStatus.NOT_STARTED, result.get(0).getReadingStatus());
    }

    @Test
    @DisplayName("Should get user books by genre")
    void shouldGetUserBooksByGenre() {
        // Arrange
        List<Book> books = Arrays.asList(testBook);
        when(bookRepository.findByUserIdAndGenre(testUser.getId(), "Fiction"))
                .thenReturn(books);

        // Act
        List<Book> result = bookService.getUserBooksByGenre(testUser.getId(), "Fiction");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Fiction", result.get(0).getGenre());
    }

    @Test
    @DisplayName("Should update book successfully")
    void shouldUpdateBookSuccessfully() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);

        // Act
        Book result = bookService.updateBook(1L, createRequest, testUser);

        // Assert
        assertNotNull(result);
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent book")
    void shouldThrowExceptionWhenUpdatingNonExistentBook() {
        // Arrange
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.updateBook(999L, createRequest, testUser)
        );

        assertEquals("Book not found", exception.getMessage());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Should throw exception when user tries to update another user's book")
    void shouldThrowExceptionWhenUpdatingOtherUsersBook() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // Act & Assert
        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> bookService.updateBook(1L, createRequest, otherUser)
        );

        assertEquals("Unauthorized to update this book", exception.getMessage());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("Should delete book successfully")
    void shouldDeleteBookSuccessfully() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // Act
        bookService.deleteBook(1L, testUser);

        // Assert
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).delete(testBook);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent book")
    void shouldThrowExceptionWhenDeletingNonExistentBook() {
        // Arrange
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bookService.deleteBook(999L, testUser)
        );

        assertEquals("Book not found", exception.getMessage());
        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    @DisplayName("Should throw exception when user tries to delete another user's book")
    void shouldThrowExceptionWhenDeletingOtherUsersBook() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // Act & Assert
        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> bookService.deleteBook(1L, otherUser)
        );

        assertEquals("Unauthorized to delete this book", exception.getMessage());
        verify(bookRepository, never()).delete(any(Book.class));
    }

    @Test
    @DisplayName("Should get book by id successfully")
    void shouldGetBookByIdSuccessfully() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // Act
        Book result = bookService.getBookById(1L, testUser);

        // Assert
        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when accessing another user's book")
    void shouldThrowExceptionWhenAccessingOtherUsersBook() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // Act & Assert
        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> bookService.getBookById(1L, otherUser)
        );

        assertEquals("Unauthorized to access this book", exception.getMessage());
    }

    @Test
    @DisplayName("Should save book successfully")
    void shouldSaveBookSuccessfully() {
        // Arrange
        when(bookRepository.save(testBook)).thenReturn(testBook);

        // Act
        Book result = bookService.saveBook(testBook);

        // Assert
        assertNotNull(result);
        assertEquals("Test Book", result.getTitle());
        verify(bookRepository, times(1)).save(testBook);
    }
}