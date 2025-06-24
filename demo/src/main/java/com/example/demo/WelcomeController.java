package com.example.demo;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
@ViewScoped
public class WelcomeController implements Serializable {

    private static final long serialVersionUID = 1L;

    private String welcomeMessage;
    private List<String> items;

    @PostConstruct
    public void init() {
        welcomeMessage = "Welcome to PrimeFaces with Spring Boot!";
        items = new ArrayList<>();
        items.add("Item 1");
        items.add("Item 2");
        items.add("Item 3");
        items.add("Item 4");
        items.add("Item 5");
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public List<String> getItems() {
        return items;
    }
}
