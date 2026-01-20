package com.example.mybooks.service;

import com.example.mybooks.model.Note;
import com.example.mybooks.model.User;
import com.example.mybooks.dto.CreateNoteRequest;
import com.example.mybooks.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    @Transactional
    public Note createNote(CreateNoteRequest request, User user) {
        Note note = new Note();
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setUser(user);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        return noteRepository.save(note);
    }

    public List<Note> getUserNotes(Long userId) {
        return noteRepository.findByUserId(userId);
    }

    public Note getNoteById(Long noteId, User user) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        // Access Control: user can only access their own notes
        if (!note.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized to access this note");
        }

        return note;
    }

    @Transactional
    public Note updateNote(Long noteId, CreateNoteRequest request, User user) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        // Access Control: user can only update their own notes
        if (!note.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized to update this note");
        }

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setUpdatedAt(LocalDateTime.now());

        return noteRepository.save(note);
    }

    @Transactional
    public void deleteNote(Long noteId, User user) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));

        // Access Control: user can only delete their own notes
        if (!note.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized to delete this note");
        }

        noteRepository.delete(note);
    }

    // Using raw SQL query (from repository)
    public List<Note> searchNotes(String searchTerm, User user) {
        String searchPattern = "%" + searchTerm + "%";
        return noteRepository.searchNotesByTitle(user.getId(), searchPattern);
    }

    // Using raw SQL query
    public List<Note> getRecentNotes(int days, User user) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return noteRepository.findRecentNotesByUser(user.getId(), startDate.toString());
    }

    public Long getUserNotesCount(Long userId) {
        return noteRepository.countByUserId(userId);
    }
}