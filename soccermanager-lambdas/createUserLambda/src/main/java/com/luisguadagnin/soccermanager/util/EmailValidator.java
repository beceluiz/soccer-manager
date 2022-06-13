package com.luisguadagnin.soccermanager.util;

import java.util.regex.Pattern;

public class EmailValidator {

    private static final String EMAIL_REGEX_PATTERN = "^(.+)@(\\S+)\\.(\\S+)$";

    public static boolean isValid(String emailAddress) {
        return Pattern.compile(EMAIL_REGEX_PATTERN)
                .matcher(emailAddress)
                .matches();
    }

}
