package app.getxray.xray.testng.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SpecVersionDetector;
import com.networknt.schema.ValidationMessage;

// import com.google.gson.JsonParser;

import org.joox.Match;
import org.json.JSONObject;
import org.json.JSONArray;
import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;

import app.getxray.xray.testng.listeners.XrayJsonReporter;
import org.testng.ITestNGListener;


public class XrayJsonReportTests {

    private Path tempDirectory;
    private static final String REPORT_NAME = "xray-report.json";
    private static final Class BASIC_EXAMPLES_CLASS = BasicExamples.class;
    private static final Class DATADRIVEN_EXAMPLES_CLASS = DataDrivenExamples.class;
    private ObjectMapper mapper = new ObjectMapper();
  
    @BeforeSuite
    public void setUp() throws Exception {
        tempDirectory = Files.createTempDirectory("testng-xrayjson");
    }

    @AfterSuite
    public void tearDown() throws Exception {
    }

    @Test
    public void legacyTestsShouldBeMappedToATest() throws Exception {
        String testMethodName = "legacyTest";
        executeTestMethod(BASIC_EXAMPLES_CLASS, testMethodName);
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));

        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("summary", testMethodName);
        expectedTestInfo.put("type", "Generic");

        //JSONAssert.assertEquals("{summary:\"legacyTest\"}", actual, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldMapXrayTestSummaryToTestIssueSummary() throws Exception {
        String testMethodName = "annotatedWithXrayTestSummary";
        executeTestMethod(BASIC_EXAMPLES_CLASS, testMethodName);

        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));

        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("summary", "custom summary");
        expectedTestInfo.put("type", "Generic");

        //JSONAssert.assertEquals("{summary:\"legacyTest\"}", actual, JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldMapXrayTestDescriptionToTestIssueSummary() throws Exception {
        String testMethodName = "annotatedWithXrayTestDescription";
        executeTestMethod(BASIC_EXAMPLES_CLASS, testMethodName);
     
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));

        
        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        Assert.assertFalse(actualTest.has("testKey"));
        
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("summary", "custom description");
        // expectedTestInfo.put("type", "Manual");

        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldMapTestDescriptionToTestIssueSummary() throws Exception {
        String testMethodName = "annotatedWithTestDescription";
        executeTestMethod(BASIC_EXAMPLES_CLASS, testMethodName);
     
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));

        
        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        Assert.assertFalse(actualTest.has("testKey"));
        
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("summary", "custom test level description");
        // expectedTestInfo.put("type", "Manual");

        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldMapXrayTestKeyToTestIssueKey() throws Exception {
        String testMethodName = "annotatedWithXrayTestKey";
        executeTestMethod(BASIC_EXAMPLES_CLASS, testMethodName);
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));

        
        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        Assert.assertFalse(actualTest.has("testInfo"));
        
        JSONObject expectedTest = new JSONObject();
        expectedTest.put("testKey", "CALC-2000");
        expectedTest.put("status", "PASSED");

        JSONAssert.assertEquals(expectedTest, actualTest, JSONCompareMode.LENIENT);
    }


    @Test
    public void shouldProcessTestWithParametersAnnotationAsManual() throws Exception {
        String testMethodName = "givenNumberFromXML_ifEvenCheckOK_thenCorrect";
        executeTestMethod(DATADRIVEN_EXAMPLES_CLASS, testMethodName);
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));

        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("summary", testMethodName);
        expectedTestInfo.put("type", "Manual");

        String expectedSteps = "[{\"action\": \"" + testMethodName + "\", \"data\": \"\", \"result\": \"ok\"}]";
        String actualSteps =  actualTestInfo.get("steps").toString();
       // Assert.assertEquals(actualJsonStr, expectedJsonStr);
        JSONAssert.assertEquals(expectedSteps, actualSteps, JSONCompareMode.LENIENT);
        

        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldProcessTestWithDataProvider() throws Exception {
        String testMethodName = "givenNumberFromDataProvider_ifEvenCheckOK_thenCorrect";
        executeTestMethod(DATADRIVEN_EXAMPLES_CLASS, testMethodName);
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));

        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        Assert.assertEquals(actualTest.getString("status"), "FAILED");
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("summary", testMethodName);
        expectedTestInfo.put("type", "Manual");
        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);

        Assert.assertTrue(actualTest.has("iterations"));
        JSONArray iterations = (JSONArray)actualTest.getJSONArray("iterations");
        Assert.assertEquals(iterations.length(), 4);

        String expectedIteration;
        String actualIteration;
        expectedIteration = "{  \"name\": \"iteration 1\", \"parameters\":[{\"name\":\"number\",\"value\":\"1\"},{\"name\":\"expected\",\"value\":\"false\"}], \"steps\": [ {\"evidence\": [], \"actualResult\": \"\", \"status\": \"PASSED\"} ], \"status\": \"PASSED\"}";
        actualIteration =  iterations.get(0).toString();
        //Assert.assertEquals(actualIteration, expectedIteration);
        JSONAssert.assertEquals(expectedIteration, actualIteration, JSONCompareMode.LENIENT);

        expectedIteration = "{  \"name\": \"iteration 2\", \"parameters\":[{\"name\":\"number\",\"value\":\"2\"},{\"name\":\"expected\",\"value\":\"true\"}], \"steps\": [ {\"evidence\": [], \"actualResult\": \"\", \"status\": \"PASSED\"} ], \"status\": \"PASSED\"}";
        actualIteration =  iterations.get(1).toString();
        JSONAssert.assertEquals(expectedIteration, actualIteration, JSONCompareMode.LENIENT);

        expectedIteration = "{  \"name\": \"iteration 3\", \"parameters\":[{\"name\":\"number\",\"value\":\"4\"},{\"name\":\"expected\",\"value\":\"true\"}], \"steps\": [ {\"evidence\": [], \"actualResult\": \"\", \"status\": \"PASSED\"} ], \"status\": \"PASSED\"}";
        actualIteration =  iterations.get(2).toString();
        JSONAssert.assertEquals(expectedIteration, actualIteration, JSONCompareMode.LENIENT);

        expectedIteration = "{  \"name\": \"iteration 4\", \"parameters\":[{\"name\":\"number\",\"value\":\"5\"},{\"name\":\"expected\",\"value\":\"true\"}], \"steps\": [ {\"evidence\": [], \"status\": \"FAILED\"} ], \"status\": \"FAILED\"}";
        actualIteration =  iterations.get(3).toString();
        //Assert.assertEquals(actualIteration, expectedIteration);
        JSONAssert.assertEquals(expectedIteration, actualIteration, JSONCompareMode.LENIENT);

        
        // compare the iterations array with the expected
        /*
         * http://jsonassert.skyscreamer.org/apidocs/org/skyscreamer/jsonassert/ArrayValueMatcher.html
         * https://stackoverflow.com/questions/50919776/ignore-specific-node-within-array-when-comparing-two-json-in-java
         * https://www.tabnine.com/code/java/classes/org.skyscreamer.jsonassert.JSONCompareResult
         * http://jsonassert.skyscreamer.org/javadoc/org/skyscreamer/jsonassert/JSONCompare.html
         * 
         */
