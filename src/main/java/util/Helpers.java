package util;

import java.util.Map;
import java.util.UUID;

public class Helpers {
    public static String generateToken() {
        return UUID.randomUUID()
            .toString()
            .replace("-", "")
            .toUpperCase();
    }

    public static Map<String, String> withTokenAndAction(String token, String action) {
        return Map.of("token", token, "action", action);
    }
}
