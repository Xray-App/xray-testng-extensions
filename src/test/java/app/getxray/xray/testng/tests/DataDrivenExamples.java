package app.getxray.xray.testng.tests;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.testng.annotations.Parameters;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import app.getxray.xray.testng.annotations.Requirement;
import app.getxray.xray.testng.annotations.XrayTest;

public class DataDrivenExamples {
 
    @Test
    @Parameters({"value", "isEven"})
    public void givenNumberFromXML_ifEvenCheckOK_thenCorrect(int value, boolean isEven) {
        assertEquals(isEven, value % 2 == 0);
    }

    @DataProvider(name = "numbers")
    public static Object[][] evenNumbers() {
        // the last data/example if not correct on purpose, to fail the test
        return new Object[][]{{1, false}, {2, true}, {4, true}, {5, true}};
    }
  

    @Test(dataProvider = "numbers")
    public void givenNumberFromDataProvider_ifEvenCheckOK_thenCorrect(Integer number, boolean expected) {

        // add dummy evidence
        ITestResult result = Reporter.getCurrentTestResult();
        File attachments[] = new File[] { new File("1.png"), new File("2.png") } ;
        result.setAttribute("attachments", attachments); 

        assertEquals(expected, number % 2 == 0);
    }


}
