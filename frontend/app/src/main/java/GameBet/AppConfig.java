package GameBet;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private final Properties props = new Properties();

    public AppConfig(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            props.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Could not load config file: " + filePath, e);
        }
    }

    public String getString(String key, String defaultValue) {
        String value = props.getProperty(key);
        return value != null ? value.trim() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}