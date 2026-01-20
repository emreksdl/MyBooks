package com.example.mybooks.repository;

import com.example.mybooks.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    // JPA Method Query
    List<Note> findByUserId(Long userId);

    // JPA Method Query
    Optional<Note> findByIdAndUserId(Long id, Long userId);

    // Raw SQL Query with Prepared Statement (prevents SQL injection)
    @Query(value = "SELECT * FROM notes WHERE user_id = :userId AND created_at >= :startDate ORDER BY created_at DESC",
            nativeQuery = true)
    List<Note> findRecentNotesByUser(@Param("userId") Long userId, @Param("startDate") String startDate);

    // Raw SQL Query - Search by title (with prepared statement)
    @Query(value = "SELECT * FROM notes WHERE user_id = :userId AND LOWER(title) LIKE LOWER(:searchTerm) ORDER BY created_at DESC",
            nativeQuery = true)
    List<Note> searchNotesByTitle(@Param("userId") Long userId, @Param("searchTerm") String searchTerm);

    // JPA Query - Count user's notes
    @Query("SELECT COUNT(n) FROM Note n WHERE n.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    // Raw SQL Query - Delete old notes (admin utility)
    @Query(value = "DELETE FROM notes WHERE created_at < :beforeDate", nativeQuery = true)
    void deleteOldNotes(@Param("beforeDate") String beforeDate);
}