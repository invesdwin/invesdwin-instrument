# invesdwin-instrument

This is an instrumentation java agent that is able to load itself into the running JVM. It is able to dynamically setup springs `InstrumentationLoadTimeWeaver` to enable support for aspects without having to start the JVM with an explicit java agent.

## Maven

Releases and snapshots are deployed to this maven repository:
```
http://invesdwin.de/artifactory/invesdwin-oss-remote
```

Dependency declaration:
```xml
<dependency>
	<groupId>de.invesdwin</groupId>
	<artifactId>invesdwin-instrument</artifactId>
	<version>1.0.4</version><!---version.invesdwin-instrument-->
</dependency>
```

## Usage

Simply define a static initializer like this:
```java
static {
  DynamicInstrumentationLoader.waitForInitialized(); //dynamically attach java agent to jvm if not already present
  DynamicInstrumentationLoader.initLoadTimeWeavingContext(); //weave all classes before they are loaded as beans
}
```
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
        SpringApplication.run(MySpringBootApplication.class, args); //start application, load some classes
    }
}
```
To enable the spring aspects, just add the [spring-aspects.jar](http://mvnrepository.com/artifact/org.springframework/spring-aspects) dependency to your project, they are enabled directly, others might need a [aop.xml](http://www.springbyexample.org/examples/aspectj-ltw-aspectj-config.html) to be enabled. Note that the "ctx.spring.weaving.xml" is part of the invesdwin-instrument jar.


Make sure that the instrumentation is loaded before the classes of the aspects or the classes that use the aspects are loaded by the classloader. Only classes that get loaded after initializing load time weaving will be successfully woven by aspectj.

Please note that you need to have a JDK installed with `tools.jar` available in it for this to work properly (this is not required anymore since Java 9).
Alternatively just load the invesdwin-instrument jar via the `-javaagent <path>` JVM parameter if you only have a JRE installed. The dynamic loading will be skipped then.

For a sample usage see the junit test cases in the [invesdwin-aspects](https://github.com/subes/invesdwin-aspects) project or the spring-boot example project in [invesdwin-nowicket](https://github.com/subes/invesdwin-nowicket).

## Support

If you need further assistance or have some ideas for improvements and don't want to create an issue here on github, feel free to start a discussion in our [invesdwin-platform](https://groups.google.com/forum/#!forum/invesdwin-platform) mailing list.
