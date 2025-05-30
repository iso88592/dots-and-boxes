package hu.mocman.dotsandboxes.selenium;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClientTests {
    private static WebDriver webDriver;
    private String SUT = "http://localhost:8080/";
    private static List<TestClient> clients = new ArrayList<>();

    @BeforeAll
    public static void setup() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--kiosk");
        webDriver = new FirefoxDriver(options);
    }

    @AfterAll
    public static void tearDown() {
        webDriver.quit();
        for (TestClient client : clients) {
            client.deleteRepo();
        }
    }

    @Test
    public void clientCanCreateANewRepo() throws NoSuchAlgorithmException, IOException {
        TestClient testClient = new TestClient();
        testClient.createSslKeys();
        clients.add(testClient);
        webDriver.get(SUT);
        webDriver.findElement(By.id("join")).click();
        webDriver.findElement(By.id("ssh-key")).sendKeys(testClient.getPublicKey());
        webDriver.findElement(By.id("ssh-key")).submit();

        new WebDriverWait(webDriver, java.time.Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("clone-command")));

        Assertions.assertThat(webDriver.findElement(By.id("clone-command")).getText()).isNotEmpty();

        String cloneCommand = webDriver.findElement(By.id("clone-command")).getText();

        testClient.cloneRepo(cloneCommand);

        Assertions.assertThat(testClient.hasRepo()).isTrue();
    }

    @Test
    public void clientCanAlterTheirNameAndItShowsOnTheDashboard() throws NoSuchAlgorithmException, IOException {
        TestClient testClient = new TestClient();
        testClient.createSslKeys();
        clients.add(testClient);
        webDriver.get(SUT);
        webDriver.findElement(By.id("join")).click();
        webDriver.findElement(By.id("ssh-key")).sendKeys(testClient.getPublicKey());
        webDriver.findElement(By.id("ssh-key")).submit();

        new WebDriverWait(webDriver, java.time.Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("clone-command")));

        Assertions.assertThat(webDriver.findElement(By.id("clone-command")).getText()).isNotEmpty();

        String cloneCommand = webDriver.findElement(By.id("clone-command")).getText();

        testClient.cloneRepo(cloneCommand);

        testClient.replaceInFile("dots-and-boxes/DotsAndBoxes/DotsAndBoxesLib/ExampleGame.cs", "Várkonyi Tibor", "Teszt Elek");

        testClient.commitAndPush();

        webDriver.get(SUT);

        String containerIp = cloneCommand.replaceAll(".*root@","").replaceAll(":.*","");

        new WebDriverWait(webDriver, java.time.Duration.ofSeconds(10))
                .until(ExpectedConditions.textToBePresentInElementLocated(By.id("participants"), containerIp));

        Assertions.assertThat(webDriver.findElement(By.id("participants")).getText())
                .contains("Teszt Elek")
                .contains(containerIp);


    }
}
