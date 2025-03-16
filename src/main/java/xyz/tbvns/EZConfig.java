package xyz.tbvns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.reflections.Reflections;
import xyz.tbvns.Exeptions.NoEmptyConstructor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EZConfig {
    @Getter @Setter
    private static String configFolderName = "Config";
    @Getter
    private static final List<Class<? extends Config>> registeredClasses = new ArrayList<>();

    /**
     * @param classpath The path to the package containing the config classes.
     *                  E.G xyz.tbvns.config or xyz.tbvns.conf
     */
    public static void registerClassPath(String classpath) {
        registeredClasses.addAll(new Reflections(classpath).getSubTypesOf(Config.class));
    }

    public static void save() throws Exception {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .excludeFieldsWithModifiers()
                .setPrettyPrinting()
                .create();

        for (Class<? extends Config> registeredClass : registeredClasses) {
            File folder = new File(EZConfigUtils.getConfigFolder(registeredClass));
            if (!folder.exists()) {
                folder.mkdir();
            }

            File file = new File(EZConfigUtils.getConfig(registeredClass.getSimpleName(), registeredClass));
            if (!file.exists()) {
                file.createNewFile();
            }

            if (registeredClass.getDeclaredConstructor().isVarArgs()) {
                throw new NoEmptyConstructor("No empty constructor found on class " + registeredClass.getSimpleName());
            }

            FileUtils.writeStringToFile(
                    file,
                    gson.toJson(registeredClass.getDeclaredConstructor().newInstance()),
                    Charset.defaultCharset()
            );
        }
    }

    public static void createDefault(Class<? extends Config> c) throws Exception {
        // Get the configuration directory path
        File configFolder = new File(EZConfigUtils.getConfigFolder(c));

        // Create directories with proper existence check
        if (!configFolder.exists() && !configFolder.mkdirs()) {
            throw new IOException("Failed to create config directory: " + configFolder.getAbsolutePath());
        }

        // Initialize Gson instance
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .excludeFieldsWithModifiers()
                .setPrettyPrinting()
                .create();

        // Get the target config file
        File configFile = new File(EZConfigUtils.getConfig(c.getSimpleName(), c));

        // Create file only if it doesn't exist
        if (!configFile.exists()) {
            // Ensure parent directories exist (again, in case of race condition)
            File parentDir = configFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
            }

            // Create empty file
            if (!configFile.createNewFile()) {
                throw new IOException("Failed to create config file: " + configFile.getAbsolutePath());
            }
        }

        // Verify empty constructor exists
        Constructor<?> constructor;
        try {
            constructor = c.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new NoEmptyConstructor("No empty constructor found on class " + c.getSimpleName());
        }

        // Only write defaults if file is empty
        if (configFile.length() == 0) {
            // Create default config instance
            Config defaultConfig = (Config) constructor.newInstance();

            // Write to file with proper resource handling
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(defaultConfig, writer);
            }
        }
    }
    public static void load() throws Exception {
        for (Class<? extends Config> registeredClass : registeredClasses) {
            File file = new File(EZConfigUtils.getConfig(registeredClass.getSimpleName(), registeredClass));
            if (!file.exists()) {
                createDefault(registeredClass);
                continue;
            }

            EZConfigUtils.deserializeAndUpdateStaticFields(
                    FileUtils.readFileToString(file, StandardCharsets.UTF_8),
                    registeredClass
            );
        }
    }
}
