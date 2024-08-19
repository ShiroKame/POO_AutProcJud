package com.example.demo;

import org.springframework.stereotype.Component;

@Component
public class FirstBootStatus {

    private boolean firstBootCompleted = true;

    public boolean isFirstBootCompleted() {
        return firstBootCompleted;
    }

    public void setFirstBootCompleted(boolean firstBootCompleted) {
        this.firstBootCompleted = firstBootCompleted;
    }
}
