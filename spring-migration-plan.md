# **ErPlus Application Migration Plan**

```
Spring 4.3 / JSF 2.2 / Java 17 / Tomcat 9
to:
Spring 6.2 / Jakarta Faces 4.0 / Java 21 / Tomcat 10.1

Target: Java 21  |  Jakarta EE 10  |  Incremental Migration
```

Prepared: March 2026

# **Executive Summary**

This document provides a phased migration plan for upgrading the ErPlus enterprise Java web application:

* from Spring Framework 4.3 / JSF 2.2 / Java 17 / Tomcat 9   
* to Spring Framework 6.2 / Jakarta Faces 4.0 / Java 21 / Tomcat 10.1

targeting the Jakarta EE 10 (jakarta namespace) platform.

## **Why This Migration Is Necessary**

**Security exposure is the primary driver.** Spring 4.x no longer receives security patches. When CVEs are discovered in the framework (and they continue to be Spring4Shell in 2022 being the most prominent example), this version will never be patched. The same applies to Hibernate Validator 5.0.1, javax.faces 2.2.14, and OmniFaces 1.14 all well past their support windows.

**Java ecosystem compatibility is narrowing.** Spring 4.x targets Java 8. Modern JDK releases (17, 21 LTS) offer substantial performance improvements ZGC/Shenandoah garbage collectors, virtual threads, compact strings, improved JIT compilation — none of which the current stack can reliably leverage. As Oracle and the broader ecosystem drop older JDK support, the deployment surface shrinks. The good news: we have already migrated from Java 8 to 17.

**Dependency isolation is eroding.** MyBatis-Spring 1.3.3 predates Spring 5 compatibility work. Tomcat JDBC 9.x is already ahead of Spring 4.x's expected Servlet API level. javax.servlet 3.1.0 prohibits Servlet 4.0+ features. These version mismatches create subtle integration bugs that are difficult to diagnose and will worsen as any single dependency is updated.

## **Phased Approach**

The migration is structured into 4 phases (with a Phase 0 preparation step), designed to minimize risk at each step. Since the application is already on Java 17 (the minimum required by Spring 6), the Java upgrade to 21 can happen at any convenient point and is not a blocking prerequisite. The critical constraint is the javax-to-jakarta namespace change, which requires a coordinated cut-over and cannot be done incrementally across those boundaries.

| Key Constraint: The javax → jakarta Wall The namespace change from javax.\* to jakarta.\* touches all layers simultaneously (Servlet API, JSF, CDI, EL, Bean Validation). It is not possible to run half the app on javax and the other half on jakarta. The strategy is: incremental preparation → Spring 5.3 (last javax) → coordinated namespace cut-over → stabilization. |
| :---- |

| OmniFaces Assessment: Low Risk OmniFaces usage is limited to Faces.redirect(), Ajax.oncomplete() (handful of Java calls), and 61 \<o:importConstants\> tags in XHTML. All three are stable across OmniFaces versions 1.x through 4.x. The migration is a dependency coordinate swap with no code changes required. The XHTML namespace (xmlns:o="http://omnifaces.org/ui") is still supported in OmniFaces 4.x. |
| :---- |

# **Current vs Target Dependency Matrix**

The table below maps every major dependency from its current version to its target version. All target versions have been verified as current stable releases.

| Dependency | Current | Target | Phase |
| :---- | :---- | :---- | :---- |
| **Java** | 17 | 21 (LTS) | Phase 2 or anytime |
| **Spring Framework** | 4.3.30.RELEASE | 6.2.x (latest) | Phase 1 → 2 |
| **Tomcat (server)** | 9.x | 10.1.x (10.1.52+) | Phase 2 |
| **Tomcat JDBC pool** | 9.0.108 | 10.1.x | Phase 2 |
| **JSF (Mojarra)** | javax.faces 2.2.14 | Jakarta Faces 4.0 (Mojarra 4.0.x) | Phase 2 |
| **PrimeFaces** | 13.0.10 | 15.0.x (jakarta classifier) | Phase 2 |
| **OmniFaces** | 1.14 | 4.7.2 | Phase 2 |
| **Servlet API** | javax.servlet 3.1 | jakarta.servlet 6.0 | Phase 2 |
| **EL API** | javax.el 3.0.0 | jakarta.el 5.0 | Phase 2 |
| **CDI API** | cdi-api 2.0 | jakarta.cdi 4.0 | Phase 2 |
| **MyBatis-Spring** | 1.3.3 | 3.0.x | Phase 1 → 2 |
| **Hibernate Validator** | 5.0.1.Final | 8.0.x | Phase 2 |
| **Oracle JDBC** | ojdbc11 23.6 | ojdbc11 23.x (latest) | Phase 0/1 |
| **JSTL** | javax.servlet:jstl 1.2 | jakarta.servlet.jsp.jstl 3.0 | Phase 2 |
| **Maven** | 3.9.x | 3.9.x (no change) | N/A |

