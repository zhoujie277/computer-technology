package com.future.basis;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class CharacterCode {

    public static void main(String[] args) {
        String defaultStr = " ";
        log.debug("defaultStr length: {}", defaultStr.length());
        String asciiStr = "h";
        log.debug("asciiStr length: {}", asciiStr.length());
        String unicodeStr = "世界";
        log.debug("unicodeStr length: {} codePoint: {}", unicodeStr.length(), unicodeStr.codePointCount(0, unicodeStr.length()));
        byte[] utf8Str = "世界".getBytes(StandardCharsets.UTF_8);
        log.debug("unicodeStr length: {}", utf8Str.length);
        char[] chars = {'h'};
    }
}
