package com.banco.hr.domain.model.position;

import java.util.Locale;

final class PositionText {

    private static final Locale LOCALE = Locale.forLanguageTag("pt-BR");

    private PositionText() {
    }

    static String toTitleCase(String value) {
        String[] words = value.trim().split("\\s+");
        for (int index = 0; index < words.length; index++) {
            String word = words[index].toLowerCase(LOCALE);
            if (index > 0 && word.equals("de")) {
                words[index] = word;
                continue;
            }

            int firstCodePoint = word.codePointAt(0);
            words[index] = new String(Character.toChars(firstCodePoint)).toUpperCase(LOCALE)
                    + word.substring(Character.charCount(firstCodePoint));
        }

        return String.join(" ", words);
    }
}
