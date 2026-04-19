package com.taxworkbench.application.shared;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public final class CursorCodec {

    private CursorCodec() {
    }

    public static String encode(List<String> parts) {
        String raw = String.join("\t", parts);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static List<String> decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return List.of();
        }
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        return List.of(raw.split("\t", -1));
    }
}
