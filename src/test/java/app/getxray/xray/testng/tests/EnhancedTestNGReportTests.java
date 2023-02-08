package app.getxray.xray.testng.tests;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.joox.Match;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterSuite;
import org.testng.TestNG;
import org.testng.reporters.XMLReporter;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import app.getxray.xray.testng.listeners.XrayListener;

import org.testng.ITestNGListener;

import static org.joox.JOOX.$;

public class EnhancedTestNGReportTests {

    private Path tempDirectory;
    private static final String REPORT_NAME = "testng-results.xml";
    private static final Class TEST_EXAMPLES_CLASS = BasicExamples.class;
    private static final Class TEST_EXAMPLES_USING_LISTENER_CLASS = ReportListenerExamples.class;
  
    @BeforeSuite
    public void setUp() throws Exception {
        tempDirectory = Files.createTempDirectory("testng");
    }

    @AfterSuite
    public void tearDown() throws Exception {
    }

    @Test
    public void legacyTestsShouldNotHaveXrayAttributes() throws Exception {
        String testMethodName = "legacyTest";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        
        Match report = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testsuite = report.child("suite");
        Match testcase = testsuite.child("test").child("class").child("test-method");
        Assert.assertEquals(testcase.attr("name", String.class), testMethodName);
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "summary"), Collections.emptySet());
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "description"), Collections.emptySet());
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "test"), Collections.emptySet());
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "requirement"), Collections.emptySet());
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "labels"), Collections.emptySet());
    }

    @Test
    public void shouldMapXrayTestSummaryToTestcaseAttribute() throws Exception {
        String testMethodName = "annotatedWithXrayTestSummary";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        
        Match report = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testsuite = report.child("suite");
        Match testcase = testsuite.child("test").child("class").child("test-method");
        Assert.assertEquals(testcase.attr("name", String.class), testMethodName);
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "summary").cdata().trim(), "custom summary");
    }

    @Test
    public void shouldMapXrayTestDescriptionToTestcaseAttribute() throws Exception {
        String testMethodName = "annotatedWithXrayTestDescription";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        
        Match report = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testsuite = report.child("suite");
        Match testcase = testsuite.child("test").child("class").child("test-method");
        Assert.assertEquals(testcase.attr("name", String.class), testMethodName);
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "description").cdata().trim(), "custom description");
    }

    @Test
    public void shouldMapXrayTestKeyToTestcaseAttribute() throws Exception {
        String testMethodName = "annotatedWithXrayTestKey";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        
        Match report = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testsuite = report.child("suite");
        Match testcase = testsuite.child("test").child("class").child("test-method");
        Assert.assertEquals(testcase.attr("name", String.class), testMethodName);
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "test").cdata().trim(), "CALC-2000");
    }

    @Test
    public void shouldMapXrayRequirementKeyToTestcaseAttribute() throws Exception {
        String testMethodName = "annotatedWithRequirementKey";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        
        Match report = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testsuite = report.child("suite");
        Match testcase = testsuite.child("test").child("class").child("test-method");
        //System.out.println(testcase);
        Assert.assertEquals(testcase.attr("name", String.class), testMethodName);
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "requirement").cdata().trim(), "CALC-1234");
    }

    @Test
    public void shouldMapXraySpaceDelimitedLabelsToTestcaseAttribute() throws Exception {
        String testMethodName = "annotatedWithXrayTestLabels";
        executeTestMethod(TEST_EXAMPLES_CLASS, testMethodName);
        
        Match report = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testsuite = report.child("suite");
        Match testcase = testsuite.child("test").child("class").child("test-method");
        Assert.assertEquals(testcase.attr("name", String.class), testMethodName);
        Assert.assertEquals(testcase.child("attributes").children("attribute").matchAttr("name", "labels").cdata().trim(), "core addition");
    }

    private Match readValidXmlFile(Path xmlFile) throws Exception {
		Assert.assertTrue(Files.exists(xmlFile), "File does not exist: " + xmlFile);
		try (BufferedReader reader = Files.newBufferedReader(xmlFile)) {
			Match xml = $(reader);
            //assertValidAccordingToTestNGSchema(xml.document());
			return xml;
		}
	}

    /*
     * This requires the implementation of this story in Xray side: https://jira.getxray.app/browse/XRAY-4557
     */
    @Test
    public void shouldStoreTestRunEvidenceToTestcaseProperty() throws Exception {
        String testMethodName = "addAttachements";
        executeTestMethod(TEST_EXAMPLES_USING_LISTENER_CLASS, testMethodName);
        
        String file = "src/test/java/app/getxray/xray/testng/tests/xray.png";
        Base64.Encoder enc = Base64.getEncoder();
        byte[] fileContent = Files.readAllBytes(Paths.get(file));
        byte[] encoded = enc.encode(fileContent);
        String contentInBase64 = new String(encoded, "UTF-8");
        
        Match report = readValidXmlFile(tempDirectory.resolve(REPORT_NAME));
        Match testsuite = report.child("suite");
        Match testcase = testsuite.child("test").child("class").child("test-method");
        Assert.assertEquals(testcase.attr("name", String.class), testMethodName);
        Assert.assertEquals(testcase.child("attachments").children("attachment")
            .matchAttr("filename", "xray.png")
            .matchAttr("src", Paths.get(file).toAbsolutePath().toString())
            .child("rawcontent").text().trim(), contentInBase64);
    }
    
    private void executeTestMethod(Class<?> testClass, String methodName) {
        XMLReporter reporter = new XMLReporter();
        reporter.getConfig().setGenerateTestResultAttributes(true);
        //reporter.getConfig().setOutputDirectory(".");
    
        TestNG testng = new TestNG();
       // testng.setTestClasses(new Class[] { app.getxray.testng.Examples.class, });
       // testng.setTestNames(Arrays.asList(methodName));

        //List<XmlSuite> suites = new ArrayList<XmlSuite>();
        List<XmlClass> classes = new ArrayList<XmlClass>();
        XmlSuite suite = new XmlSuite();
        suite.setName("XrayTests");
        XmlTest test = new XmlTest(suite);
        test.setName("ReportTest");
        XmlClass class1 = new XmlClass(testClass);
        class1.setIncludedMethods(Collections.singletonList(new XmlInclude(methodName)));
        classes.add(class1);
        test.setXmlClasses(classes);  
        testng.setXmlSuites(Collections.singletonList(suite));

        List<java.lang.Class<? extends ITestNGListener>> listeners = new ArrayList<>();
        listeners.add(XrayListener.class);
        testng.setListenerClasses(listeners);
        testng.addListener(reporter);
        testng.setOutputDirectory(tempDirectory.toString());
        testng.run();

    }

}
