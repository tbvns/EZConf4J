package xyz.tbvns;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class EZConfigUtils {
    public static String getJarPath(Class<?> clazz) {
        try {
            String className = clazz.getName().replace('.', '/') + ".class";
            URL classUrl = clazz.getClassLoader().getResource(className);

            if (classUrl == null) {
                throw new IllegalStateException("Cannot find class resource");
            }

            String urlString = classUrl.toString();
            String decodedPath;

            if (urlString.startsWith("jar:nested:")) {
                int endIndex = urlString.indexOf("!/");
                String path = urlString.substring("jar:nested:".length(), endIndex);
                decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    if (decodedPath.startsWith("/") && decodedPath.length() > 2 && decodedPath.charAt(2) == ':') {
                        decodedPath = decodedPath.substring(1);
                    }
                }
            }
            else if (urlString.startsWith("jar:file:")) {
                int endIndex = urlString.indexOf("!");
                decodedPath = URLDecoder.decode(
                        urlString.substring("jar:file:".length(), endIndex),
                        StandardCharsets.UTF_8.name()
                );
            }
            // Handle IDE execution
            else if (urlString.startsWith("file:")) {
                int endIndex = urlString.length() - className.length() - 2;
                decodedPath = URLDecoder.decode(
                        urlString.substring("file:".length(), endIndex),
                        StandardCharsets.UTF_8.name()
                );
            }
            else {
                throw new IllegalStateException("Unsupported class location: " + urlString);
            }

            // Handle special characters and normalize path
            File jarFile = new File(decodedPath);
            return jarFile.getAbsolutePath();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get JAR path", e);
        }
    }

    public static String getJarParent(Class clazz) throws URISyntaxException {
        return new File(getJarPath(clazz)).getParent();
    }

    public static String getConfigFolder(Class clazz) throws URISyntaxException {
        return getJarParent(clazz) + File.separator + EZConfig.getConfigFolderName();
    }

    public static String getConfig(String name, Class clazz) throws URISyntaxException {
        return getConfigFolder(clazz) + File.separator + name + ".json";
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
