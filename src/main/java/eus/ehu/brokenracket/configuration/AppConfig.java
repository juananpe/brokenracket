package eus.ehu.brokenracket.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final String CONFIG_FILE = "/config.properties"; // Path relative to resources folder
    private static AppConfig instance;
    private Properties properties;

    private AppConfig() {
        properties = new Properties();
        try (InputStream input = AppConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                System.err.println("Sorry, unable to find " + CONFIG_FILE);
                // Consider throwing a runtime exception or handling this more gracefully
                return;
            }
            properties.load(input);
            System.out.println("Configuration loaded from " + CONFIG_FILE);
        } catch (IOException ex) {
            System.err.println("Error loading configuration file: " + CONFIG_FILE);
            ex.printStackTrace();
            // Handle exceptions appropriately
        }
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            System.err.println("Configuration key not found: " + key + " in " + CONFIG_FILE);
            // Optionally throw an exception or return a default
        }
        return value;
    }

    // --- Getters for specific properties ---

    public String getLocale() {
        return getProperty("locale", "en"); // Default to 'en' if not found
    }

    public String getBusinessLogicNode() {
        return getProperty("businessLogic.node", "localhost");
    }

    public String getBusinessLogicPort() {
        return getProperty("businessLogic.port", "1099");
    }

    public String getBusinessLogicName() {
        return getProperty("businessLogic.name", "BusinessLogicService");
    }

    public boolean isBusinessLogicLocal() {
        return Boolean.parseBoolean(getProperty("businessLogic.isLocal", "true"));
    }

    public String getDataAccessNode() {
        return getProperty("dataAccess.node", "localhost");
    }

    public int getDataAccessPort() {
        // Requires careful handling if the property is missing or not an integer
        try {
            return Integer.parseInt(getProperty("dataAccess.port", "0")); // Provide a default or handle error
        } catch (NumberFormatException e) {
            System.err.println("Invalid format for dataAccess.port. Using default 0.");
            return 0;
        }
    }

    public boolean isDataAccessLocal() {
        return Boolean.parseBoolean(getProperty("dataAccess.isLocal", "true"));
    }

    public String getDataBaseFilename() {
        return getProperty("dataBase.filename");
    }

    public String getDataBaseOpenMode() {
        return getProperty("dataBase.openMode", "open"); // Default to 'open'
    }

    public String getDataBaseUser() {
        return getProperty("dataBase.user", "");
    }

    public String getDataBasePassword() {
        return getProperty("dataBase.password", "");
    }
} 