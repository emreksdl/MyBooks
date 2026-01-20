package com.example.mybooks.controller;

import com.example.mybooks.model.Note;
import com.example.mybooks.model.User;
import com.example.mybooks.dto.CreateNoteRequest;
import com.example.mybooks.dto.NoteResponse;
import com.example.mybooks.service.NoteService;
import com.example.mybooks.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final UserService userService;

    public NoteController(NoteService noteService, UserService userService) {
        this.noteService = noteService;
        this.userService = userService;
    }

    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userService.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> createNote(
            @Valid @RequestBody CreateNoteRequest request,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Note note = noteService.createNote(request, user);
            NoteResponse response = new NoteResponse(note);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<NoteResponse>> getAllNotes(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<Note> notes = noteService.getUserNotes(user.getId());

        List<NoteResponse> response = notes.stream()
                .map(NoteResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getNoteById(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Note note = noteService.getNoteById(id, user);
            NoteResponse response = new NoteResponse(note);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody CreateNoteRequest request,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Note note = noteService.updateNote(id, request, user);
            NoteResponse response = new NoteResponse(note);

            return ResponseEntity.ok(response);
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
    public ResponseEntity<?> deleteNote(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            noteService.deleteNote(id, user);

            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<NoteResponse>> searchNotes(
            @RequestParam String query,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<Note> notes = noteService.searchNotes(query, user);

        List<NoteResponse> response = notes.stream()
                .map(NoteResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent/{days}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<NoteResponse>> getRecentNotes(
            @PathVariable int days,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<Note> notes = noteService.getRecentNotes(days, user);

        List<NoteResponse> response = notes.stream()
                .map(NoteResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Long>> getNotesCount(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Long count = noteService.getUserNotesCount(user.getId());

        return ResponseEntity.ok(Map.of("count", count));
    }
}