/*
        String expectedJsonStr = "{\"iterations\": [{\"id\": \"valueA\"}, {\"colour\": \"Blue\"}]}";
        String actualJsonStr = "{\"anArray\": [{\"id\": \"valueB\"}, {\"colour\": \"Blue\"}]}";

        //Create custom comparator which compares two json strings but ignores reporting any differences for anArray[n].id values
        //as they are a tolerated difference
        // https://stackoverflow.com/questions/50919776/ignore-specific-node-within-array-when-comparing-two-json-in-java
        ArrayValueMatcher<Object> arrValMatch = new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE,
                new Customization("iterations[*].id", (o1, o2) -> true)));

        Customization arrayValueMatchCustomization = new Customization("iterations", arrValMatch);
        CustomComparator customArrayValueComparator = new CustomComparator(
                JSONCompareMode.NON_EXTENSIBLE, 
                arrayValueMatchCustomization);
        JSONAssert.assertEquals(expectedJsonStr, iterations.toString(), customArrayValueComparator);
*/


    }

    

    @Test
    public void shouldMapXrayRequirementKeyToTestcaseAttribute() throws Exception {
        String testMethodName = "annotatedWithRequirementKey";
        executeTestMethod(BASIC_EXAMPLES_CLASS, testMethodName);
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));

        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        Assert.assertTrue(actualTest.has("testInfo"));
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("summary", testMethodName);
        expectedTestInfo.put("type", "Generic");
        String[] requirementKeys = { "CALC-1234" };
        expectedTestInfo.put("requirementKeys", new JSONArray(requirementKeys));  
        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);   

        JSONObject expectedTest = new JSONObject();
        expectedTest.put("status", "PASSED");
        JSONAssert.assertEquals(expectedTest, actualTest, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldMapXraySpaceDelimitedLabelsToTestcaseAttribute() throws Exception {
        String testMethodName = "annotatedWithXrayTestLabels";
        executeTestMethod(BASIC_EXAMPLES_CLASS, testMethodName);
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));

        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        Assert.assertTrue(actualTest.has("testInfo"));
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("summary", testMethodName);
        expectedTestInfo.put("type", "Generic");
        String[] labels = { "core", "addition" };
        expectedTestInfo.put("labels", new JSONArray(labels));  
        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);   

        JSONObject expectedTest = new JSONObject();
        expectedTest.put("status", "PASSED");
        JSONAssert.assertEquals(expectedTest, actualTest, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldStoreTestRunEvidenceToTestcaseProperty() throws Exception {
        String testMethodName = "testWithAttachment";
        executeTestMethod(BASIC_EXAMPLES_CLASS, testMethodName);
        
        String file = "src/test/java/app/getxray/xray/testng/tests/xray.png";
        Base64.Encoder enc = Base64.getEncoder();
        byte[] fileContent = Files.readAllBytes(Paths.get(file));
        byte[] encoded = enc.encode(fileContent);
        String contentInBase64 = new String(encoded, "UTF-8");
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));
        validateXrayCloudJsonSchema(tempDirectory.resolve(REPORT_NAME).toAbsolutePath().toString());

        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        JSONArray actualEvidence = (JSONArray)actualTest.getJSONArray("evidence");
        Assert.assertEquals(1, actualEvidence.length());
        JSONObject actualAttachment = (JSONObject)actualEvidence.get(0);
        JSONObject expectedAttachment = new JSONObject();
        expectedAttachment.put("filename", "xray.png");
        expectedAttachment.put("contentType", "image/png");
        expectedAttachment.put("data", contentInBase64);
        JSONAssert.assertEquals(expectedAttachment, actualAttachment, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldUseProjectKeyFromPropertiesForAutoprovisionedTests() throws Exception {
        String customProperties = "project_key=CALC\n";
        Path customPropertiesFile = Files.createTempFile("xray", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());

        String testMethodName = "legacyTest";
        executeTestMethodWithCustomProperties(BASIC_EXAMPLES_CLASS, testMethodName, customPropertiesFile.toAbsolutePath().toString());
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));
        validateXrayCloudJsonSchema(tempDirectory.resolve(REPORT_NAME).toAbsolutePath().toString());

        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("projectKey", "CALC");
        expectedTestInfo.put("summary", "legacyTest");
        expectedTestInfo.put("type", "Generic");

        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldReadSummaryFromProperties() throws Exception {
        String customProperties = "summary=results of automated tests\n";
        Path customPropertiesFile = Files.createTempFile("xray", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());

        String testMethodName = "legacyTest";
        executeTestMethodWithCustomProperties(BASIC_EXAMPLES_CLASS, testMethodName, customPropertiesFile.toAbsolutePath().toString());
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));
        validateXrayCloudJsonSchema(tempDirectory.resolve(REPORT_NAME).toAbsolutePath().toString());

        /*
        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        JSONAssert.assertEquals(expectedTest, actualTest, JSONCompareMode.LENIENT);
        */

        JSONObject actualInfo = report.getJSONObject("info");
        JSONObject expectedInfo = new JSONObject();
        expectedInfo.put("summary", "results of automated tests");
        JSONAssert.assertEquals(expectedInfo, actualInfo, JSONCompareMode.LENIENT);

        /*
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("projectKey", "CALC");
        expectedTestInfo.put("summary", "legacyTest");
        expectedTestInfo.put("type", "Manual");
        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);

        JSONObject expectedTest = new JSONObject();
        expectedTest.put("summary", "CALC");
        Assert.assertEquals(report.get("project"), "CALC");   
        */
    }


    @Test
    public void shouldReadDescriptionFromProperties() throws Exception {
        String customProperties = "description=results of automated tests\n";
        Path customPropertiesFile = Files.createTempFile("xray", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());

        String testMethodName = "legacyTest";
        executeTestMethodWithCustomProperties(BASIC_EXAMPLES_CLASS, testMethodName, customPropertiesFile.toAbsolutePath().toString());
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));
        validateXrayCloudJsonSchema(tempDirectory.resolve(REPORT_NAME).toAbsolutePath().toString());

        JSONObject actualInfo = report.getJSONObject("info");
        JSONObject expectedInfo = new JSONObject();
        expectedInfo.put("description", "results of automated tests");
        JSONAssert.assertEquals(expectedInfo, actualInfo, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldReadTestplanKeyFromProperties() throws Exception {
        String customProperties = "testplan_key=CALC-1200\n";
        Path customPropertiesFile = Files.createTempFile("xray", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());

        String testMethodName = "legacyTest";
        executeTestMethodWithCustomProperties(BASIC_EXAMPLES_CLASS, testMethodName, customPropertiesFile.toAbsolutePath().toString());
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));
        validateXrayCloudJsonSchema(tempDirectory.resolve(REPORT_NAME).toAbsolutePath().toString());

        JSONObject actualInfo = report.getJSONObject("info");
        JSONObject expectedInfo = new JSONObject();
        expectedInfo.put("testPlanKey", "CALC-1200");
        JSONAssert.assertEquals(expectedInfo, actualInfo, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldReadTestExecutionKeyFromProperties() throws Exception {
        String customProperties = "testexecution_key=CALC-1200\n";
        Path customPropertiesFile = Files.createTempFile("xray", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());

        String testMethodName = "legacyTest";
        executeTestMethodWithCustomProperties(BASIC_EXAMPLES_CLASS, testMethodName, customPropertiesFile.toAbsolutePath().toString());
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));
        validateXrayCloudJsonSchema(tempDirectory.resolve(REPORT_NAME).toAbsolutePath().toString());

        JSONObject expectedReport = new JSONObject();
        expectedReport.put("testExecutionKey", "CALC-1200");
        JSONAssert.assertEquals(expectedReport, report, JSONCompareMode.LENIENT);
    }

    @Test
    public void shouldReadTestEnvironmentsFromProperties() throws Exception {
        String customProperties = "test_environments=chrome\n";
        Path customPropertiesFile = Files.createTempFile("xray", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());

        String testMethodName = "legacyTest";
        executeTestMethodWithCustomProperties(BASIC_EXAMPLES_CLASS, testMethodName, customPropertiesFile.toAbsolutePath().toString());
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));
        validateXrayCloudJsonSchema(tempDirectory.resolve(REPORT_NAME).toAbsolutePath().toString());

        JSONObject actualInfo = report.getJSONObject("info");
        JSONArray actualEnvs = (JSONArray)actualInfo.getJSONArray("testEnvironments");
        Assert.assertEquals(actualEnvs.length(), 1);
        Assert.assertEquals(actualEnvs.getString(0), "chrome");
    }


    @Test
    public void shouldMapRegularTestToAManualTest() throws Exception {
        String customProperties = "use_manual_tests_for_regular_tests=true\n";
        Path customPropertiesFile = Files.createTempFile("xray", ".properties");
        Files.write(customPropertiesFile, customProperties.getBytes());

        String testMethodName = "legacyTest";
        executeTestMethodWithCustomProperties(BASIC_EXAMPLES_CLASS, testMethodName, customPropertiesFile.toAbsolutePath().toString());
        
        JSONObject report = readJsonFile(tempDirectory.resolve(REPORT_NAME));
        validateXrayCloudJsonSchema(tempDirectory.resolve(REPORT_NAME).toAbsolutePath().toString());

        JSONArray actualTests = (JSONArray)report.getJSONArray("tests");
        Assert.assertEquals(1, actualTests.length());
        JSONObject actualTest = (JSONObject)(report.getJSONArray("tests")).get(0);
        JSONObject actualTestInfo = actualTest.getJSONObject("testInfo");
        JSONObject expectedTestInfo = new JSONObject();
        expectedTestInfo.put("summary", testMethodName);
        expectedTestInfo.put("type", "Manual");

        JSONAssert.assertEquals(expectedTestInfo, actualTestInfo, JSONCompareMode.LENIENT);
    }

    private JSONObject readJsonFile(Path jsonFile) throws Exception {
        Assert.assertTrue(Files.exists(jsonFile), "File does not exist: " + jsonFile);
        return new JSONObject(Files.readString(jsonFile));
    }

    private void executeTestMethod(Class<?> testClass, String methodName) {
        executeTestMethodWithCustomProperties(testClass, methodName, "xray.properties");
    }

    private void executeTestMethodWithCustomProperties(Class<?> testClass, String methodName, String propertiesFile) {
        TestNG testng = new TestNG();
        //List<XmlSuite> suites = new ArrayList<XmlSuite>();
        List<XmlClass> classes = new ArrayList<XmlClass>();
        XmlSuite suite = new XmlSuite();
        suite.setName("XrayTests");
        XmlTest test = new XmlTest(suite);
        test.setName("ReportTest");
            
        Map<String, String> parameters = new HashMap<>();
        parameters.put("value", "1");
        parameters.put("isEven", "false");
        test.setParameters(parameters);

        XmlClass class1 = new XmlClass(testClass);
        class1.setIncludedMethods(Collections.singletonList(new XmlInclude(methodName)));
        
        classes.add(class1);
        test.setXmlClasses(classes);  
        testng.setXmlSuites(Collections.singletonList(suite));
        

        List<java.lang.Class<? extends ITestNGListener>> listeners = new ArrayList<>();
        //listeners.add(XrayJsonReporter.class);
        testng.setListenerClasses(listeners);
        XrayJsonReporter reporter = new XrayJsonReporter();
        reporter.usePropertiesFile(propertiesFile);
        testng.addListener(reporter);
        testng.setOutputDirectory(tempDirectory.toString());
        testng.setDataProviderThreadCount(1);

        testng.run();
    }

    private static InputStream inputStreamFromClasspath(String path) {
        return XrayJsonReportTests.class.getResourceAsStream(path);
    }

    private void validateXrayCloudJsonSchema(String filename) throws Exception {
        /*
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        // JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(jsonSchema));
        System.out.println("1############################################################"); 
        JsonSchema jsonSchema = factory.getSchema( 
        XrayJsonReportTests.class.getResourceAsStream("/xray_cloud_json_schema.json"));
        System.out.println("2############################################################"); 
        JsonNode jsonNode = mapper.readTree( XrayJsonReportTests.class.getResourceAsStream(filename)); 
        System.out.println("3############################################################");
        Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);
        System.out.println(errors.toString());
        while(errors.iterator().hasNext()) {
            System.out.println(errors.iterator().next().toString());
         }
         */

         ObjectMapper objectMapper = new ObjectMapper();
         try (InputStream jsonStream = new FileInputStream(filename)) {
             JsonNode json = objectMapper.readTree(jsonStream);
             JsonSchemaFactory validatorFactory;
             try (InputStream schemaStream = inputStreamFromClasspath("/xray_cloud_json_schema.json")) {
                 JsonNode jsonSchema = objectMapper.readTree(schemaStream);
                 validatorFactory = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(jsonSchema));
             }
     

             final StringBuilder errorMessages = new StringBuilder();
             try (InputStream schemaStream = inputStreamFromClasspath("/xray_cloud_json_schema.json")) {
                 JsonSchema schema = validatorFactory.getSchema(schemaStream);
                 Set<ValidationMessage> validationResult = schema.validate(json);

                 if (validationResult.isEmpty()) {
                     // System.out.println("no Xray JSON schema validation errors :-)");
                 } else {
                     validationResult.forEach(vm -> errorMessages.append(vm.getMessage() + "\n"));
                 }
                 Assert.assertTrue(validationResult.isEmpty(), errorMessages.toString());
             }
         }

    }

}