| Note on Spring 7.0 / Spring Boot 4 Spring 7.0 was released November 2025 targeting Jakarta EE 11\. While tempting, Spring 6.2.x remains the safer target: it has longer open-source support (through June 2026), broader library ecosystem compatibility, and doesn’t require Tomcat 11\. We can upgrade from 6.2 to 7.0 later as a smaller incremental step. |
| :---- |

| Note on Java Version Since the application is already on Java 17, there is no Java upgrade prerequisite blocking the Spring 6 migration (Spring 6 requires Java 17 minimum). The upgrade to Java 21 is recommended for LTS support and features like virtual threads, but can happen at any convenient point — before, during, or after the Spring/Jakarta migration. |
| :---- |

# **Phased Migration Plan**

## **Phase 0: Preparation and Test Infrastructure**

**Estimated effort: 2-4 weeks. This phase makes no production code changes but is critical for risk reduction.**

*Objective: Build a safety net before touching any dependencies. With moderate test coverage, this is the single highest-ROI investment in the entire migration.*

**0.1 Audit and catalog all dependencies.** Run 'mvn dependency:tree' and catalog every transitive dependency. Flag any that are known to be javax-only or abandoned.

**0.2 Expand integration test coverage for critical paths.** Focus on the most critical JSF flows (core business workflows, data entry forms). Write Selenium or Arquillian tests that exercise the full request cycle. These tests will be the regression safety net during Phases 1–3.

**0.3 Establish a baseline build and deployment pipeline.** Ensure the CI/CD pipeline can build the project, run all tests, and deploy to a staging Tomcat 9 instance reproducibly. 

**0.4 Create a migration branch and version control strategy.** Create a long-lived migration branch. Each phase should be a merge-back point. Never do the entire migration on a single unmerged branch.

**0.5 Document all custom JSF components, phase listeners, and converters.** These are the elements most likely to break during the JSF 2.2 → 4.0 transition. Create an inventory with the specific javax.faces APIs each one uses.

**0.6 Inventory OmniFaces usage.** Confirmed: 61 \<o:importConstants\> tags in XHTML, plus a handful of Faces.redirect() and Ajax.oncomplete() Java calls. No other o: namespace tags. All of these are stable across OmniFaces versions and require no code changes — only a dependency coordinate swap in Phase 2\.

**0.7 Inventory PrimeFaces component usage.** PrimeFaces 14 changed SearchExpression handling and 15 removed InputSwitch and old chart components (\<p:barChart\>, \<p:pieChart\>, etc.) deprecated since PF 10\. Grep XHTML files for these components now to understand the scope of PrimeFaces-specific changes.

| Exit Criteria for Phase 0 All existing tests pass. CI/CD pipeline is reliable. Dependency inventory is complete. Critical JSF flows have integration test coverage. PrimeFaces and OmniFaces usage inventories are documented. Team is aligned on the migration branch strategy. |
| :---- |

## **Phase 1: Spring 4.3 - Spring 5.3 (Java 17, Tomcat 9 still javax)**

**Estimated effort: 2-4 weeks. Moderate risk. This is the largest behavioral change within the javax world.**

*Objective: Upgrade Spring Framework to 5.3.x (the last javax-based Spring release) and update MyBatis-Spring to its javax-compatible latest. This resolves the majority of Spring API deprecations and breaking changes before the namespace wall. Since it is already on Java 17, there is no JDK upgrade needed for this phase.*

