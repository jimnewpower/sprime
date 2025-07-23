package com.example.demo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.weld.exceptions.IllegalArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootVersion;
import org.springframework.stereotype.Component;

import com.example.demo.application.AppCache;
import com.example.demo.application.Application;
import com.example.demo.application.Env;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Component
@ViewScoped
public class WelcomeController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LogManager.getLogger(WelcomeController.class);

    @Autowired
    private Application application;

    @Autowired
    private AppCache appCache;

    @PostConstruct
    public void init() {
        logger.info("Initializing WelcomeController");
        String welcomeMessage = "Welcome to PrimeFaces with Spring Boot";
        List<String> items = new ArrayList<>();
        items.add("Item 1");
        items.add("Item 2");
        items.add("Item 3");
        items.add("Item 4");
        items.add("Item 5");
        appCache.put("welcomeMessage", welcomeMessage);
        appCache.put("items", items);
        logger.debug("Welcome message and items added to cache");
    }

    public String getWelcomeMessage() {
        return (String) appCache.get("welcomeMessage");
    }

    @SuppressWarnings("unchecked")
    public List<String> getItems() {
        return (List<String>) appCache.get("items");
    }

    public String getProperty(String key) {
        return application.getProperty(key);
    }

    public Env getEnv() {
        return application.getEnv();
    }

    public String getJavaVersion() {
        return System.getProperty("java.version");
    }

    public String getJsfVersion() {
        Package jsfPkg = jakarta.faces.FactoryFinder.class.getPackage();
        return jsfPkg.getImplementationVersion() + " (spec: " + jsfPkg.getSpecificationVersion() + ")";
    }

    public String getSpringBootVersion() {
        return SpringBootVersion.getVersion();
    }

    public String getPrimeFacesVersion() {
        Package pkg = org.primefaces.PrimeFaces.class.getPackage();
        String impl = pkg.getImplementationVersion();
        String spec = pkg.getSpecificationVersion();
        if (impl == null && spec == null) {
            return "Unknown (see pom.xml)";
        }
        return impl + " (spec: " + spec + ")";
    }

    public void throwException() {
        logger.error("Test exception thrown from WelcomeController");
        throw new IllegalArgumentException("Test exception");
    }
}
