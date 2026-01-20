package com.example.mybooks.repository;

import com.example.mybooks.model.Book;
import com.example.mybooks.model.ReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByUserId(Long userId);

    List<Book> findByUserIdAndReadingStatus(Long userId, ReadingStatus status);

    List<Book> findByUserIdAndGenre(Long userId, String genre);
}