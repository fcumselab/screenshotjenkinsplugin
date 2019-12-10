package io.jenkins.plugins.screenshot;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.Symbol;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public class ScreenShotBuilder extends Builder implements SimpleBuildStep {

  private final static String HTML = ".html";
  private final String seleniumUrl;
  private final static int JENKINS_WORKSPACE_PATH = 28;

  @DataBoundConstructor
  public ScreenShotBuilder(String seleniumUrl) {
    this.seleniumUrl = seleniumUrl;
  }

  public String getSeleniumUrl() {
    return seleniumUrl;
  }

  // --step 1--
  private static List<String> searchAllHtmlFile(String filePath) {
    List<String> allFiles = null;
    try (Stream<Path> walk = Files.walk(Paths.get(filePath))) {

      allFiles = walk.filter(Files::isRegularFile).map(x -> x.toString())
          .filter(f -> f.endsWith(HTML)).collect(Collectors.toList());

    } catch (IOException e) {
      e.printStackTrace();
    }
    //cut /var/jenkins_home/workspace/ 28 chars
    for (int i = 0; i < allFiles.size(); i++) {
      allFiles.set(i, allFiles.get(i).substring(JENKINS_WORKSPACE_PATH));
    }

    return allFiles;
    }
  // --step 1/--
  
  // --step 2--
  private void screenshotMethod(FilePath workspace, List<String> htmlFiles, TaskListener listener) throws IOException {
    String screenshotPath = workspace + "/target/screenshot/";

    String driverGetPath = "file:////var/lib/workspace/";
    // create screenshot folder
    File screenshotFolder = new File(screenshotPath);
    if (!screenshotFolder.exists()) {
      screenshotFolder.mkdirs();
    }

    // screenshot method
    DesiredCapabilities capabilities = DesiredCapabilities.chrome();
    ChromeOptions options = new ChromeOptions();
    options.addArguments("--window-size=1024,768");
    capabilities.setCapability(ChromeOptions.CAPABILITY, options);
    WebDriver driver = new RemoteWebDriver(new URL(seleniumUrl), capabilities);
    for (int i = 0; i < htmlFiles.size(); i++) {
      String htmlFile = driverGetPath + htmlFiles.get(i);
      driver.get(htmlFile);
      TakesScreenshot screenshot = ((TakesScreenshot) driver);
      File screenshotFile = screenshot.getScreenshotAs(OutputType.FILE);
      String fileName = new File(htmlFiles.get(i)).getName();//get HTML file name
      FileUtils.copyFile(screenshotFile, new File(screenshotFolder + "/" + fileName + ".png"));
    }
    listener.getLogger().println("save the screenshot png in target/screenshot/");
    driver.quit();
  }
  // --step 2/--

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {

    String filePath = workspace + "/src";
    // step1: search all html file
    List<String> htmlFiles = null;
    htmlFiles = searchAllHtmlFile(filePath);
    for (int i = 0; i < htmlFiles.size(); i++) {
      listener.getLogger().println("Find: " + htmlFiles.get(i));
    }

    // step2: screenshot method
    screenshotMethod(workspace, htmlFiles, listener);
  }

  @Symbol("greet")
  @Extension
  public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public String getDisplayName() {
      return "Selenium Screenshot Method";
    }

  }

}