**1.0 Mitigate properties sprawl:** 
- remove AppSettings.properties from `ERMobile/env`. 
- Identify unused properties and remove them from ETER_Services AppSettings.properties.
- Bypass `Constants.getProperties()`, `Constants.getEnvironment()`, etc. 
- Use standard Spring property accessors.
- Remove dependencies upon `snl.app.Application` to obtain properties.

**1.2 Collapse 4-Project Structure** into a single multi-module Maven project. This is not strictly required for the migration, but it simplifies dependency management and makes the OpenRewrite recipes more effective. The current structure with separate ETER_Services, ERMobile, ERPlus, and EventTracker projects creates complexity in managing dependencies and applying consistent upgrades. A single multi-module project with clear module boundaries (e.g., core, web, mobile) will streamline the migration process.

Single-project structure:  
```
erplus  
├───src  
│   ├───main  
│   │   ├───java  
│   │   │   └───gov  
│   │   │       └───snl  
│   │   │           └───app  
│   │   │               ├───er
│   │   │               ├───actions  
│   │   │               ├───api  
│   │   │               ├───bean  
│   │   │               ├───controller 
│   │   │               ├───converter
│   │   │               ├───dao
│   │   │               ├───exception  
│   │   │               ├───filter
│   │   │               └───mapper
│   │   │               └───...
│   │   ├───resources  
│   │   │   ├───static  
│   │   │   │   └───error  
│   │   │   └───templates  
│   │   └───webapp  
│   │       ├───access  
│   │       ├───admin  
│   │       ├───approve  
│   │       ├───...
│   │       ├───resources  
│   │       │   └───images  
│   │       └───WEB-INF  
│   │       └───index.xhtml  
│   └───test  
│       └───java  
│           └───gov  
│               └───snl  
│                   └───app  
├─ pom.xml
├─ readme.md
```

