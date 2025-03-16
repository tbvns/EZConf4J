package xyz.tbvns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.reflections.Reflections;
import xyz.tbvns.Exeptions.NoEmptyConstructor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        // Get config directory path
        String configDirPath = EZConfigUtils.getConfigFolder(c);
        Path configDir = Paths.get(configDirPath);

        // Create config directory with proper permissions
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        // Create config file path
        Path configFile = Paths.get(EZConfigUtils.getConfig(c.getSimpleName(), c));

        // Only create file if it doesn't exist
        if (!Files.exists(configFile)) {
            // Create empty file with proper permissions
            Files.createFile(configFile);

            // Initialize default config
            Constructor<?> constructor = c.getDeclaredConstructor();
            Config defaultConfig = (Config) constructor.newInstance();

            // Write config with proper file locking
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                new GsonBuilder()
                        .setPrettyPrinting()
                        .create()
                        .toJson(defaultConfig, writer);
            }
        }
    }    public static void load() throws Exception {
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
