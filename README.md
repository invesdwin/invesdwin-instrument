# invesdwin-instrument

This is an instrumentation java agent that is able to load itself into the running JVM. It is able to dynamically setup springs `InstrumentationLoadTimeWeaver` to enable support for aspects without having to start the JVM with an explicit java agent.

## Maven

Releases and snapshots are deployed to this maven repository:
```
http://invesdwin.de:8081/artifactory/invesdwin-oss
```

Dependency declaration:
```xml
<dependency>
	<groupId>de.invesdwin</groupId>
	<artifactId>invesdwin-instrument</artifactId>
	<version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Usage

Simply define a static initializer like this:
```xml
static {
  DynamicInstrumentationLoader.waitForInitialized();
}
```
Make sure that the instrumentation is loaded before the classes of the aspects or the classes that use the aspects are loaded in the classloader, since only classes that have not been loaded yet will be successfully weaved by aspectj.

Please note that you need to have a JDK installed with `tools.jar` available in it for this to work properly.
Alternatively just load the invesdwin-instrument jar via the `-javaagent <path>` JVM parameter if you only have a JRE installed. The dynamic loading will be skipped then.

To load aspectj load time weaver, you can just instantiate a spring `GenericXmlApplicationContext` with the following xml:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns="http://www.springframework.org/schema/beans"
	xmlns:context="http://www.springframework.org/schema/context"
	xmlns:aop="http://www.springframework.org/schema/aop"
	xsi:schemaLocation="http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd
		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
		http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

	<context:load-time-weaver aspectj-weaving="on"/>

</beans>
```