**1.3 Upgrade Spring Framework from 4.3.30 to 5.3.x (latest 5.3 patch).** Key breaking changes in Spring 5.0+: removal of several deprecated classes in spring-webmvc, changes to default content negotiation, updated Jackson/databind requirements. The [Spring 4 → 5 migration guide](https://docs.spring.io/spring-integration/reference/changes-4.3-5.0.html) should be reviewed carefully.

**1.4 Upgrade MyBatis-Spring from 1.3.3 to 2.1.x.** MyBatis-Spring 2.1.x is the last version compatible with Spring 5\. This is a significant jump (1.3 → 2.1) that may require changes to SqlSessionFactoryBean configuration. Review the MyBatis-Spring migration notes.

**1.5 Address deprecation warnings.** Spring 5.3 deprecated many APIs that were removed in 6.0. Fix these now while still on the javax stack so the Phase 2 cut-over is cleaner. Key areas: WebMvcConfigurerAdapter → WebMvcConfigurer, SimpleClientHttpRequestFactory changes, and any removed XML schema namespaces.

**1.6 Run OpenRewrite for automated fixes (optional but recommended).** Use the org.openrewrite.java.spring.framework.UpgradeSpringFramework\_5\_3 recipe to catch mechanical changes automatically.

**1.7 Full regression testing on Tomcat 9 with Java 17\.** Deploy and test thoroughly. Every JSF page, every MyBatis query. This is the final javax checkpoint.

| Critical Warning: Spring 4 → 5 Breaking Changes Spring 5 dropped support for several things silently present in 4.x: Tiles integration, Velocity templates, XMLBeanFactory, and various deprecated web utilities. If the JSF integration layer used any Spring 4-specific web utilities, they may need replacement. Check spring-webmvc carefully. |
| :---- |

## **Phase 2: The Big Cut-Over javax to jakarta \+ Spring 6.2 \+ Tomcat 10.1**

**Estimated effort: 4-8 weeks. HIGH RISK. This is the coordinated namespace migration. All Jakarta EE dependencies change simultaneously.**

*Objective: Migrate the entire application from the javax namespace to the jakarta namespace. This requires simultaneous upgrades of Spring, Tomcat, JSF/PrimeFaces, Servlet API, EL, CDI, Bean Validation, OmniFaces, and all related libraries. This cannot be done incrementally.*

| Why this must be a single coordinated phase The javax jakarta rename is a binary-incompatible change. A class compiled against javax.servlet.http.HttpServletRequest cannot be passed to a method expecting jakarta.servlet.http.HttpServletRequest. All libraries in the dependency tree must agree on which namespace they use. There is no gradual migration path across this boundary. |
| :---- |

**2.1 Run OpenRewrite to automate the mechanical migration.** Use these recipes in sequence: (a) org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta for the namespace change, (b) org.openrewrite.java.spring.framework.UpgradeSpringFramework\_6\_0 for Spring-specific changes, (c) org.openrewrite.java.migrate.jakarta.JakartaEE10 for Jakarta EE 10 API updates. Run in dry-run mode first to review changes.

**2.2 Update all Maven dependencies simultaneously. Key changes:**

Spring Framework: 5.3.x - 6.2.x (latest stable)

Servlet API: javax.servlet:javax.servlet-api:3.1 - jakarta.servlet:jakarta.servlet-api:6.0.0 (scope: provided)

JSF: org.glassfish:javax.faces:2.2.14 - org.glassfish:jakarta.faces:4.0.x (Mojarra 4.0 impl)

EL: javax.el:javax.el-api:3.0.0 - jakarta.el:jakarta.el-api:5.0.x

CDI: javax.enterprise:cdi-api:2.0 - jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.x

Bean Validation: org.hibernate:hibernate-validator:5.0.1.Final - org.hibernate.validator:hibernate-validator:8.0.x (note: groupId changed)

PrimeFaces: 13.0.10 - 15.0.x with \<classifier\>jakarta\</classifier\>

OmniFaces: 1.14 - 4.7.2 (coordinate swap only; Faces.redirect(), Ajax.oncomplete(), and \<o:importConstants\> all unchanged)

MyBatis-Spring: 2.1.x - 3.0.x (Spring 6 compatible)

JSTL: javax.servlet:jstl:1.2 - jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.x

Tomcat JDBC: 9.0.x - 10.1.x

**2.3 Upgrade Java to 21 (recommended to do in this phase).** Because already on Java 17, Spring 6.2 will compile and run without a JDK change. However, this is the natural point to upgrade to Java 21 for LTS alignment and virtual thread support with Tomcat 10.1. Update maven-compiler-plugin source/target to 21 and verify with the full test suite.

**2.4 Migrate XML Configuration to Java Configuration.** 

Inventory XML Configuration Files

* `applicationContext.xml` (root context: datasource, MyBatis, transactions, services)  
* `dispatcher-servlet.xml` or `spring-mvc.xml` (web context: view resolvers, interceptors)  
* `web.xml` (servlet/filter/listener declarations)  
* Possibly `faces-config.xml` (JSF navigation, managed beans - this is JSF-specific, not Spring XML)

**Do not touch `faces-config.xml` in this step.** That's JSF configuration, not Spring configuration. It follows different rules.

**Critical nuance for this stack:** Using JSF/PrimeFaces alongside Spring MVC, causes a dual-servlet situation. `FacesServlet` needs to be registered too. `AbstractAnnotationConfigDispatcherServletInitializer` only handles `DispatcherServlet`. Need to define a separate `ServletContainerInitializer` or a `ServletContextListener` for JSF:

Mojarra (current `javax.faces` implementation at 2.2.14) already ships its own `ServletContainerInitializer` (`com.sun.faces.config.FacesInitializer`). In most Tomcat deployments, JSF self-registers if the JAR is on the classpath. So we may not need to manually register `FacesServlet` at all. Test this before writing boilerplate.

**Alternatively**, use the hybrid approach: keep a minimal `web.xml` just for JSF-specific entries and do Spring config in Java. This is pragmatic and avoids fighting JSF's initialization expectations. There's no award for zero XML.

**2.5 Migrate JSF/Facelets XHTML files.** This is the most labor-intensive manual step. All XHTML namespace declarations must change:

xmlns:h="http://xmlns.jcp.org/jsf/html" → xmlns:h="jakarta.faces.html"

xmlns:f="http://xmlns.jcp.org/jsf/core" → xmlns:f="jakarta.faces.core"

xmlns:ui="http://xmlns.jcp.org/jsf/facelets" → xmlns:ui="jakarta.faces.facelets"

xmlns:p="http://primefaces.org/ui" remains unchanged (PrimeFaces namespace did not change)

xmlns:o="http://omnifaces.org/ui" remains unchanged (OmniFaces 4.x still supports this URI)

Use sed/grep for bulk replacement, then manually verify each page.

**2.6 Update faces-config.xml and web.xml.** The schema namespace in faces-config.xml changes to jakarta.ee. The web.xml schema namespace changes similarly. Any javax.faces.\* context parameters must be renamed to jakarta.faces.\* equivalents.

**2.7 Fix custom JSF components, converters, validators, and phase listeners.** Every class that imports from javax.faces.\* must change to jakarta.faces.\*. OpenRewrite handles Java imports, but check for string literals referencing javax.faces in annotations or configuration.

**2.8 Handle PrimeFaces 13 → 15 breaking changes.** PrimeFaces 14 dropped JSF 2.2 compatibility and changed SearchExpression handling to delegate to core Faces. PrimeFaces 15 removed InputSwitch and old chart components (\<p:barChart\>, \<p:pieChart\>, etc.) deprecated since PF 10\. Review the PrimeFaces 14.0.0 and 15.0.0 migration guides on GitHub.

**2.9 Deploy to Tomcat 10.1.x.** Switch deployment target from Tomcat 9 to Tomcat 10.1. Tomcat 10.1 provides Servlet 6.0, which is required by Spring 6.2 and Jakarta Faces 4.0.

**2.10 Full regression testing.** This is the most critical testing phase. Test every JSF page, every form submission, every AJAX interaction, every MyBatis query path. Expect to find issues and budget time for fixes.

| OpenRewrite Coverage and Limitations OpenRewrite will handle approximately 70–80% of the mechanical changes: Java import rewrites, Maven dependency coordinate changes, and some XML namespace updates. It will NOT handle: JSF XHTML namespace changes (use sed/grep), custom component behavioral changes, PrimeFaces API changes between major versions, or OmniFaces API removals. Budget significant manual effort for XHTML and PrimeFaces fixes. |
| :---- |

## **Phase 3: Stabilization and Optimization**

**Estimated effort: 2-4 weeks. Low–medium risk. Cleanup and optimization after the big cut-over.**

**3.1 Fix remaining runtime issues.** Phase 2 testing will surface issues. Common problems: serialization issues with session-scoped beans, CDI scope resolution differences, EL expression evaluation changes between EL 3.0 and 5.0.

**3.2 Remove \--add-opens flags if any were added.** Java 21 module system restrictions may have required temporary workarounds. Now that all libraries are updated, most should no longer be needed.

**3.3 Performance testing.** Run load tests comparing the migrated application against the old baseline. Spring 6 and Tomcat 10.1 generally perform as well or better, but JSF rendering performance should be verified.

**3.4 Update deployment documentation and runbooks.** Update all ops documentation to reference Tomcat 10.1, Java 21, and any changed configuration parameters.

**3.5 Consider Spring 6.2 features.** With the migration complete, it’s now possible to leverage Spring 6.2 features like improved AOT compilation support, virtual thread integration with Tomcat 10.1 on Java 21, and the modernized parameter name retention.

# **Risk Register**

| Risk | Likelihood | Impact | Mitigation |
| :---- | :---- | :---- | :---- |
| **PrimeFaces 13→15 component breakage** | High | High | Review PF 14 and 15 migration guides before Phase 2\. Test every PF component in use. Budget 1–2 weeks for PF-specific fixes. |
| **JSF XHTML namespace migration errors** | High | Medium | Use automated sed/grep for bulk replacement, then manually verify each XHTML file compiles. Create a simple test script that loads every page. |
| **Spring 4→5 breaking changes in web layer** | Medium | High | Phase 1 isolates this risk. Full regression testing after the Spring 5 upgrade catches issues before the namespace wall. |
| **Third-party libs without jakarta support** | Medium | High | Phase 0 dependency audit identifies these early. For each, determine: is there a jakarta version, a replacement library, or can the Tomcat Migration Tool (bytecode transform) bridge it? |
| **MyBatis mapper XML incompatibilities** | Low | Medium | MyBatis core mapper XML format has been stable. Main risk is in SqlSessionFactory configuration changes in mybatis-spring 2.x/3.x. |
| **Insufficient test coverage misses regressions** | High | High | Phase 0 directly addresses this. Prioritize integration tests for critical business flows before starting any upgrades. |
| **OmniFaces migration issues** | Low | Low | Usage is limited to stable APIs (Faces.redirect, Ajax.oncomplete, \<o:importConstants\>). All survive to 4.x unchanged. Dependency swap only. |

# **OpenRewrite Quick-Start for Maven**

OpenRewrite automates many mechanical migration tasks. Add the following to pom.xml to use the recommended recipes:

| Maven Plugin Configuration Add to pom.xml \<build\>\<plugins\> section:\<plugin\>  \<groupId\>org.openrewrite.maven\</groupId\>     \<artifactId\>rewrite-maven-plugin\</artifactId\>    \<version\>6.33.0\</version\>    \<configuration\>    \<activeRecipes\>      \<recipe\>org.openrewrite.java.migrate.jakarta.JakartaEE10\</recipe\>      \<recipe\>org.openrewrite.java.spring.framework.UpgradeSpringFramework\_6\_0\</recipe\>    \</activeRecipes\>  \</configuration\>  \<dependencies\>    \<dependency\>      \<groupId\>org.openrewrite.recipe\</groupId\>      \<artifactId\>rewrite-migrate-java\</artifactId\>      \<version\>3.30.0\</version\>    \</dependency\>    \<dependency\>      \<groupId\>org.openrewrite.recipe\</groupId\>      \<artifactId\>rewrite-spring\</artifactId\>      \<version\>6.27.0\</version\>    \</dependency\>  \</dependencies\>\</plugin\>Run: mvn rewrite:dryRun (to preview) then mvn rewrite:run (to apply) |
| :---- |
|  |

Key OpenRewrite recipes for this migration, in recommended execution order:

**Phase 1 (Spring 5.3):** org.openrewrite.java.spring.framework.UpgradeSpringFramework\_5\_3

**Phase 2 (jakarta cut-over):** (a) org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta, (b) org.openrewrite.java.spring.framework.UpgradeSpringFramework\_6\_0, (c) org.openrewrite.java.migrate.jakarta.JakartaEE10, (d) org.openrewrite.java.migrate.UpgradeToJava21 (if upgrading JDK in this phase)

# **Estimated Timeline Summary**

| Phase | Duration | Risk Level | Key Deliverable |
| :---- | :---- | :---- | :---- |
| **Phase 0: Preparation** | 2–4 weeks | None | Test infrastructure, dependency \+ PF/OF audit, CI/CD baseline |
| **Phase 1: Spring 5.3** | 2–4 weeks | Medium | App on Spring 5.3 \+ MyBatis-Spring 2.1 \+ Java 17 \+ Tomcat 9 |
| **Phase 2: jakarta cut-over** | 4–8 weeks | High | Full jakarta: Spring 6.2, Java 21, Tomcat 10.1, Faces 4.0, PF 15 |
| **Phase 3: Stabilization** | 2–4 weeks | Low–Medium | Production-ready migrated application |
| **Total** | **10–20 weeks** |  | **Fully modernized enterprise application** |

This timeline assumes a single developer or small team working primarily on the migration. With a larger team working in parallel on Phase 0 test coverage and Phase 2 XHTML migration, the calendar time can be compressed significantly. The effort hours remain roughly the same.

# **Future Considerations**

**Spring 7.0 / Spring Boot 4.0:** Once stable on Spring 6.2, upgrading to Spring 7.0 is a relatively small step (Jakarta EE 11 baseline, Tomcat 11). This can be planned as a separate, lower-risk effort.

**Java 25 (LTS):** Released September 2025\. Once the ecosystem matures around it (mid-2026), upgrading from Java 21 to 25 should be straightforward.

**Spring Boot adoption:** Current project uses Spring Framework directly (no Spring Boot). Once on Spring 6.2, adopting Spring Boot 3.4.x is feasible and would provide auto-configuration, embedded Tomcat option, and simplified dependency management. However, this is a significant architectural change for a WAR-based JSF application and should be evaluated carefully.

