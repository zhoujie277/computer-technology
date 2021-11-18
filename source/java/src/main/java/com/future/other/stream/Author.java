package com.future.other.stream;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class Author {
    private Long id;
    private String name;
    private Integer age;
    private String intro;
    private List<Book> books;
}
