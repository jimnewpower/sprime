package com.example.demo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.application.AppCache;
import com.example.demo.application.Application;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;

@Component
@ViewScoped
public class WelcomeController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired
    private Application application;

    @Autowired
    private AppCache appCache;

    @PostConstruct
    public void init() {
        String welcomeMessage = "Welcome to PrimeFaces with Spring Boot";
        List<String> items = new ArrayList<>();
        items.add("Item 1");
        items.add("Item 2");
        items.add("Item 3");
        items.add("Item 4");
        items.add("Item 5");
        appCache.put("welcomeMessage", welcomeMessage);
        appCache.put("items", items);
    }

    public String getWelcomeMessage() {
        return (String) appCache.get("welcomeMessage");
    }

    @SuppressWarnings("unchecked")
    public List<String> getItems() {
        return (List<String>) appCache.get("items");
    }

    public String getProfile() {
        return application.getProfile();
    }
}
