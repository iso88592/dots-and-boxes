package hu.mocman.dotsandboxes.selenium;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class DashboardTests {
    private static WebDriver webDriver;
    private String SUT = "http://localhost:8080/";

    @BeforeAll
    public static void setup() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--kiosk");
        webDriver = new FirefoxDriver(options);
    }

    @AfterAll
    public static void tearDown() {
        webDriver.quit();
    }

    @Test
    public void dashboardTitleShouldBeDotsAndBoxes() {
        webDriver.get(SUT);
        Assertions.assertThat(webDriver.getTitle()).isEqualTo("Dots and Boxes");
    }

    @Test
    public void dashboardShowsParticipantList() {
        webDriver.get(SUT);
        Assertions.assertThat(webDriver.findElement(By.id("participants")).getText()).contains("Participants");
    }

}
