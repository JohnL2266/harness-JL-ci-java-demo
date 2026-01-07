package com.example;

// ------------------------------------------------------------
// Unit tests for the App class (JUnit 5)
// ------------------------------------------------------------

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    // ------------------------------------------------------------
    // Test: normal mode returns a "Hello" greeting with the name
    // ------------------------------------------------------------
    @Test
    public void normalGreetingIncludesHelloAndName() {

        // Arrange: create app instance (no server needed)
        App app = new App();

        // Act: call the deterministic test helper
        String greeting = app.getGreetingForTest("John", "normal");

        // Assert: verify key behavior (not the whole string)
        assertTrue(
                greeting.contains("Hello, John"),
                "Normal greeting should include 'Hello, John'"
        );
    }

    // ------------------------------------------------------------
    // Test: pirate mode returns an "Ahoy" greeting with the name
    // ------------------------------------------------------------
    @Test
    public void pirateGreetingIncludesAhoy() {

        // Arrange
        App app = new App();

        // Act
        String greeting = app.getGreetingForTest("John", "pirate");

        // Assert
        assertTrue(
                greeting.contains("Ahoy, John"),
                "Pirate greeting should include 'Ahoy, John'"
        );
    }

    // ------------------------------------------------------------
    // Test: greeting includes CI/CD metadata fields
    // ------------------------------------------------------------
    @Test
    public void greetingIncludesMetadata() {

        // Arrange
        App app = new App();

        // Act
        String greeting = app.getGreetingForTest("Alice", "normal");

        // Assert: verify CI/CD-relevant metadata is present
        assertTrue(
                greeting.contains("Version:"),
                "Greeting should include version info"
        );
        assertTrue(
                greeting.contains("Git SHA:"),
                "Greeting should include git commit SHA"
        );
        assertTrue(
                greeting.contains("Greeting Mode:"),
                "Greeting should include greeting mode information"
        );
        assertTrue(
                greeting.contains("Pod:"),
                "Greeting should include pod name"
        );
        assertTrue(
                greeting.contains("Instance:"),
                "Greeting should include instance ID"
        );
    }
}