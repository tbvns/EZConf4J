package xyz.tbvns;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class EZConfigUtils {
    private static final String APP_NAME = "nsi-website";
    private static final String CONFIG_DIR_NAME = "Config";

    public static String getJarPath(Class<?> clazz) {
        try {
            String className = clazz.getName().replace('.', '/') + ".class";
            URL classUrl = clazz.getClassLoader().getResource(className);

            if (classUrl == null) {
                throw new IllegalStateException("Cannot find class resource");
            }

            String urlString = classUrl.toString();
            String decodedPath = "";

            if (urlString.startsWith("jar:nested:")) {
                // Spring Boot nested JAR handling
                int endIndex = urlString.indexOf("!/");
                String path = urlString.substring("jar:nested:".length(), endIndex);
                decodedPath = decodePath(path);
            } else if (urlString.startsWith("jar:file:")) {
                // Regular JAR file
                int endIndex = urlString.indexOf("!");
                decodedPath = decodePath(urlString.substring("jar:file:".length(), endIndex));
            } else if (urlString.startsWith("file:")) {
                // IDE execution
                int endIndex = urlString.length() - className.length() - "file:".length() - 2;
                decodedPath = decodePath(urlString.substring("file:".length(), endIndex));
            }

            return Paths.get(decodedPath).normalize().toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get JAR path", e);
        }
    }

    private static String decodePath(String encodedPath) throws Exception {
        String decoded = URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.name());

        // Fix Windows paths
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            if (decoded.startsWith("/") && decoded.length() > 2 && decoded.charAt(2) == ':') {
                return decoded.substring(1);
            }
        }
        return decoded;
    }

    public static String getConfigFolder(Class<?> clazz) {
        if (isRunningFromJar(clazz)) {
            return getSystemConfigDirectory();
        }
        return getDevelopmentConfigDirectory();
    }

    private static boolean isRunningFromJar(Class<?> clazz) {
        String className = clazz.getName().replace('.', '/') + ".class";
        URL resource = clazz.getClassLoader().getResource(className);
        return resource != null && resource.toString().startsWith("jar:");
    }

    private static String getSystemConfigDirectory() {
        String baseDir = System.getProperty("user.home");

        if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfigHome != null && !xdgConfigHome.isEmpty()) {
                baseDir = xdgConfigHome;
            } else {
                baseDir += "/.config";
            }
        } else if (System.getProperty("os.name").toLowerCase().contains("win")) {
            baseDir = System.getenv("APPDATA");
        }

        return Paths.get(baseDir, APP_NAME, CONFIG_DIR_NAME).toString();
    }

    private static String getDevelopmentConfigDirectory() {
        return Paths.get("").toAbsolutePath().toString()
                + File.separator + CONFIG_DIR_NAME;
    }

    public static String getConfig(String name, Class<?> clazz) {
        return Paths.get(getConfigFolder(clazz), name + ".json").toString();
    }

    public static <T> T deserializeAndUpdateStaticFields(String jsonContent, Class<T> clazz) throws Exception {
        JsonObject jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
        Gson gson = new Gson();
        T object = gson.fromJson(jsonContent, clazz);

        for (Field field : clazz.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                String fieldName = field.getName();

                if (jsonObject.has(fieldName)) {
                    JsonElement jsonElement = jsonObject.get(fieldName);
                    Object value = gson.fromJson(jsonElement, field.getType());
                    field.set(null, value);
                }
            }
        }
        return object;
    }
}