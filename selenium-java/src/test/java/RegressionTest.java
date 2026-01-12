/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

public class RegressionTest {

  private WebDriver driver;

  @BeforeEach
  public void setUp() {
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--headless");
    options.addArguments("--no-sandbox");

    // By default, the test uses the latest stable Chrome version.
    // Replace the "stable" with the specific browser version if needed,
    // e.g. 'canary', '115' or '144.0.7534.0' for example.
    options.setBrowserVersion("stable");

    ChromeDriverService service =
        new ChromeDriverService.Builder()
            .withLogFile(new java.io.File("chromedriver.log"))
            .withVerbose(true)
            .build();

    driver = new ChromeDriver(service, options);
  }

  @AfterEach
  public void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  @Test
  public void verifySetup_shouldBeAbleToNavigateToGoogleCom() {
    // Navigate to a URL
    driver.get("https://www.google.com");
    // Assert that the navigation was successful
    assertEquals("Google", driver.getTitle());
  }

  @Test
  public void ISSUE_REPRODUCTION() {
    // Benchmark for Chrome (Classic vs BiDi vs CDP)
    ChromeOptions options = new ChromeOptions();
    options.enableBiDi();
    options.addArguments("--headless=new");
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    runBenchmark(new ChromeDriver(options), "Chrome");
  }

  private static final int ITERATIONS = 5000;

  private void runBenchmark(WebDriver driver, String browserName) {
    System.out.println("\n=== Testing Browser: " + browserName + " ===");
    try {
      driver.get("about:blank");
      org.openqa.selenium.JavascriptExecutor setupExec = (org.openqa.selenium.JavascriptExecutor) driver;

      setupExec.executeScript(
          "document.body.innerHTML = `" +
              "<div style='font-family:Segoe UI, sans-serif; padding:20px; background:#f4f7f6;'>" +
              "  <h2>" + browserName + " Protocol Benchmark</h2>" +
              "  <div style='display:flex; gap:15px;'>" +
              "    <div id='classic-box' style='flex:1; padding:15px; background:white; border-left:5px solid #e74c3c;'>Classic HTTP<div id='classic-counter' style='font-size:24px;'>0</div><div id='classic-res'>-</div></div>" +
              "    <div id='bidi-box' style='flex:1; padding:15px; background:white; border-left:5px solid #2ecc71;'>WebDriver BiDi<div id='bidi-counter' style='font-size:24px;'>0</div><div id='bidi-res'>-</div></div>" +
              "    <div id='cdp-box' style='flex:1; padding:15px; background:white; border-left:5px solid #3498db;'>CDP (Legacy WS)<div id='cdp-counter' style='font-size:24px;'>0</div><div id='cdp-res'>-</div></div>" +
              "  </div>" +
              "</div>`;"
      );

      String handle = driver.getWindowHandle();

      // 1. Classic Benchmark
      long startClassic = System.nanoTime();
      for (int i = 0; i < ITERATIONS; i++) {
        setupExec.executeScript("document.getElementById('classic-counter').innerText = 'Iter: " + (i + 1) + "';");
      }
      long endClassic = System.nanoTime();
      double classicAvg = printOnPage(setupExec, "classic", endClassic - startClassic);

      // 2. BiDi Benchmark
      double bidiAvg = 0;
      try (org.openqa.selenium.bidi.module.Script bidiModule = new org.openqa.selenium.bidi.module.Script(driver)) {
        org.openqa.selenium.bidi.script.ContextTarget target = new org.openqa.selenium.bidi.script.ContextTarget(handle);
        long startBidi = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
          bidiModule.evaluateFunction(new org.openqa.selenium.bidi.script.EvaluateParameters(target, "document.getElementById('bidi-counter').innerText = 'Iter: " + (i + 1) + "';", false));
        }
        long endBidi = System.nanoTime();
        bidiAvg = printOnPage(setupExec, "bidi", endBidi - startBidi);
      }

      // 3. CDP Benchmark
      double cdpAvg = 0;
      if (driver instanceof org.openqa.selenium.devtools.HasDevTools) {
        org.openqa.selenium.devtools.HasDevTools hasDevTools = (org.openqa.selenium.devtools.HasDevTools) driver;
        org.openqa.selenium.devtools.DevTools devTools = hasDevTools.getDevTools();
        devTools.createSession();
        long startCdp = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
          devTools.send(org.openqa.selenium.devtools.v143.runtime.Runtime.evaluate("document.getElementById('cdp-counter').innerText = 'Iter: " + (i + 1) + "';",
              java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
              java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
              java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
              java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty()));
        }
        long endCdp = System.nanoTime();
        cdpAvg = printOnPage(setupExec, "cdp", endCdp - startCdp);
      } else {
        setupExec.executeScript("document.getElementById('cdp-counter').innerText = 'Unsupported';");
      }

      System.out.printf("[%s] Classic: %.4f | BiDi: %.4f | CDP: %.4f ms/call%n",
          browserName, classicAvg, bidiAvg, cdpAvg);
    } finally {
      try {
        Thread.sleep(3000);
      } catch (Exception ignored) {
      }
      driver.quit();
    }
  }

  private static double printOnPage(org.openqa.selenium.JavascriptExecutor executor, String prefix, long nanoTime) {
    double avg = (nanoTime / 1_000_000.0) / ITERATIONS;
    executor.executeScript("document.getElementById('" + prefix + "-res').innerText = 'Avg: " + String.format("%.4f", avg) + " ms/call';");
    return avg;
  }
}
