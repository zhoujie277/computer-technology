package com.future.other.stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
class Book {
    private Long id;
    private String name;
    private String category;
    private Integer score;
    private String intro;
}
