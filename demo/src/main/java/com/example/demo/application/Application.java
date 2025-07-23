package com.example.demo.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.properties")
public class Application {

    @Autowired
    private Environment environment;
    
    public String getProfile() {
        return environment.getProperty("joinfaces.jsf.project-stage");
    }

    public String getProperty(String key) {
        return environment.getProperty(key);
    }

}
