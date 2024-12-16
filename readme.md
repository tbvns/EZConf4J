# EZConfig4J
## What is it ?
It's the simplest way to create config file !
## Usage:
First create a new package. 
You are going to place all your config class inside the package.
For this example, we are going to use `xyz.tbvns.Example.configs`.

Then create a config class inside this new package:
```java
public class Conf implements Config {
    static String wowAString = "OMG a string";
    public static List<Integer> omgAnIntList = new ArrayList<>(){{
        add(1);
        add(2);
        add(3);
    }};
    public static HashMap<String, Integer> HelloWorld = new HashMap<>(){{
       put("Hello", 1);
       put("World", 2);
    }};
}
```
then register the class and load the config file:
```java
public class Test {
    public static void main(String[] args) throws Exception {
        //You should create a package with only configs
        EZConfig.registerClassPath("xyz.tbvns.Example.configs");
        
        EZConfig.load(); //Load the config files
     // EZConfig.save(); //Save the config files
        
        System.out.println(Conf.HelloWorld.get("Hello"));
    }
}
```
And that's it ! You have a fully working config file.

If you need you can change the config folder name using 
`EZConfig.setConfigFolderName("<Insert name here>");`

# Use it in your project:
### First, add JitPack to yor repo:
#### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
#### Gradle
```groovy
dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url 'https://jitpack.io' }
	}
}
```
___
### then add the dependency:
#### Maven
```xml
<dependency>
    <groupId>com.github.tbvns</groupId>
    <artifactId>EZConf4j</artifactId>
    <version>-SNAPSHOT</version>
</dependency>
```
#### Gradle
```groovy
dependencies {
        implementation 'com.github.tbvns:EZConf4j:-SNAPSHOT'
}
```

### Support me:
[![Ko-Fi](https://img.shields.io/badge/Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/tbvns)

