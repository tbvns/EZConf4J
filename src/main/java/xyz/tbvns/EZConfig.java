package xyz.tbvns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.reflections.Reflections;
import xyz.tbvns.Exeptions.NoEmptyConstructor;

import java.io.File;
import java.io.IOException;
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
        File folder = new File(EZConfigUtils.getConfigFolder(c));
        if (!folder.exists()) {
            folder.mkdir();
        }

        Gson gson = new GsonBuilder()
                .serializeNulls()
                .excludeFieldsWithModifiers()
                .setPrettyPrinting()
                .create();

        File file = new File(EZConfigUtils.getConfig(c.getSimpleName(), c));

        if (!file.exists()) {
            file.createNewFile();
        }

        if (c.getDeclaredConstructor().isVarArgs()) {
            throw new NoEmptyConstructor("No empty constructor found on class " + c.getSimpleName());
        }

        FileUtils.writeStringToFile(
                file,
                gson.toJson(c.getDeclaredConstructor().newInstance()),
                Charset.defaultCharset()
        );

    }

    public static void load() throws Exception {
        for (Class<? extends Config> registeredClass : registeredClasses) {
            File file = new File(EZConfigUtils.getConfig(registeredClass.getSimpleName(), registeredClass));
            System.out.println(file.getPath());
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
