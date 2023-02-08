package app.getxray.xray.testng.tests;

import java.io.File;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Test;

import app.getxray.xray.testng.annotations.Requirement;
import app.getxray.xray.testng.annotations.XrayTest;

public class BasicExamples {
 
    @Test
    public void legacyTest() {
        Assert.assertEquals(true, true);
    }

    @Test
    public void testWithAttachment()
    {
        ITestResult result = Reporter.getCurrentTestResult();
        String myFile = "src/test/java/app/getxray/xray/testng/tests/xray.png";
        File attachments[] = new File[] { new File(myFile) } ;
        result.setAttribute("attachments", attachments);
    }

    @Test
    @XrayTest(summary = "custom summary")
    public void annotatedWithXrayTestSummary() {
    }

    @Test(description = "custom test level description")
    public void annotatedWithTestDescription() {
    }

    @Test
    @XrayTest(description = "custom description")
    public void annotatedWithXrayTestDescription() {
    }

    @Test
    @Requirement(key = "CALC-1234")
    public void annotatedWithRequirementKey() {
    }

    @Test
    @XrayTest(key = "CALC-2000")
    public void annotatedWithXrayTestKey() {
    }

    @Test
    @XrayTest(labels = "core addition")
    public void annotatedWithXrayTestLabels() {
    }

}
