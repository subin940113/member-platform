package com.example.member.infrastructure.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class PhoneHasher {

    private final String pepper;

    public PhoneHasher(CryptoProperties properties) {
        this.pepper = properties.phonePepper();
    }

    public String normalize(String rawPhone) {
        return rawPhone == null ? "" : rawPhone.replaceAll("\\D", "");
    }

    public String hash(String rawPhone) {
        return sha256Hex((normalize(rawPhone) + pepper).getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(byte[] input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("해시 생성에 실패했습니다", e);
        }
    }
}
