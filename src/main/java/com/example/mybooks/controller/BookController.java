package com.example.mybooks.controller;

import com.example.mybooks.model.Book;
import com.example.mybooks.model.ReadingStatus;
import com.example.mybooks.model.User;
import com.example.mybooks.dto.CreateBookRequest;
import com.example.mybooks.service.BookService;
import com.example.mybooks.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final UserService userService;

    public BookController(BookService bookService, UserService userService) {
        this.bookService = bookService;
        this.userService = userService;
    }

    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> createBook(
            @Valid @RequestBody CreateBookRequest request,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            Authentication authentication) {
        try {
            if (contentType == null || !contentType.contains("application/json")) {
                Map<String, String> error = Map.of("error", "Content-Type must be application/json");
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
            }

            User user = getCurrentUser(authentication);
            Book book = bookService.createBook(request, user);

            if (userAgent != null) {
                System.out.println("Book created via: " + userAgent);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(book);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getAllBooks(
            @RequestHeader(value = "Accept", defaultValue = "application/json") String accept,
            Authentication authentication) {

        if (!accept.contains("application/json") && !accept.contains("*/*")) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                    .body(Map.of("error", "This endpoint only supports application/json"));
        }

        User user = getCurrentUser(authentication);
        List<Book> books = bookService.getUserBooks(user.getId());
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getBookById(
            @PathVariable Long id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Book book = bookService.getBookById(id, user);

            String etag = "\"book-" + book.getId() + "\"";

            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            return ResponseEntity.ok()
                    .header("ETag", etag)
                    .body(book);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Book>> getBooksByStatus(
            @PathVariable ReadingStatus status,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<Book> books = bookService.getUserBooksByStatus(user.getId(), status);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/genre/{genre}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Book>> getBooksByGenre(
            @PathVariable String genre,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<Book> books = bookService.getUserBooksByGenre(user.getId(), genre);
        return ResponseEntity.ok(books);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody CreateBookRequest request,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            Authentication authentication) {
        try {
            if (contentType == null || !contentType.contains("application/json")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(Map.of("error", "Content-Type must be application/json"));
            }

            User user = getCurrentUser(authentication);
            Book book = bookService.updateBook(id, request, user);
            return ResponseEntity.ok(book);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> deleteBook(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            bookService.deleteBook(id, user);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Book>> getAllBooksAdmin() {
        List<Book> allBooks = bookService.getAllBooks();
        return ResponseEntity.ok(allBooks);
    }
}