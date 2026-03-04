# Comprehensive Migration Guide: Spring Framework 4.3 → Spring Boot 4

## Application Profile

| Component | Current | Target |
|---|---|---|
| Java | 17 | 25 (LTS, Sept 2025) |
| Spring | Framework 4.3.30 | Spring Boot 4.0.x (Framework 7.x) |
| Servlet Container | Tomcat 9.0.108 (standalone WAR) | Tomcat 11 (embedded or standalone) |
| JSF | org.glassfish javax.faces 2.2.14 | Jakarta Faces 4.1 |
| UI Components | PrimeFaces 13 | PrimeFaces 15.x (jakarta classifier) |
| Oracle JDBC | 23.6 | 23.x (ojdbc17) |
| Build | Maven | Maven |
| Config | XML-based | Java @Configuration + application.properties |
| Tests | JUnit 5 | JUnit 5 |

---

## Why This Isn't a Simple Version Bump

This migration crosses **three generational boundaries simultaneously**:

1. **Spring Framework 4.x → 7.x** — skipping the entire 5.x and 6.x lines
2. **Java EE (`javax.*`) → Jakarta EE 11 (`jakarta.*`)** — every servlet, faces, inject, and annotation import must change
3. **Traditional Spring XML config + standalone WAR → Spring Boot auto-configuration**

The Spring Boot team strongly recommends upgrading to Spring Boot 3.5 before migrating to Spring Boot 4.0. This guide follows that advice with a three-phase approach.

---

## Phase 1: Spring Boot 3.5 on Java 17 (The Bridge Release)

### 1.1 Restructure the Maven POM

Replace your individual Spring module dependencies with Boot starters:

**Before** (your current approach):
```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>4.3.30.RELEASE</version>
</dependency>
<!-- spring-webmvc, spring-jdbc, spring-aspects similarly -->
```

**After**:
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.0</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc17</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.joinfaces</groupId>
        <artifactId>faces-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.joinfaces</groupId>
        <artifactId>primefaces-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.joinfaces</groupId>
            <artifactId>joinfaces-dependencies</artifactId>
            <version>5.4.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Do **not** specify versions for dependencies managed by the Boot BOM.

### 1.2 The javax → jakarta Namespace Migration

This is the single largest mechanical change. The `javax.*` packages are completely abandoned and replaced by `jakarta.*` packages.

**Affected packages across your codebase**:

| Old | New |
|---|---|
| `javax.servlet.*` | `jakarta.servlet.*` |
| `javax.faces.*` | `jakarta.faces.*` |
| `javax.inject.*` | `jakarta.inject.*` |
| `javax.annotation.*` | `jakarta.annotation.*` |
| `javax.el.*` | `jakarta.el.*` |

**Recommended tooling**: Use OpenRewrite to automate this. Add to your POM temporarily:

```xml
<plugin>
    <groupId>org.openrewrite.maven</groupId>
    <artifactId>rewrite-maven-plugin</artifactId>
    <version>5.45.0</version>
    <configuration>
        <activeRecipes>
            <recipe>org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta</recipe>
        </activeRecipes>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>org.openrewrite.recipe</groupId>
            <artifactId>rewrite-migrate-java</artifactId>
            <version>2.28.0</version>
        </dependency>
    </dependencies>
</plugin>
```

Run `mvn rewrite:run`, then verify with: `grep -rn "javax\." --include="*.java" --include="*.xml" --include="*.xhtml" src/`

**Critical for your 80+ XHTML files** — namespace URIs must change:

| Old Namespace URI | New |
|---|---|
| `http://xmlns.jcp.org/jsf/html` | `jakarta.faces.html` |
| `http://xmlns.jcp.org/jsf/core` | `jakarta.faces.core` |
| `http://xmlns.jcp.org/jsf/facelets` | `jakarta.faces.facelets` |
| `http://xmlns.jcp.org/jsf/passthrough` | `jakarta.faces.passthrough` |

A batch `sed` command can handle all 80+ files at once, but verify rendering afterward.

### 1.3 JSF Integration via JoinFaces

Your current app runs JSF on standalone Tomcat with `web.xml` config. Spring Boot doesn't natively support JSF, but JoinFaces enables JSF usage inside Spring Boot Applications and auto-configures Mojarra/MyFaces, PrimeFaces, and the servlet container.

**What JoinFaces replaces**:
- FacesServlet registration (automatic)
- JSF init parameters (`web.xml` context-params → `application.properties`)
- Spring-JSF bean bridging (via `SpringBeanFacesELResolver`, auto-configured)
- JSF scope annotations mapped to Spring scopes

**Migrating `faces-config.xml` managed beans** — convert to Spring-managed beans:

```java
// Before: <managed-bean> in faces-config.xml
// After:
import jakarta.inject.Named;
import org.springframework.web.context.annotation.SessionScope;

@Named("loginBean")
@SessionScope
public class LoginBean { /* ... */ }
```

