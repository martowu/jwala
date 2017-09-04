package com.cerner.jwala.ui.selenium.steps.configuration;

import com.cerner.jwala.ui.selenium.component.JwalaUi;
import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertFalse;

/**
 * Created by SB053052 on 9/2/2017.
 */
public class HandleResourceRunSteps {

    @Autowired
    private JwalaUi jwalaUi;

    @Autowired
    @Qualifier("parameterProperties")
    private Properties paramProp;

    @When("^I select resource file \"(.*)\"$")
    public void selectFile(String fileName) {
        jwalaUi.click(By.xpath("//span[text()='" + fileName + "']"));
    }

    @When("^I right click resource file \"(.*)\"$")
    public void rightClickFile(String fileName) {
        jwalaUi.rightClick(By.xpath("//span[text()='" + fileName + "']"));
    }

    @When("^I click resource deploy option$")
    public void clickDeploy() {
        jwalaUi.click(By.xpath("//*[text()='deploy']"));
    }

    @When("^I click resource deploy to a host option$")
    public void clickDeployToAHost() {
        jwalaUi.click(By.xpath("//*[text()='a host']"));
    }

    @When("^I click resource deploy All option$")
    public void clickDeployAll() {
        jwalaUi.click(By.xpath("//*[text()='all hosts']"));
    }

    @Then("^I click yes button to deploy a resource popup")
    public void confirmDeployPopup() {
        jwalaUi.click(By.xpath("//*[text()='Yes']"));
    }


    @When("^I click \"(.*)\" component$")
    public void selectComponent(String component) {
        jwalaUi.clickWhenReady(By.xpath("//span[contains(text(),'" + component + "')]"));
    }

    @And("^I select the resource file \"([^\"]*)\"$")
    public void clickResource(String resource) {
        jwalaUi.click(By.xpath("//span[text()='" + resource + "']"));
    }

    @When("^I expand \"(.*)\" node in data tree$")
    public void expandPropertyDataTree(String property) {
        jwalaUi.clickWhenReady(By.xpath("//span[contains(@class,'nodeKey') and contains(text(),'" + property + "') ]/preceding-sibling::span"));
    }

    @Then("^I verify dataTree has the key-value pair as \"(.*)\" and \"(.*)\" respectively$")
    public void verifyDataTree(String key, String value) {
        jwalaUi.waitUntilElementIsVisible(By.xpath("//span[contains(@class,'nodeKey') and contains(text(),'" + key + "') ]"));
        jwalaUi.waitUntilElementIsVisible(By.xpath("//span[contains(@class,'nodeVal') and contains(text(),'" + value + "')]"));
    }

    @When("^I add property \"(.*)\"$")
    public void addProperty(String property) {
        jwalaUi.click(By.xpath("//div[contains(@class, 'CodeMirror') and contains(@class, 'cm-s-default')]"));
        jwalaUi.sendKeys(Keys.ENTER + property + Keys.ENTER);
    }

    /**
     * @param text
     * @param textPosition Inserts a value in the edit box of a resource at a specific position in the file
     */

    @When("^I enter value \"(.*)\" in the edit box at text \"(.*)\"$")
    public void enterInEditBox(String text, String textPosition) {
        jwalaUi.clickWhenReady(By.xpath("//*[text()='" + textPosition + "']"));
        jwalaUi.sendKeys(Keys.ENTER);
        jwalaUi.sendKeys(text);
        jwalaUi.sendKeys(Keys.ENTER);
    }

    /*
        clicks the save button of the template or the metadata, as the xpath returns both the elements
     */
    @When("^I click save button of edit box of \"(.*)\"$")
    public void clickSaveButton(String tab) {
        List<WebElement> elements = jwalaUi.getWebDriver().findElements(By.xpath("//span[contains(@class, 'ui-icon-disk') and @title='Save']"));
        WebElement saveElement = tab.equals("Template") ? elements.get(0) : elements.get(1);
        saveElement.click();
    }

    @When("^I wait for notification \"(.*)\"")
    public void waitForNotification(String notification) {
        jwalaUi.waitUntilElementIsVisible(By.xpath("//div[text()='" + notification + "']"));
    }

    @Then("^I verify edit \"(.*)\"$")
    public void verifyText(String text) {
        jwalaUi.waitUntilElementIsVisible(By.xpath("//div[contains(.,'" + text + "')]"));
    }

    @When("^I click the resource delete icon$")
    public void clickDelete() {
        jwalaUi.click(By.xpath("//*[@title='delete']"));
    }

    @When("^I click check-box for resourceFile \"(.*)\"$")
    public void selectResourceCheckBox(String resourceFile) {
        jwalaUi.clickWhenReady(By.xpath("//li/span[text()='" + resourceFile + "']/preceding-sibling::input[@type='checkbox']"));
    }

    @And("^I click yes button to delete a resource$")
    public void deleteResource() {
        jwalaUi.click(By.xpath("//button/span[text()='Yes']"));
    }


    @And("^I click \"([^\"]*)\" tab$")
    public void clickTab(String tab) {
        jwalaUi.clickTab(tab);
    }

    @And("^I check for unable to save error$")
    public boolean checkForUnableToSaveError() {
        return jwalaUi.isElementExists(By.xpath("//*[contains(text(),'Unable to save')]"));
    }

    /*
        Needed if resources are immediately saved one after the other, resulting in a saved notifcation hovering over the saved button
     */
    @And("^I wait for the save button to be visible again$")
    public void waitForSaveButton() {
        jwalaUi.waitUntilElementIsNotVisible(By.xpath("//*[contains(text(),'Saved')]"));
      jwalaUi.waitUntilElementIsClickable(By.xpath("//*[contains(@class, 'ui-icon-disk') and @title='Save']"),60);
    }
}
