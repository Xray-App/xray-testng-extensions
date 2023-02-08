package app.getxray.xray.testng.listeners;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IExecutionListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;

import app.getxray.xray.testng.annotations.Requirement;
import app.getxray.xray.testng.annotations.XrayTest;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.internal.Utils;

import com.beust.jcommander.internal.Sets;

import java.text.SimpleDateFormat;   

public class XrayJsonReporter implements IReporter, IExecutionListener, IInvokedMethodListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(XrayJsonReporter.class);
    private final XrayJsonReporterConfig config = new XrayJsonReporterConfig();
    private static final String DEFAULT_XRAY_PROPERTIES_FILE = "xray.properties";

    private String propertiesFile = DEFAULT_XRAY_PROPERTIES_FILE;

    private long executionsStartedAt;
    private long executionsFinishedAt;



    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        /*
        String methodName = method.getTestMethod().getMethodName();
        System.out.println("getTestResult().getName()     - " + method.getTestResult().getName());
        System.out.println("getTestResult().getTestName() - " + method.getTestResult().getTestName());
        System.out.println("getTestResult().getInstanceName() - " + method.getTestResult().getInstanceName());
        System.out.println("getTestResult().getInstanceName() - " + method.getTestResult().getInstance());
        System.out.println("\n");
        */
    }


    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        // hack: the onExecutionFinish() callback seems to be called but the value is not set; maybe a concurrency issue?
        this.executionsFinishedAt = System.currentTimeMillis();
        JSONObject report = new JSONObject();

        
        Set<ITestResult> testResults = Sets.newLinkedHashSet();
        // temporary structure to hold all test results, including data-driven ones, indexed by test FQN
        HashMap<String, ArrayList<ITestResult>> results = new HashMap<String, ArrayList<ITestResult>>();        
        JSONObject info = new JSONObject();

        try {

            InputStream stream = null;
            if (DEFAULT_XRAY_PROPERTIES_FILE.equals(this.propertiesFile))  {
                stream = getClass().getClassLoader().getResourceAsStream(this.propertiesFile);
            } else {
                stream = new FileInputStream(this.propertiesFile);
            }
            
			// if properties exist, or are enforced from the test, then process them
			if (stream != null) {
				Properties properties = new Properties();
				properties.load(stream);

				String user = properties.getProperty("user");
				if (!emptyString(user)) {
                    this.config.setUser(user);
				}

				String summary = properties.getProperty("summary");
				if (!emptyString(summary)) {
                    this.config.setSummary(summary);
				}
				String description = properties.getProperty("description");
				if (!emptyString(description)) {
                    this.config.setDescription(description);
				}
				String projectKey = properties.getProperty("project_key");
				if (!emptyString(projectKey)) {
                    this.config.setProjectKey(projectKey);
				}
                String version = properties.getProperty("version");
				if (!emptyString(version)) {
                    this.config.setVersion(version);
				}
                String revision = properties.getProperty("revision");
				if (!emptyString(revision)) {
                    this.config.setRevision(revision);  
				}
                String testExecutionKey = properties.getProperty("testexecution_key");
				if (!emptyString(testExecutionKey)) {
                    this.config.setTestExecutionKey(testExecutionKey);  
				}
                String testPlanKey = properties.getProperty("testplan_key");
				if (!emptyString(testPlanKey)) {
                    this.config.setTestPlanKey(testPlanKey);   
				}
                String testEnvironments = properties.getProperty("test_environments");
				if (!emptyString(testEnvironments)) {
					this.config.setTestEnvironments(testEnvironments);
				}
				
                this.config.setXrayCloud(!"false".equals(properties.getProperty("xray_cloud")));  // true, if not specified
                this.config.setUseManualTestsForDatadrivenTests(!"false".equals(properties.getProperty("use_manual_tests_for_datadriven_tests"))); // true, if not specified
                this.config.setUseManualTestsForRegularTests("true".equals(properties.getProperty("use_manual_tests_for_regular_tests"))); // false, if not specified
			}
        } catch (Exception e) {
            LOGGER.error("error loading listener configuration from properties files", e);
            System.err.println(e);
		}

        if (!Utils.isStringEmpty(config.getUser())) {
            info.put("user", config.getUser());  
        }
        if (!Utils.isStringEmpty(config.getSummary())) {
            info.put("summary", config.getSummary());  
        }
        if (!Utils.isStringEmpty(config.getDescription())) {
            info.put("description", config.getDescription());  
        }
        if (!Utils.isStringEmpty(config.getProjectKey())) {
            info.put("project", config.getProjectKey());  
        }
        if (!Utils.isStringEmpty(config.getVersion())) {
            info.put("version", config.getVersion());  
        }
        if (!Utils.isStringEmpty(config.getRevision())) {
            info.put("revision", config.getRevision());  
        }
        if (!Utils.isStringEmpty(config.getTestExecutionKey())) {
            report.put("testExecutionKey", config.getTestExecutionKey());  
        }
        if (!Utils.isStringEmpty(config.getTestPlanKey())) {
            info.put("testPlanKey", config.getTestPlanKey());  
        }
        if (!Utils.isStringEmpty(config.getTestEnvironments())) {
            ArrayList<String> envs = new ArrayList<String>(Arrays.asList(config.getTestEnvironments().split(",")));
            info.put("testEnvironments", envs) ; 
        }

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String startDate = dateFormatter.format(this.executionsStartedAt);
        String finishDate = dateFormatter.format(this.executionsFinishedAt);
        info.put("startDate", startDate);
        info.put("finishDate", finishDate);
        report.put("info", info);


        // add all test results to a temporary testResults set
        JSONArray tests = new JSONArray();
        for (ISuite s : suites) {
            Map<String, ISuiteResult> suiteResults = s.getResults();
            for (ISuiteResult sr : suiteResults.values()) {
                ITestContext testContext = sr.getTestContext();
                addAllTestResults(testResults, testContext.getPassedTests());
                addAllTestResults(testResults, testContext.getFailedTests());
                addAllTestResults(testResults, testContext.getSkippedTests());
            }
        }

        // process testResults, look for multiple results for the same test, and add it hashmap
        for (ITestResult testResult : testResults) {
            String testUid = testResult.getMethod().getQualifiedName();
            LOGGER.info(testUid);
            if (results.containsKey(testUid)) {
                ArrayList<ITestResult> resultsArray = results.get(testUid);
                resultsArray.add(testResult);
                results.put(testUid, resultsArray); 
            } else {
                ArrayList<ITestResult> resultsArray = new ArrayList<ITestResult>();
                resultsArray.add(testResult);
                results.put(testUid, resultsArray);
            }
        }

        for (String testMethod : results.keySet()) {
            addTestResults(tests, results.get(testMethod));
        }
        report.put("tests", tests);
        saveReport(outputDirectory, report);
    }

    private boolean emptyString(String string) {
        return (string == null || string.isEmpty() || string.trim().isEmpty());
    }

    private static List<String> getParameterNames(Method method) {
        Parameter[] parameters = method.getParameters();
        List<String> parameterNames = new ArrayList<>();

        for (Parameter parameter : parameters) {
            if(!parameter.isNamePresent()) {
                throw new IllegalArgumentException("Parameter names are not present!");
            }

            String parameterName = parameter.getName();
            parameterNames.add(parameterName);
        }

        return parameterNames;
    }

    @Override
    public void onExecutionStart() {
      this.executionsStartedAt = System.currentTimeMillis();
    }

    @Override
    public void onExecutionFinish() {
      this.executionsFinishedAt = System.currentTimeMillis();
    }

    private void addTestResults(JSONArray tests, ArrayList<ITestResult> results){
        String xrayTestKey = null;
        String xrayTestSummary = null;
        String xrayTestDescription = null;
        String testDescription = null;

        // auxiliary variable with summary to use, based on a criteria defined ahead
        String testSummary;

        JSONObject test = new JSONObject();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        ITestResult firstResult = results.get(0);
        Method method = firstResult.getMethod().getConstructorOrMethod().getMethod();

        // if no testKey was given, use autoprovision mechanism
        boolean autoprovision =  !method.isAnnotationPresent(XrayTest.class) ||  (method.isAnnotationPresent(XrayTest.class)) && (emptyString(method.getAnnotation(XrayTest.class).key()));
      
        String testKey = "";
        if (method.isAnnotationPresent(XrayTest.class)) {
            testKey = method.getAnnotation(XrayTest.class).key();
            if (!emptyString(testKey))
                test.put("testKey", testKey);
        }

        if (autoprovision) {
            JSONObject testInfo = new JSONObject();
            testInfo.put("projectKey", config.getProjectKey());

            //testInfo.put("summary", firstResult.getMethod().getConstructorOrMethod().getMethod());

            // TODO: description (and other Test issue level custom fields) can't yet be defined for new Test issues
            // add FQN of test method as a comment for easier tracking
            test.put("comment", results.get(0).getMethod().getQualifiedName());

            if (method.isAnnotationPresent(Requirement.class)) {
                String requirementKeys = method.getAnnotation(Requirement.class).key();
                if (!emptyString(requirementKeys)) {
                    ArrayList<String> requirementKeysArray = new ArrayList<String>(Arrays.asList(requirementKeys.split(" ")));
                    testInfo.put("requirementKeys", requirementKeysArray);
                }
            }
            if (method.isAnnotationPresent(XrayTest.class)) {
                if (!emptyString(testKey)) {
                    xrayTestKey = testKey;
                    test.put("testKey", testKey);
                }
                    
                String labels = method.getAnnotation(XrayTest.class).labels();
                if (!emptyString(labels)) {
                    ArrayList<String> labelsArray = new ArrayList<String>(Arrays.asList(labels.split(" ")));
                    testInfo.put("labels", labelsArray);
                }    
                xrayTestSummary = method.getAnnotation(XrayTest.class).summary();
                xrayTestDescription = method.getAnnotation(XrayTest.class).description();
            }
            if  (method.isAnnotationPresent(Test.class)) {
                testDescription = method.getAnnotation(Test.class).description();
            }

            // summary should only be added if no testKey was given
            if (emptyString(xrayTestKey)) {
                // override default Test issue summary using the "summary" attribute from the XrayTest annotation
                // or else, with teh "description" of @XrayTest, or from @Test; else use test name (method name or overriden)
                if (!emptyString(xrayTestSummary)) {
                    testSummary = xrayTestSummary;
                } else if (!emptyString(xrayTestDescription)) {
                    testSummary = xrayTestDescription;
                } else if (!emptyString(testDescription)) {
                    testSummary = testDescription;
                } else {
                    testSummary = firstResult.getName();
                }
                testInfo.put("summary", testSummary);
            }
            
            if ( (results.size() == 1 && config.isUseManualTestsForRegularTests()) ||
                 (results.size() == 1 && results.get(0).getParameters().length > 0) || 
                 ((results.size() > 1 && config.isUseManualTestsForDatadrivenTests()))
                ) {
                testInfo.put("type", "Manual");
                JSONArray steps = new JSONArray();
                JSONObject dummyStep = new  JSONObject();
                dummyStep.put("action", results.get(0).getName());
                dummyStep.put("data", "");
                dummyStep.put("result", "ok");
                steps.add(dummyStep);
                testInfo.put("steps", steps);
            } else {
                testInfo.put("type", "Generic");
                testInfo.put("definition", results.get(0).getMethod().getQualifiedName());
            }
            test.put("testInfo", testInfo);
        }
        // end autoprovision logic

        // just one result.. may be DD/parameterized though
        // if there is only one result for the test and it has no parameters, then create a regular non-datadriven test, i.e. without dataset (this is discussable)
        // maybe create a DD test always?
        // if (firstResult.getParameters().length > or == 0) depending..
        if (results.size() == 1) {

            // regular test; non data-driven
            ITestResult result =  results.get(0);           
            String start = dateFormatter.format(result.getStartMillis());
            String finish = dateFormatter.format(result.getEndMillis());
            test.put("start", start);
            test.put("finish", finish);
            test.put("status", getTestStatus(result.getStatus()));
            if (result.getStatus() == ITestResult.FAILURE)
                test.put("comment", result.getThrowable().getMessage());

            // process attachments
            processAttachments(result, test);
        } else {
            // mutiple results => data-driven test

            // TODO: this should be based not on the first result but on the start&endtime of all iterations
            String start = dateFormatter.format(firstResult.getStartMillis());
            String finish = dateFormatter.format(firstResult.getEndMillis());
            test.put("start", start);
            test.put("finish", finish);

            JSONArray iterations = new JSONArray();
            int counter = 1;
            int totalPassed = 0;
            int totalFailed = 0;
            int totalSkipped = 0;
            for (ITestResult result: results) {
                JSONObject iteration = new JSONObject();
                iteration.put("name", "iteration " + counter++);
                Object[] params = result.getParameters();
                if (params.length > 0) {
                    JSONArray parameters = new JSONArray();
                    int count = 1;

                    List<String> parameterNames = null;
                    try {
                        parameterNames = getParameterNames(result.getMethod().getConstructorOrMethod().getMethod());
                    } catch (Exception ex ){

                    }                   
                    for (Object param: params) {
                        JSONObject p = new JSONObject();
                        String paramName = "param" + count;
                        if (parameterNames != null)
                            paramName = parameterNames.get(count-1);
                        p.put("name", paramName);
                        p.put("value", param.toString());
                        parameters.add(p);
                        count++;
                    }
                    iteration.put("parameters", parameters);
                }

                JSONArray steps = new JSONArray();
                JSONObject dummyStep = new  JSONObject();
                String actualResult  = "";
                if (result.getStatus() == ITestResult.FAILURE) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    result.getThrowable().printStackTrace(pw);
                    actualResult = sw.toString();
                }

                /*
                [ERROR] givenNumberFromDataProvider_ifEvenCheckOK_thenCorrect[5, true](com.baeldung.ParametrizedLongRunningUnitTest)  Time elapsed: 0.009 s  <<< FAILURE!
                java.lang.AssertionError: expected [false] but found [true]
	                at com.baeldung.ParametrizedLongRunningUnitTest.givenNumberFromDataProvider_ifEvenCheckOK_thenCorrect(ParametrizedLongRunningUnitTest.java:34)

                 * result.getThrowable().getMessage():
                 *   expected [false] but found [true]
                 * result.getThrowable().toString():
                 *   java.lang.AssertionError: expected [false] but found [true]
                 * result.getThrowable().getLocalizedMessage():
                 *   expected [false] but found [true]
                 * result.getThrowable().printStackTrace(pw):
                 *    java.lang.AssertionError: expected [false] but found [true]
	             *      at com.baeldung.ParametrizedLongRunningUnitTest.givenNumberFromDataProvider
                 *      ... (full trace)...
                 */
                dummyStep.put("actualResult", actualResult);
                dummyStep.put("status", getTestStatus(result.getStatus()));


                // attachments
                processAttachments(result, dummyStep);

                steps.add(dummyStep);
                iteration.put("steps", steps);

                iteration.put("status", getTestStatus(result.getStatus()));
                iterations.add(iteration);

                if (result.getStatus() == ITestResult.SUCCESS)
                    totalPassed++;
                if (result.getStatus() == ITestResult.FAILURE)
                    totalFailed++;
                if (result.getStatus() == ITestResult.SKIP)
                    totalSkipped++;            
            }
            test.put("iterations", iterations);
            if (totalFailed > 0)
             test.put("status", getTestStatus(ITestResult.FAILURE));
            else if (totalPassed == iterations.size())
                test.put("status", getTestStatus(ITestResult.SUCCESS));
            else
                test.put("status", getTestStatus(ITestResult.SKIP));
        }
        tests.add(test);
    }

    private void processAttachments(ITestResult result, JSONObject targetObject) {
        JSONArray evidence = new JSONArray();
        Base64.Encoder enc= Base64.getEncoder();
        File[] attachments = (File[])result.getAttribute("attachments");
        if (attachments != null){
            for (File file : attachments)  {
                try {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    byte[] encoded = enc.encode(fileContent);
                    String encodedStr = new String(encoded,"UTF-8");
          
                    JSONObject tmpAttach = new JSONObject();
                    tmpAttach.put("data", encodedStr);
                    tmpAttach.put("filename", file.getName());
                    tmpAttach.put("contentType", getContentTypeFor(file));
                    evidence.add(tmpAttach);
                  } catch (Exception ex) {
                    LOGGER.error("problem processing attachment " + file.getAbsolutePath() + ": " + ex);
                  }
            }
            targetObject.put("evidence", evidence);
        }
    }

