# invesdwin-instrument

This is an instrumentation java agent that is able to load itself into the running JVM. It is able to dynamically setup springs `InstrumentationLoadTimeWeaver` to enable support for aspects without having to start the JVM with an explicit java agent.

## Maven

Releases and snapshots are deployed to this maven repository:
```
https://invesdwin.de/repo/invesdwin-oss-remote/
```

Dependency declaration:
```xml
<dependency>
	<groupId>de.invesdwin</groupId>
	<artifactId>invesdwin-instrument</artifactId>
	<version>1.0.14</version><!---project.version.invesdwin-instrument-->
</dependency>
```

There is no plan to publish artifacts to maven central, see answer [here](https://github.com/invesdwin/invesdwin-instrument/issues/17#issuecomment-728772422) for alternatives.

## Usage

Simply define a static initializer like this:
```java
static {
  DynamicInstrumentationLoader.waitForInitialized(); //dynamically attach java agent to jvm if not already present
  DynamicInstrumentationLoader.initLoadTimeWeavingContext(); //weave all classes before they are loaded as beans
}
```


### Spring-Boot

With [spring-boot](http://projects.spring.io/spring-boot/) you have to ensure that the context in the aspects is updated by importing the one responsible for initializing the load time weaving into the spring-boot configuration:
```java
@SpringBootApplication
/** 
 * Make @Configurable work via @EnableLoadTimeWeaving.
 * If it does not work, alternatively you can try: 
 * @ImportResource(locations = "classpath:/META-INF/ctx.spring.weaving.xml") 
 */
@EnableLoadTimeWeaving
public class MySpringBootApplication {
    public static void main(final String[] args) {
        DynamicInstrumentationLoader.waitForInitialized(); //dynamically attach java agent to jvm if not already present
        DynamicInstrumentationLoader.initLoadTimeWeavingContext(); //weave all classes before they are loaded as beans
	//org.aspectj.weaver.loadtime.Agent.agentmain("", InstrumentationSavingAgent.getInstrumentation()); //workaround for spring-boot-devtools RestartLauncher
        SpringApplication.run(MySpringBootApplication.class, args); //start application, load some classes
    }
}
```
To enable the spring aspects, just add the [spring-aspects.jar](http://mvnrepository.com/artifact/org.springframework/spring-aspects) dependency to your project, they are enabled directly, others might need a [aop.xml](http://www.springbyexample.org/examples/aspectj-ltw-aspectj-config.html) to be enabled. Note that the "ctx.spring.weaving.xml" is part of the invesdwin-instrument jar.

Make sure that the instrumentation is loaded before the classes of the aspects or the classes that use the aspects are loaded by the classloader. Only classes that get loaded after initializing load time weaving will be successfully woven by aspectj.

Please note that you need to have a JDK installed with `tools.jar` available in it for this to work properly (this is not required anymore since Java 9).
Alternatively just load the invesdwin-instrument jar via the `-javaagent:/path/to/invesdwin-instrument.jar` JVM parameter if you only have a JRE installed. The dynamic loading will be skipped then.

For a sample usage see the junit test cases in the [invesdwin-aspects](https://github.com/subes/invesdwin-aspects) project or the spring-boot example project in [invesdwin-nowicket](https://github.com/subes/invesdwin-nowicket).

### Spring-Boot-Devtools

Note that [spring-boot-devtools](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools) uses a `org.springframework.boot.devtools.restart.RestartLauncher` that reinitializes the application in a nested classloader. This restart mechanism can be disabled via the system property `spring.devtools.restart.enabled=false`, but that might disable functionality of spring-boot-devtools. To work around this, use `org.aspectj.weaver.loadtime.Agent.agentmain("", InstrumentationSavingAgent.getInstrumentation());` inside the nested classloader to reinitialize AspectJ. Also you might have to add the AspectJ weaver option `-Xreweavable` to make it work properly. For more details see: https://github.com/invesdwin/invesdwin-instrument/issues/24

### Testing Aspects inside Unit Tests

See discussion [here](https://stackoverflow.com/a/70348250/67492). The workaround is to use the aspects inside nested classes that are loaded after invesdwin-instrument is initialized by the unit test class. Example [here](https://github.com/invesdwin/invesdwin-context-persistence/blob/master/invesdwin-context-persistence-parent/invesdwin-context-persistence-jpa-hibernate/src/test/java/de/invesdwin/context/persistence/jpa/hibernate/MultiplePersistenceUnitsTest.java).

### Burningwave Java Agent

With invesdwin-instrument-burningwave you can sometimes launch external applications on newer Java versions without having to add any add-opens commands. The java agent automatically opens all modules when being used via `StaticComponentContainer.Modules.exportAllToAll()` from [burningwave](https://www.burningwave.org/). This agent needs to be specified manually via `-javaagent:/path/to/invesdwin-instrument-burningwave.jar`. It does not load itself into the running process because you can just call burningwave yourself if you can modify your application anyway.

## Support

If you need further assistance or have some ideas for improvements and don't want to create an issue here on github, feel free to start a discussion in our [invesdwin-platform](https://groups.google.com/forum/#!forum/invesdwin-platform) mailing list.
