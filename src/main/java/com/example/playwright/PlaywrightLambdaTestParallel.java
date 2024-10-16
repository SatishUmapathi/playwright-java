package com.example.playwright;

import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;

import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaywrightLambdaTestParallel {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        executor.submit(() -> runTest("chrome", "latest", "Windows 10"));
        executor.submit(() -> runTest("pw-firefox", "latest", "Windows 11"));
        executor.submit(() -> runTest("MicrosoftEdge", "latest", "Windows 10"));
        executor.submit(() -> runTest("chrome", "latest", "macOS Big sur"));
        executor.submit(() -> runTest("pw-webkit", "latest", "macOS Ventura"));

        executor.shutdown();
    }

    // Method to run a test in parallel
    public static void runTest(String browserName, String browserVersion, String platform) {

        try (Playwright playwright = Playwright.create()) {
            JsonObject capabilities = new JsonObject();
            JsonObject ltOptions = new JsonObject();

            String user = "satishumapathi";
            String accessKey = "ndeW5tPC3b22DwpTcleeT44Xcdk58kRA8yF1rfA8atgbxMFm0l";

            capabilities.addProperty("browsername", browserName);
            capabilities.addProperty("browserVersion", browserVersion);
            ltOptions.addProperty("platform", platform);
            ltOptions.addProperty("name", platform + " Playwright Test in parallel " + browserName);
            ltOptions.addProperty("build", "Playwright Build parallel");
            ltOptions.addProperty("user", user);
            ltOptions.addProperty("accessKey", accessKey);
            capabilities.add("LT:Options", ltOptions);
            BrowserType chromium;

            switch (browserName) {
                case "Safari":
                    chromium = playwright.webkit();
                    break;
                case "Firefox":
                    chromium = playwright.firefox();
                    break;
                default:
                    chromium = playwright.chromium();
                    break;
            }
            String caps = URLEncoder.encode(capabilities.toString(), "utf-8");
            String cdpUrl = "wss://cdp.lambdatest.com/playwright?capabilities=" + caps;
            Browser browser = chromium.connect(cdpUrl);
            Page page = browser.newPage();
            try {
                page.navigate("https://www.primevideo.com", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
                Thread.sleep(5000);
                String title = page.title();
                if (title.equalsIgnoreCase("Welcome to Prime Video")) {
                    Locator joinPrimeButton = page.locator("[aria-label=\"Join Prime\"]");

                    joinPrimeButton.nth(0).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
                    joinPrimeButton.nth(0).click();
                    page.keyboard().press("Enter");

                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    Thread.sleep(5000);

                    setTestStatus("passed", "Title matched", page);
                } else {
                    setTestStatus("failed", "Title not matched", page);
                }

            } catch (Exception err) {
                setTestStatus("failed", err.getMessage(), page);
                err.printStackTrace();
            }
            browser.close();
        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    public static void setTestStatus(String status, String remark, Page page) {
        page.evaluate("_ => {}", "lambdatest_action: { \"action\": \"setTestStatus\", \"arguments\": { \"status\": \"" + status + "\", \"remark\": \"" + remark + "\"}}");
    }
}