/*
    private boolean annotationPresent(IInvokedMethod method, Class clazz) {
        boolean retVal = method.getTestMethod().getConstructorOrMethod().getMethod().isAnnotationPresent(clazz) ? true : false;
        return retVal;
    }
*/
    
    private String getTestStatus(int status){
        boolean xrayCloud = config.isXrayCloud();
        switch (status) {
            case ITestResult.FAILURE:
                return xrayCloud ? "FAILED": "FAIL";

            case ITestResult.SUCCESS:
            case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
                return xrayCloud ? "PASSED": "PASS";

            case ITestResult.SKIP:
                return xrayCloud ? "SKIPPED": "SKIP";

            default:
                return "EXECUTING";
            }    
    }

    private void addAllTestResults(Set<ITestResult> testResults, IResultMap resultMap) {
        if (resultMap != null) {
          // Sort the results chronologically before adding them
          testResults.addAll(
              resultMap.getAllResults().stream()
                  .sorted((o1, o2) -> (int) (o1.getStartMillis() - o2.getStartMillis()))
                  .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
    }


    private void saveReport(String outputDirectory, JSONObject report) {
        new File(outputDirectory).mkdirs();
        PrintWriter reportWriter = null;
        try {
            reportWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputDirectory, config.getReportFilename()))));
            reportWriter.println(report.toJSONString());
        } catch (IOException e) {
            LOGGER.error("Problem saving report", e);
        } finally {
            try {
                reportWriter.flush();
                reportWriter.close();
            } catch (Exception e) {
                LOGGER.error("Problem closing report file", e);
            }
        }
    }

    public void usePropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    private String getContentTypeFor(File file) {
        String filename = file.getName();
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        switch (extension) {
            case "png":
                return "image/png";
            case "jpeg":
            case "jpg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "txt":
            case "log":
                return "text/plain";
            case "zip":
                return "application/zip";
            case "json":
                return "application/json";
            default:
                return "application/octet-stream";
            }
    }


    @Override
    public XrayJsonReporterConfig getConfig() {
      return config;
    }

}