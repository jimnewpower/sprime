# Spring Boot 4 Migration Guide

**Project Migration:** Legacy Spring Framework 4.3.30 + JSF/PrimeFaces → Modern Spring Boot 4

**From:**
- Spring Framework 4.3.30 (`spring-core`, `spring-aspects`, `spring-jdbc`, `spring-webmvc`)
- Java 17
- Oracle JDBC 23.6
- Apache Tomcat 9.0.108
- Mojarra (`javax.faces`) 2.2.14
- PrimeFaces 13
- `web.xml` configuration
- Pure JSF/PrimeFaces with Spring backing beans only
- Manual `JdbcTemplate` + DataSource
- Maven build

**To:**
- Spring Boot 4.0.3 (latest stable as of March 2026)
- Java 25
- Embedded Tomcat 11 (Jakarta EE 11)
- JoinFaces 6.0.3 (auto-configures JSF + Spring)
- Mojarra 4.1 (Jakarta Faces)
- PrimeFaces 15.0.13 (Jakarta edition)
- Executable JAR + Docker container (your preference)
- Auto-configured HikariCP DataSource

---

## Phase 1: Update to Java 25

```xml
<!-- In pom.xml -->
<properties>
    <java.version>25</java.version>
    <maven.compiler.release>25</maven.compiler.release>
</properties>
```

Rebuild and run your tests.

---

## Phase 2: Jakarta EE Namespace Migration (`javax.*` → `jakarta.*`)

**Recommended (fastest):** Use **OpenRewrite** (one command).

Add this plugin temporarily to `pom.xml` and run:
```bash
mvn rewrite:run
```

**Manual fallback for `.xhtml` files:**
```xml
<!-- OLD -->
xmlns:h="http://xmlns.jcp.org/jsf/html"
xmlns:p="http://primefaces.org/ui"

<!-- NEW -->
xmlns:h="jakarta.faces.html"
xmlns:p="jakarta.faces.primefaces"
```

Also update `web.xml` and `faces-config.xml` schemas.

---

## Phase 3: Updated `pom.xml` (Replace your entire file)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>your.group</groupId>
    <artifactId>your-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.3</version>
    </parent>

    <properties>
        <java.version>25</java.version>
        <joinfaces.version>6.0.3</joinfaces.version>
    </properties>

    <dependencies>
        <!-- Spring Boot + embedded Tomcat 11 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JDBC + JdbcTemplate -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <!-- Oracle JDBC (latest compatible with Java 25 + Oracle 19c) -->
        <dependency>
            <groupId>com.oracle.database.jdbc</groupId>
            <artifactId>ojdbc11</artifactId>
            <version>23.9.0.25.07</version>
        </dependency>

        <!-- JoinFaces – auto-configures JSF, FacesServlet, Spring EL resolver, PrimeFaces -->
        <dependency>
            <groupId>org.joinfaces</groupId>
            <artifactId>jsf-spring-boot-starter</artifactId>
            <version>${joinfaces.version}</version>
        </dependency>

        <!-- PrimeFaces 15 (Jakarta flavor) -->
        <dependency>
            <groupId>org.primefaces</groupId>
            <artifactId>primefaces</artifactId>
            <version>15.0.13</version>
            <classifier>jakarta</classifier>
        </dependency>

        <!-- Tests -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Phase 4: Main Application Class

`src/main/java/your/package/YourApplication.java`
```java
package your.package;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

---

## Phase 5: `application.yml` (replaces manual DataSource + web.xml)

`src/main/resources/application.yml`
```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@//your-host:1521/your_service_name
    username: your_user
    password: your_password
    driver-class-name: oracle.jdbc.OracleDriver

  hikari:
    maximum-pool-size: 20

server:
  port: 8080

# JSF / PrimeFaces settings
jakarta.faces:
  PROJECT_STAGE: Production
  STATE_SAVING_METHOD: server
  DEFAULT_SUFFIX: .xhtml

primefaces:
  THEME: saga-blue   # change to your current theme
  UPLOADER: auto
```

---

## Phase 6: `faces-config.xml` (if you use one)

Update to Jakarta Faces 4.0:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<faces-config xmlns="https://jakarta.ee/xml/ns/jakartaee"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                                  https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_0.xsd"
              version="4.0">
    <!-- Your existing beans, navigation rules, etc. remain unchanged -->
</faces-config>
```

---

## Phase 7: Build & Run

```bash
mvn clean package
java -jar target/your-app-1.0.0.jar
```

Open: `http://localhost:8080/your-page.xhtml`

---

## Dockerfile (for Docker / container deployment)

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## What Was Removed / Simplified
- `web.xml`, manual `FacesServlet`, `SpringBeanFacesELResolver` → all gone (JoinFaces handles it)
- External Tomcat 9 → embedded Tomcat 11
- Manual DataSource → auto-configured HikariCP
- Old Spring XML/config → pure auto-configuration

Your `@Component` / `@Named` backing beans and `JdbcTemplate` code work unchanged.

---

## Common Pitfalls & Quick Fixes
- Faces not rendering → check `.xhtml` namespaces and `DEFAULT_SUFFIX`
- Oracle connection error → verify thin URL and `ojdbc11` version
- Missing styles → add `<p:resources />` or check theme
- Any error? Paste the stack trace here for an instant fix.

**You now have a modern, fast, secure Spring Boot 4 + Java 25 application!**
