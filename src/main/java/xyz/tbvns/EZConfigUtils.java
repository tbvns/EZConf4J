package xyz.tbvns;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;

public class EZConfigUtils {
    public static String getJarPath(Class clazz) throws URISyntaxException {
        return new File(clazz.getProtectionDomain().getCodeSource().getLocation()
                .toURI()).getPath();
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
