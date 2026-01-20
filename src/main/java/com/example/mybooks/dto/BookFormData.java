package com.example.mybooks.dto;

import com.example.mybooks.model.ReadingStatus;

public class BookFormData {

    private String title;
    private String author;
    private String isbn;
    private Integer publicationYear;
    private String genre;
    private String readingStatus;
    private Integer rating;
    private String notes;

    public BookFormData() {}

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getReadingStatus() {
        return readingStatus;
    }

    public void setReadingStatus(String readingStatus) {
        this.readingStatus = readingStatus;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public ReadingStatus getReadingStatusEnum() {
        if (readingStatus == null || readingStatus.isEmpty()) {
            return ReadingStatus.NOT_STARTED;
        }
        return ReadingStatus.valueOf(readingStatus.toUpperCase());
    }
}