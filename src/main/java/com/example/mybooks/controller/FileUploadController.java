package com.example.mybooks.controller;

import com.example.mybooks.model.Book;
import com.example.mybooks.model.User;
import com.example.mybooks.dto.BookFormData;
import com.example.mybooks.service.BookService;
import com.example.mybooks.service.FileStorageService;
import com.example.mybooks.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final BookService bookService;
    private final UserService userService;

    public FileUploadController(FileStorageService fileStorageService,
                                BookService bookService,
                                UserService userService) {
        this.fileStorageService = fileStorageService;
        this.bookService = bookService;
        this.userService = userService;
    }

    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @PostMapping("/book-cover")
    public ResponseEntity<?> uploadBookCover(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bookId") Long bookId,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            Authentication authentication) {

        try {
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(Map.of("error", "Content-Type must be multipart/form-data"));
            }

            User user = getCurrentUser(authentication);
            Book book = bookService.getBookById(bookId, user);
            String filename = fileStorageService.storeFile(file);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("filename", filename);
            response.put("bookId", bookId);
            response.put("url", "/api/upload/book-covers/" + filename);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/book-covers/{filename:.+}")
    public ResponseEntity<Resource> getBookCover(@PathVariable String filename) {
        try {
            Path filePath = fileStorageService.getFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "image/jpeg";
            if (filename.endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.endsWith(".gif")) {
                contentType = "image/gif";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/book-with-form", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> createBookFromForm(
            @ModelAttribute BookFormData formData,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            Authentication authentication) {

        try {
            if (contentType == null || !contentType.startsWith("application/x-www-form-urlencoded")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(Map.of("error", "Content-Type must be application/x-www-form-urlencoded"));
            }

            User user = getCurrentUser(authentication);

            Book book = new Book();
            book.setTitle(formData.getTitle());
            book.setAuthor(formData.getAuthor());
            book.setIsbn(formData.getIsbn());
            book.setPublicationYear(formData.getPublicationYear());
            book.setGenre(formData.getGenre());
            book.setReadingStatus(formData.getReadingStatusEnum());
            book.setRating(formData.getRating());
            book.setNotes(formData.getNotes());
            book.setUser(user);

            Book savedBook = bookService.saveBook(book);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}