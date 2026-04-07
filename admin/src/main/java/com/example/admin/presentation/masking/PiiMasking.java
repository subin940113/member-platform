package com.example.admin.presentation.masking;

public final class PiiMasking {

    private PiiMasking() {}

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 1) {
            return "*" + domain;
        }
        return local.charAt(0) + "***" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String s = name.strip();
        if (s.length() <= 1) {
            return "*";
        }
        return s.charAt(0) + "**";
    }
}
