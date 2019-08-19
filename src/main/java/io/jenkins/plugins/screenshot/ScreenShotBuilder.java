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
import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.Symbol;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.Dimension;

public class ScreenShotBuilder extends Builder implements SimpleBuildStep {

  private final static String HTML = ".html";
  private final String seleniumUrl;
  

  @DataBoundConstructor
  public ScreenShotBuilder(String seleniumUrl) {
    this.seleniumUrl = seleniumUrl;
  }

  public String getSeleniumUrl() {
    return seleniumUrl;
  }

  // --step 1--
  private static ArrayList searchAllHtmlFile(String filePath,String screenshotPath) throws IOException{
    ArrayList allFiles = new ArrayList<>();
    File file = new File(filePath);
    if(file.isDirectory()){
      for (String fileName:file.list()){
        allFiles.addAll(searchAllHtmlFile(filePath+"/"+fileName,screenshotPath));
      }
    }else{
      if(filePath.toString().contains(HTML)){
        File targetHtml = new File(screenshotPath + "/" + filePath.substring(filePath.lastIndexOf("/")+1));
        FileUtils.copyFile(file,targetHtml);

        String htmlName = filePath.substring(filePath.lastIndexOf("/")+1);
        allFiles.add(htmlName.substring(0, htmlName.length() - HTML.length()));
        return allFiles;
      }else{
        return allFiles;
      }
    }
    return allFiles;
    }
  // --step 1/--
  
  // --step 2--
  private void screenshotMethod(FilePath workspace, List<String> htmlFiles, TaskListener listener) throws IOException {
    String screenshotPath = workspace + "/target/screenshot/";

    String jobName = workspace.toString().substring(workspace.toString().lastIndexOf("/") + 1);
    String driverGetPath = Jenkins.getInstance().getRootUrl() + "job/" + jobName + "/ws/target/screenshot/";
    // create screenshot folder
    File screenshotFolder = new File(screenshotPath);
    if (!screenshotFolder.exists()) {
      screenshotFolder.mkdirs();
    }

    // screenshot method
    WebDriver driver = new RemoteWebDriver(new URL(seleniumUrl), DesiredCapabilities.chrome());
    for (int i = 0; i < htmlFiles.size(); i++) {
      Dimension windowDimension = new Dimension(1920,2160);
      driver.manage().window().maximize();
      driver.manage().window().setSize(windowDimension);
      driver.manage().window().fullscreen();
      String htmlFile = driverGetPath + htmlFiles.get(i) + HTML;
      driver.get(htmlFile);
      TakesScreenshot screenshot = ((TakesScreenshot) driver);
      File screenshotFile = screenshot.getScreenshotAs(OutputType.FILE);
      FileUtils.copyFile(screenshotFile, new File(screenshotFolder + "/" + htmlFiles.get(i) + ".png"));
    }
    listener.getLogger().println("save the screenshot png in target/screenshot/");
    driver.quit();
  }
  // --step 2/--

  // --step 3--
  private void deleteHtmlFile(String screenshotPath, List<String> htmlFiles){
    for (String file : htmlFiles){
      File deleteFile = new File(screenshotPath + file + ".html");
      deleteFile.delete();
    }

  }
  // --step3/--
  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
      throws InterruptedException, IOException {

    String screenshotPath = workspace + "/target/screenshot/";
    String filePath = workspace + "/src";
    // step1: search all html file
    ArrayList htmlFiles = new ArrayList<>();
    htmlFiles = searchAllHtmlFile(filePath, screenshotPath);

    // step2: screenshot method
    screenshotMethod(workspace, htmlFiles, listener);

    // step3: delete html file in screenshot folder
    deleteHtmlFile(screenshotPath,htmlFiles);
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
