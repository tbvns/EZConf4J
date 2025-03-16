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

public class EZConfigUtils {
    public static String getJarPath(Class<?> clazz) {
        try {
            String className = clazz.getName().replace('.', '/') + ".class";
            URL classUrl = clazz.getClassLoader().getResource(className);

            if (classUrl == null) {
                throw new IllegalStateException("Cannot find class resource");
            }

            String urlString = classUrl.toString();

            if (urlString.startsWith("jar:file:")) {
                // Extract actual file path from JAR URL
                int endIndex = urlString.indexOf("!");
                return new File(new URI(urlString.substring(4, endIndex))).getAbsolutePath();
            } else if (urlString.startsWith("file:")) {
                // Handle class files in directory
                int endIndex = urlString.length() - className.length() - 2;
                return new File(new URI(urlString.substring(0, endIndex))).getAbsolutePath();
            }

            throw new IllegalStateException("Unsupported class location: " + urlString);
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
