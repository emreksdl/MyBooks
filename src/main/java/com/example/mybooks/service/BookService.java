package com.example.mybooks.service;

import com.example.mybooks.model.Book;
import com.example.mybooks.model.ReadingStatus;
import com.example.mybooks.model.User;
import com.example.mybooks.dto.CreateBookRequest;
import com.example.mybooks.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional
    public Book createBook(CreateBookRequest request, User user) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublicationYear(request.getPublicationYear());
        book.setGenre(request.getGenre());
        book.setReadingStatus(request.getReadingStatus() != null ?
                request.getReadingStatus() : ReadingStatus.NOT_STARTED);
        book.setRating(request.getRating());
        book.setNotes(request.getNotes());
        book.setUser(user);

        return bookRepository.save(book);
    }

    @Transactional
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    public List<Book> getUserBooks(Long userId) {
        return bookRepository.findByUserId(userId);
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Book> getUserBooksByStatus(Long userId, ReadingStatus status) {
        return bookRepository.findByUserIdAndReadingStatus(userId, status);
    }

    public List<Book> getUserBooksByGenre(Long userId, String genre) {
        return bookRepository.findByUserIdAndGenre(userId, genre);
    }

    @Transactional
    public Book updateBook(Long bookId, CreateBookRequest request, User user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        if (!book.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized to update this book");
        }

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublicationYear(request.getPublicationYear());
        book.setGenre(request.getGenre());
        if (request.getReadingStatus() != null) {
            book.setReadingStatus(request.getReadingStatus());
        }
        book.setRating(request.getRating());
        book.setNotes(request.getNotes());

        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long bookId, User user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        if (!book.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized to delete this book");
        }

        bookRepository.delete(book);
    }

    public Book getBookById(Long bookId, User user) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found"));

        if (!book.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized to access this book");
        }

        return book;
    }
}