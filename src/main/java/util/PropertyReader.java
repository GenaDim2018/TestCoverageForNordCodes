package util;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = PropertyReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Файл config.properties не найден");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения config.properties", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static int getIntProperty(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}