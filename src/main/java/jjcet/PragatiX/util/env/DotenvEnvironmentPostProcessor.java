package jjcet.PragatiX.util.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads .env key-value pairs into Spring's Environment for local development
 * without requiring third-party libraries.
 * Real OS environment variables and JVM system properties always take precedence.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = findDotenvFile();
        if (envFile == null || !envFile.exists() || !envFile.canRead()) {
            return;
        }

        Map<String, Object> dotenvMap = parseDotenv(envFile);
        if (!dotenvMap.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, dotenvMap));
        }
    }

    private File findDotenvFile() {
        String userDir = System.getProperty("user.dir", ".");
        Path[] candidatePaths = new Path[]{
                Paths.get(userDir, ".env"),
                Paths.get(".env"),
                Paths.get("updating_decipline_backend", ".env"),
                Paths.get("..", ".env"),
                Paths.get(userDir, "..", ".env")
        };

        for (Path path : candidatePaths) {
            try {
                File f = path.toFile().getCanonicalFile();
                if (f.exists() && f.isFile()) {
                    return f;
                }
            } catch (IOException ignored) {
                // Ignore and try next path
            }
        }
        return null;
    }

    private Map<String, Object> parseDotenv(File envFile) {
        Map<String, Object> properties = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int eqIdx = line.indexOf('=');
                if (eqIdx <= 0) {
                    continue;
                }

                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1).trim();

                if (value.length() >= 2) {
                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                }

                if (!key.isEmpty()) {
                    properties.put(key, value);
                }
            }
        } catch (Exception e) {
            // Silently ignore parsing errors during bootstrap
        }
        return properties;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