If a bean was defined as request-scoped, its class should be annotated with the corresponding Spring `@RequestScope`. For view-scoped beans, JoinFaces maps Jakarta's `@ViewScoped` to a Spring scope.

**Replace web.xml context-params with** `application.properties`:
```properties
joinfaces.faces.project-stage=Production
joinfaces.faces.facelets-suffix=.xhtml
joinfaces.primefaces.theme=saga
joinfaces.primefaces.font-awesome=true
joinfaces.faces-servlet.url-mappings=*.xhtml
```

### 1.4 Convert XML Spring Config to Java @Configuration

Don't convert everything at once. Use `@ImportResource` as a bridge:

```java
@SpringBootApplication
@ImportResource("classpath:applicationContext.xml")
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

Then iteratively extract beans into `@Configuration` classes. **Spring Boot auto-configures most of what your XML likely declares**:

| XML Configuration | Boot Equivalent |
|---|---|
| `<context:component-scan>` | `@SpringBootApplication` includes it |
| `<context:property-placeholder>` | `application.properties` auto-loaded |
| `<bean id="dataSource">` | Auto-configured from `spring.datasource.*` |
| `<bean id="jdbcTemplate">` | Auto-configured with starter-jdbc |
| `<tx:annotation-driven/>` | Auto-configured; just use `@Transactional` |
| `<aop:aspectj-autoproxy/>` | Auto-configured by starter-aop |
| `<mvc:annotation-driven/>` | Auto-configured by starter-web |

**DataSource in** `application.properties`:
```properties
spring.datasource.url=jdbc:oracle:thin:@//dbhost:1521/MYSERVICE
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
```

### 1.5 Migrate Custom Servlet Filters and Listeners

Register your `web.xml` filters and listeners as Spring beans:

```java
@Configuration
public class WebConfig {
    @Bean
    public FilterRegistrationBean<MyCustomFilter> myFilter() {
        FilterRegistrationBean<MyCustomFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new MyCustomFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public ServletListenerRegistrationBean<MyContextListener> myListener() {
        return new ServletListenerRegistrationBean<>(new MyContextListener());
    }
}
```

The filter/listener classes themselves need `javax.servlet` → `jakarta.servlet` import updates.

### 1.6 Oracle JDBC

Your Oracle JDBC 23.6 driver is already modern. Use `ojdbc17` for Java 17+:

```xml
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc17</artifactId>
    <version>23.6.0.24.10</version>
    <scope>runtime</scope>
</dependency>
```

Spring Boot defaults to HikariCP for connection pooling, which works well with Oracle. If you need Oracle UCP, add the `ucp` dependency and set `spring.datasource.type=oracle.ucp.jdbc.PoolDataSource`.

### 1.7 JUnit 5 Tests

Your JUnit 5 tests mainly need annotation changes — replace `@ContextConfiguration(locations = "classpath:applicationContext.xml")` with `@SpringBootTest`, and ensure any lingering `@RunWith(SpringRunner.class)` becomes `@ExtendWith(SpringExtension.class)` or is simply removed (since `@SpringBootTest` includes it).

### Phase 1 Checkpoint

At this point: Spring Boot 3.5 on Java 17, all `javax.*` → `jakarta.*`, JSF/PrimeFaces running via JoinFaces, XML config loading via `@ImportResource` with progressive conversion, tests green. **Run full regression across all 80+ JSF pages before proceeding.**

---

## Phase 2: Spring Boot 4.0 on Java 17

### 2.1 Update the Parent POM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.3</version>
</parent>
```

### 2.2 Boot 4 Modularization

Spring Boot 4 features complete modularization of the codebase providing smaller and more focused jars. For applications using starter POMs, this is mostly transparent. However, if you directly depended on `spring-boot-autoconfigure`, remove it — it's no longer a public dependency. Classes, methods and properties that were deprecated in Spring Boot 3.x have been removed in this release.

### 2.3 Spring Framework 7 Breaking Changes

- **Removed deprecated APIs**: Anything deprecated in Spring 5.x or 6.x is gone.
- **Configuration properties binding**: Spring Boot 4 removes binding to public fields. Use private fields with getters/setters in `@ConfigurationProperties` classes.
- **Virtual threads**: Enabled by default for web request handling and `@Async`. If you have custom `TaskExecutor` beans, update them.
- **JSpecify null safety**: Informational; your IDE may surface new null-safety warnings.

### 2.4 Jakarta EE 11 Alignment

Spring Boot 4 is based on Jakarta EE 11 and requires a Servlet 6.1 baseline. The `jakarta.*` namespace doesn't change again, but APIs have been refined. Jakarta Faces 4.1 removes remaining SecurityManager references, improves CDI alignment, adds UUIDConverter and flow injection support.

### 2.5 Jackson 3

Spring Boot 4 upgrades to Jackson 3 with changes to default serialization behavior. Since your app is JSF-based (server-side rendering), impact should be minimal — but test any REST/AJAX endpoints.

### 2.6 JoinFaces for Boot 4

JoinFaces 6.0.x targets Spring Boot 4.0 with Jakarta Faces 4.0–4.1. Update your BOM to JoinFaces 6.0.x.

### 2.7 Tomcat 11

Spring Boot 4 bundles Tomcat 11 automatically. Spring Boot does not recommend deploying to a non-Servlet 6.1 compliant container. If continuing WAR deployment, use standalone Tomcat 11 and extend `SpringBootServletInitializer`:

```java
@SpringBootApplication
public class MyApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MyApplication.class);
    }
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### Phase 2 Checkpoint

Spring Boot 4.0.x on Java 17, JoinFaces 6.x, Tomcat 11, all tests green.

---

## Phase 3: Java 25 (The Runtime Upgrade)

### 3.1 Why Java 25

Java 25 is the latest LTS release, delivering 18 JEPs including performance improvements and new language features. Spring Boot 4 provides first class support for Java 25 whilst retaining Java 17 compatibility.

### 3.2 Update Maven

```xml
<properties>
    <java.version>25</java.version>
</properties>
```

### 3.3 What Typically Breaks

- **Internal APIs**: `sun.*` or `com.sun.*` usage may be blocked. Check with `jdeps --jdk-internals myapp.jar`.
- **Reflection access**: May need `--add-opens` flags for older libraries.
- **32-bit removed**: Java 25 is 64-bit only.

Your `ojdbc17` driver works on Java 25 without changes.

### 3.4 New Language Features (Optional)

Java 25 brings records, sealed classes, pattern matching in switch, compact source files, stable virtual threads, and scoped values. These are refactoring opportunities, not requirements.

---

## PrimeFaces 13 → 15 Migration

**Dependency** (with JoinFaces managing it, or manually):
```xml
<dependency>
    <groupId>org.primefaces</groupId>
    <artifactId>primefaces</artifactId>
    <version>15.0.11</version>
    <classifier>jakarta</classifier>
</dependency>
```

**Key changes across your 80+ pages**:

- **Namespace**: Update `xmlns:p` from old URI to `xmlns:p="primefaces"` (short URI)
- **Themes**: PrimeFaces 15 uses PrimeOne design system — default themes are `saga`, `arya`, `vela`
- **LazyDataModel**: API changes in `load()` method signatures — audit all custom `LazyDataModel` implementations
- **Removed components**: Check PrimeFaces changelogs for each major version (13→14→15)
- **jQuery**: PrimeFaces bundles its own jQuery — test any custom JS interactions

**Strategy for 80+ pages**: Use automated find-and-replace for namespace changes, then create a smoke test that navigates to every page checking for JSF error messages. Focus manual QA on pages using `p:dataTable`, `p:dialog`, `p:fileUpload`, and `p:datePicker`.

---

## Recommended Timeline

| Phase | Duration | Key Risk |
|---|---|---|
| **Phase 1**: Boot 3.5 + Java 17 | 6–10 weeks | javax→jakarta across 80+ XHTMLs; XML config conversion; JoinFaces integration |
| **Phase 2**: Boot 4.0 + Java 17 | 2–4 weeks | Framework 7 removed APIs; Jackson 3; JoinFaces 6.x |
| **Phase 3**: Java 25 | 1–2 weeks | Third-party library compatibility |
| **Stabilization** | 2–4 weeks | JSF rendering edge cases; PrimeFaces behavior |
| **Total** | **~11–20 weeks** | — |

---

## Tooling Recommendations

1. **OpenRewrite** — composite recipes for Boot 3.5 and Boot 4.0 migrations that chain hundreds of individual transformations
2. **`jdeprscan --release 25 myapp.jar`** — scan for deprecated JDK API usage
3. **`jdeps --jdk-internals myapp.jar`** — find internal API dependencies
4. **IntelliJ IDEA** — built-in Jakarta migration and Java 25 support
5. **Maven dependency analysis**: `mvn dependency:tree | grep javax` to find all remaining javax dependencies

---

## Version Matrix Summary

| Component | Phase 1 | Phase 2 | Phase 3 (Final) |
|---|---|---|---|
| Java | 17 | 17 | **25** |
| Spring Boot | 3.5.x | **4.0.x** | 4.0.x |
| Spring Framework | 6.2.x | **7.x** | 7.x |
| Jakarta EE | 10 | **11** | 11 |
| Tomcat | 10.1.x | **11.x** | 11.x |
| Jakarta Faces | 4.0 | **4.1** | 4.1 |
| PrimeFaces | 15.x (jakarta) | 15.x | 15.x |
| JoinFaces | 5.4+/5.5.x | **6.0.x** | 6.0.x |
| Oracle JDBC | ojdbc17 23.x | 23.x | 23.x |

---

Want me to generate this as a downloadable Word document, or dive deeper into any specific phase? I can also create a sample `pom.xml` for any of the three phases, or a more detailed checklist for the XHTML namespace migration across your 80+ pages.