# xray-testng-extensions

[![build workflow](https://github.com/Xray-App/xray-testng-extensions/actions/workflows/maven.yml/badge.svg)](https://github.com/Xray-App/xray-testng-extensions/actions/workflows/maven.yml)
![code coverage](
https://raw.githubusercontent.com/Xray-App/xray-testng-extensions/master/.github/badges/jacoco.svg)
[![license](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/Xray-App/community)

This repo contains several enhancements to TestNG to allow you to take better advantage of [TestNG](https://testng.org/) whenever using it together with [Xray Test Management](https://getxray.app). This includes specific annotations and listener(s) that can then process their information.

This code is provided as-is; you're free to use it and modify it at your will (see license ahead).

This is a preliminary release so it is subject to changes, at any time.

## Overview

Results from automated scripts implemented as `@Test` methods can be tracked in test management tools to provide insights about quality aspects targeted by those scripts and their impacts.
Therefore, it's important to attach some relevant information during the execution of the tests, so it can be shared and analyzed later on in the test management tool (e.g. [Xray Test Management](https://getxray.app)).

The idea is to be able to produce a report containing additional information that Xray can take advantage of.
This way, testers can automate the test script and at the same time provide information such as the covered requirement, right from the test automation code. Additional information may be provided, usually through new annotations.

This project supports 2 different ways of achieving the previous goal:

- using a custom TestNG XML report (the initial output of this project)
- using a Xray JSON report (beta)

### Features

- link a test method to an existing Test issue or use auto-provisioning
- cover a "requirement" (i.e. an issue in Jira) from a test method
- specify additional fields for the auto-provisioned Test issues (e.g. summary, description, labels)
- attach screenshots or any other file as evidence to the Test Run, right from within the test method (pending Xray implementation on the TestNG XML handling; supported with Xray JSON reports)
- generate evolved TestNG XML reports that Xray is able to process
- (beta) generate a report in Xray JSON format

| feature | enhanced TestNG XML | Xray JSON |
| --- | --- | --- |
| autoprovision "generic" Tests | x | x |
| autoprovision "manual" Tests | x | x (with a dummy step)|
| report against an existing "generic" Test using @XrayTest annotation | x | x |
| report against an existing "manual" Test using @XrayTest annotation | x | x |
| report against an existing "manual" Test step | - | - |
| data-driven for manual tests (data driven from the code) | - | x |
| data-driven for manual tests (data driven from dataset In Xray) | - | - |
| link test to requirement(s) | x (during autoprovision) | x (only during autoprovision?)|
| attach evidence globally at testrun level | x (unsupported yet by Xray) | x |
| attach evidence for each data-driven iteration | - | x (only for "manual" Tests)|
| exception/error message | x | x |

### Main classes

The project consists of:

- **@XrayTest**, **@Requirement**: new, optional annotations to provide additional information whenever writing the automated test methods; these are independent of the output report format you may choose
- **XrayListener**: a listener for test events that can process the new annotations, and add them to the test results and attributes on the test results; only revelant if you wish to generate enhanced TestNG XML reports

Besides, there is also:
- **XrayJsonReporter**: a reporter that generates test results in Xray JSON format (beta)
- **XrayReportListener**: an *optional* reporter, that extends the built-in XmlReporter from TestNG and adds the capability of adding additional information (e.g., attachments) on the test results; it's not much useful/needed so far, thus you may ignore it

To generate enhanced TestNG XML reports, as seen ahead, no special reporter needs to be used; just the default XMLReporter.

## Installing

These extensions are available as an artifact available on (Maven) Central Repository, which is configured by default in your Maven instalation.

Add the following dependency to your pom.xml:

```xml
        <dependency>
          <groupId>app.getxray</groupId>
          <artifactId>xray-testng-extensions</artifactId>
          <version>0.2.0-beta</version>
          <scope>test</scope>
        </dependency>
```

### Configuration

Depending on the report format you aim to use to store the test results (i.e. enhanced TestNG XML or Xray JSON), configurations are slightly different.

#### TestNG XML report

To generate test reports in TestNG XML format there is no necessary configuration to be made as that is the default reporter (XMLReporter) that TestNG uses.

For the time being, there are *no* additional configuration properties to change the behaviour related with the generation of this type of reports.

So, what do we need to do?

- enable the *XrayListener* listener, to process the new annotations
- configure TestNG's XMLReporter (i.e., the built-in TestNG XML reporter) to include user-defined attributes on the report, on Maven's `pom.xml` or on Gradle

That's it.

##### 1. Enable the listener(s)

In order to embed additional information on the customized TestNG XML repor,t we need to register the **XrayListener** listener. This can be done in [several ways](https://testng.org/doc/documentation-main.html#testng-listeners):

- it can be discovered automatically at runtime by the ServiceLoader based on the contents of a file (e.g `src/test/resources/META-INF/services/org.testng.ITestNGListener`); this is probably the easiest one.

```bash
app.getxray.xray.testng.listeners.XrayListener
```

- or by specifying it on `the testng.xml` file

```xml
<suite>
 
  <listeners>
    <listener class-name="app.getxray.xray.testng.listeners.XrayListener" />
  </listeners>
 
...
```

- or by annotating the test class

```java
@Listeners({ app.getxray.xray.testng.listeners.XrayListener.class })
public class MyTest {
  // ...
}
```

- or programmaticaly
- or from the command linex

Registering the listener is mandatory.
In order to take advantage of the capabilities of this new listener, the new annotations can be used.

Note: there's another listener _XrayReportListener_ but you don't need to use it, as it is only required to further customize the TestNG XML report (e.g., for adding attachments) => this is not yet supported by Xray.

##### 2a. Configure Maven to use TestNG XMLReporter and enable user-defined test attributes

For Maven, we can configure the surefire plugin and define a property `reporter` to enable the user-defined test attributes on the XML report that will contain the values we provided using the new annotations.

```xml
...
    <build>
        <plugins>
            <plugin>

                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.20.1</version>

                <configuration>
                    <testFailureIgnore>true</testFailureIgnore>

                    <suiteXmlFiles>
                      <suiteXmlFile>testng.xml</suiteXmlFile>
                    </suiteXmlFiles>

                    <properties>
                        <property>
                            <name>reporter</name>
                            <value>org.testng.reporters.XMLReporter:generateTestResultAttributes=true</value>
                        </property>
                    </properties>

                </configuration>
             </plugin>

        ...
        </plugins>
    </build>
```

##### 2b. Configure Gradle to use TestNG XMLReporter and enable user-defined test attributes

For Gradle, we need to use a custom task, as the standard `test` task doesn't allow us to set attributes on the XMLReporter used by TestNG.

As an example, we could create a `testngTest` task that runs TestNG tests and enabled the intended behavior on the XMLReporter.

```gradle
task testngTest(type: JavaExec, dependsOn: [classes]) {
    group 'Verification'
    description 'Run TestNG tests'
    mainClass = 'org.testng.TestNG'
    args('testng.xml', '-reporter',
            'org.testng.reporters.XMLReporter:generateTestResultAttributes=true,generateGroupsAttribute=true'
    )
    classpath = sourceSets.test.runtimeClasspath
}
```

### Xray JSON report (beta)

Note: this reporter is in early stage and is subject to changes. The set of features provided by this format is more comprehensive than the information you can embed in TestNG XML reports, even considering the enhanced TestNG XML with additional attributes.

To generate repors in Xray JSON format, we need to:

- enable the *XrayJsonReporter* listener
- define some properties either in a `xray.properties` resource file or by passing them as parameters to the reporter (e.g., in the `surefire` configuration)
- if you aim to use data-driven tests and have proper visibility of them on Xray, configure the build tool (e.g., Maven) to include information about the name of the arguments of the called test methods

To activate the listener (see previous section for all possible ways of doing so).
As an example, you can activate it on the suite configuration file (e.g., `testng.xml`):

```xml
<suite>
 
  <listeners>
    <listener class-name="app.getxray.xray.testng.listeners.XrayJsonReporter" />
  </listeners>
 
...
```

This reporter must be configured as there are mandatory parameters that need to be explicitly defined. This can be done using properties defined in the surefire configuration in the `pom.xml` or by using a properties file.

To define the properties in `pom.xml` file, configure surefire plugin by adding a property named `reporter` having the value `app.getxray.xray.testng.listeners.XrayJsonReporter:`, followed by pair of parameter/values such as `param1=val1,...,paramN=valN`.

```xml
    <build>
        <plugins>

            <plugin>

                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.22.2</version>

                <configuration>
                    <testFailureIgnore>true</testFailureIgnore>

                    <suiteXmlFiles>
                      <suiteXmlFile>testng.xml</suiteXmlFile>
                    </suiteXmlFiles>

                    <properties>
                        <property>
                            <name>reporter</name>
                            <value>app.getxray.xray.testng.listeners.XrayJsonReporter:xrayCloud=true,projectKey=XT,testPlanKey=XT-616</value>
                        </property>
                </properties>

                </configuration>
             </plugin>

        </plugins>
    </build>
```

The configuration variables are similar to the ones below, but in camel case style.

If you choose to use a properties file, it should be named `src/test/resources/xray.properties`, where you can define some settings.

- `user`: userid for the Jira user who executed the tests
- `summary`: summary of the Test Execution issue that will be created
- `description`: description of the Test Execution issue that will be created
- `project_key`: Jira's project key where Test Execution will be created (and eventually the Tests, if autoprovisioned) 
- `version`: (optional) fixVersion (i.e., version on the corresponding Jira project) to assign the Test Execution and its results
- `revision`: (optional) revision to assign the Test Execution; this can be
- `testexecution_key`: (optional) isue key of the Test Execution if you wish to overwrite/reuse an existing one; otherwise, a new Test Execution issue will be created
- `testplan_key`: (optional) issue key of the Test Plan to link the corresponding Test Execution to
- `test_environments`: (optional) Test Environment(s) to assign to the Test Execution

- `report_filename`: the name of the report. Default is "xray-report.json"
- `use_manual_tests_for_regular_tests`: Use "Manual" tests as an abstraction of regular, non-datadriven, tests. Default is false (i.e., "Generic" tests will be created).
- `use_manual_tests_for_datadriven_tests`: Use "Manual" tests as an abstraction of DD tests during autoprovisioning. Default is true (for the time being, only these are supported)

Example:

```bash
user=xpto
project_key=CALC
summary=test automation results
description=selenium test results
version=1.0
revision=123
testplan_key=CALC-1000
test_environments=chrome
report_filename=xray-report.json
```

## How to use

### Using new annotations

Two new annotations (`@XrayTest`, `@Requirement`) can be used.
The annotations are optional and cannot be use more than once per test method.

#### @XrayTest

Test methods don't need to be annotated with `@XrayTest` unless you want to take advantage of the following enhancements.

You may use the **@XrayTest** annotation to:

- enforce mapping of result to specific, existing Test identified by issue key, using the **key** attribute
- (Xray server/datacenter only) enforce the summary on the corresponding Test issue, during auto-provisioning, using the **summary** attribute
- (Xray server/datacenter only) enforce the description on the corresponding Test issue, during auto-provisioning, using the **description** attribute

_Examples:_

1. enforce the given test and corresponding result to be reported against the existing Test issue having the key  CALC-1000 (i.e. create a Test Run in Xray for the Test with issue key CALC-1000)  

```java
    @Test
    @XrayTest(key = "CALC-1000")
    public void CanAddNumbers()
```

2. enforce the "Summary" and the "Description" fields on the corresponding Test issue. The test will be auto-provisioned in Xray, if it doesn't exist yet, based on the class name and method name implementing the test  

```java
    @Test
    @XrayTest(summary = "addition of two numbers", description = "tests the sum of two positive integers")
    public void CanAddNumbers()
```

3. set some label(s) on the corresponding Test issue; more than one label may specified, delimited by space. The test will be auto-provisioned in Xray, if it doesn't exist yet, based on the class name and method name implementing the test  

```java
    @Test
    @XrayTest(labels = "core addition")
    public void CanAddNumbers()
```

#### @Requirement

You may use the **Requirement** annotation, before your test methods in order to identify the covered requirement.
You may specify more than one requirement, by using comma as delimeter (e.g., "CALC-1234,CALC-5678").

_Examples:_


1. use this test to cover a requirement/user story identified by the issue key CALC-1234 (i.e create a issue link "tests" between the Test issue and the identified requirement);  

```java
    @Test
    @Requirement(key = "CALC-1234")
    public void CanAddNumbers()
```

### Using the test result within the test method

It's possible also to set attributes related to the test method, during the test execution lifecycle, through the ITestResult interface. This is more verbose than using the annotations approach though.
Please note that if you choose this approach, any attributes you set within the test method may not be available in the report (e.g., for example if the test is skipped or fails meanwhile).

```java
import org.testng.Reporter;
...

public class CalcTest {

    @Test
    public void CanAddNumbers()
    {
        Assert.assertEquals(Calculator.Add(1, 1),2);
        Assert.assertEquals(Calculator.Add(-1, 1),0);

        ITestResult result = Reporter.getCurrentTestResult();
        result.setAttribute("requirement", "CALC-1234");   // Xray will try to create a link to this requirement issue
        result.setAttribute("test", "CALC-2");             // Xray will try to find this Test issue and report result against it
        result.setAttribute("labels", "core addition");    // Xray will add this(ese) label(s) to the associated Test issue
    }

}
```

## Other features

### Name of Tests

When tests are autoprovisoned on Xray side, the summary of the Test issue will be set based on these rules (the first that applies):

* based on the summary attribute of @XrayTest annotation (e.g., `@XrayTest(summary = "xxx")`);
* based on the description attribute of @XrayTest annotation (e.g., `@XrayTest(description = "xxx")`);
* based on the description attribute of the @Test annotation (e.g., `@Test(description = "some test")`
* based on the overriden test's method name, if it was made using `setTestName()` through the ITest implementation

### Parameterized and data-driven tests

In TestNG we can have data-driven tests either by injecting parameters from a XML configuration using the @Parameters annotation, or from a method annotated with @DataProvider, which in this case returns an array of parameters/values.

```java
    @Test
    @Parameters({"value", "isEven"})
    public void checkNumberFromXMLConfigIsEven(int value, boolean isEven) {
        assertEquals(isEven, value % 2 == 0);
    }

    @DataProvider(name = "numbers")
    public static Object[][] evenNumbers() {
        return new Object[][]{{1, false}, {2, true}, {4, true}};
    }

    @Test(dataProvider = "numbers")
    public void checkNumberFromDataProvideIsEven(Integer number, boolean expected) {
        assertEquals(expected, number % 2 == 0);
    }
```

How can these be visible on Xray/Jira side? To have a comprehensive visibility about the data used, you need to use the Xray JSON reporter.

The Xray JSON reporter can generate fake "manual" Tests with the respective dataset attached to them; just one Test will be provisioned. In Xray, it's possible to see all iterations, the used parameters and their values.

The TestNG XML report will generate an entry for each iteration/instance of the test method. This data related information (i.e. parameters and their values) won't be visible on Xray; different Tests will be created for each data row.
In sum, this format *does not* provide a proper way to have visibility of this information in Xray side.

### Evidence / Attachments

Adding evidence/attachments can be done by accessing `ITestResult` and adding an attribute named `attachments` containing an array of `File` objects.

```java
import org.testng.ITestResult;
import org.testng.Reporter;
...

public class DemoTests {

    @Test
    public void CanAddNumbers()
    {

        // this evidence will be added as global evidence on the Test Run
        ITestResult result = Reporter.getCurrentTestResult();
        File attachments[] = new File[] { new File("1.png"), new File("2.png") } ;
        result.setAttribute("attachments", attachments); 
        
        Assert.assertEquals(Calculator.Add(1, 1),2);
    }


    @DataProvider(name = "numbers")
    public static Object[][] evenNumbers() {
        return new Object[][]{{1, false}, {2, true}, {4, true}};
    }

    @Test(dataProvider = "numbers")
    public void checkNumberFromDataProvideIsEven(Integer number, boolean expected) {

        // this evidence will be added as step-level evidence on the dummy step of the Test Run associated with a "manual" autoprovisioned Test
        ITestResult result = Reporter.getCurrentTestResult();
        File attachments[] = new File[] { new File("1.png"), new File("2.png") } ;
        result.setAttribute("attachments", attachments); 

        assertEquals(expected, number % 2 == 0);
    }

}
```

#### Making evidence/attachments part of the Xray JSON report

This information will be added automatically by the `XrayJsonReporter` listener, so nothing else needs to be done besides using this reporter.

#### Making evidence/attachments part of the TestNG XML report (TestNG 7.1 onwards)

*Note: This feature is not yet supported by Xray. Please vote and track this [issue](https://jira.getxray.app/browse/XRAY-4557)*!

In order to add attachments to the result, first you need to use/enable the *XrayReportListener* listener, besides the *XrayListener*; you can do that, for example, by adding an entry/line to the `src/test/resources/META-INF/services/org.testng.ITestNGListener`

```bash
app.getxray.xray.testng.listeners.XrayListener
app.getxray.xray.testng.listeners.XrayReportListener
```

Then, in the test code, you can add `File` objects to an array that you need to set as an attribute named "attachments" on the `ITestResult`.

```java
    @Test
    public void CanAddNumbers()
    {
        ITestResult result = Reporter.getCurrentTestResult();
        File attachments[] = new File[] { new File("1.png"), new File("2.png") } ;
        result.setAttribute("attachments", attachments); 
        
        Assert.assertEquals(Calculator.Add(1, 1),2);
        Assert.assertEquals(Calculator.Add(-1, 1),0);
    }
```

The *XrayReportListener* will process the attribute `attachments` and will add them to the TestNG XML report, in a custom `<attachments>` XML section that Xray can process.

## TO DOs

- evaluate the merge of XrayListener and XrayReportListener, even though the later is not yet usable
- rename listeners
- improve javadocs

## FAQ

1. I want to use the Xray annotations; do I need to use any listener at all?

Yes. You need to enable the *XrayListener* listener, so that it can process the information from the annotations and add them as attributes on the corresponding `<test-method>` element on the TestNG XML report.

2. I ran the tests but the TestNG XML report doesn't contain any information at all that I provided using the Xray related annotations.

First, make sure you enabled the *XrayListener* listener; second, you need to configure your build tool (e.g., Maven) so that TestNG's XMLReporter can include user-defined attributes at test level; for that, the `generateTestResultAttributes` should be set as true (see above documentation).

3. Does this extension interact with Xray through any API, e.g., REST API, whatsoever?

No. This extension only generates test reports/results with additional information that Xray can process. Submission of the report generated by this extension to Xray itself is out-of-scope and should be done afterwards by invoking Xray REST API directly or by using one of the available CI/CD plugins.

4. I want to pass the Test Plan key, version, revision, Test Environment... Can that information be stored on the TestNG XML report?

No. This information is not stored on the TestNG XML report; this information is passed on the REST API call whenever submiting the test results (see previous answer) by the CI/CD tool/process.

5. What kind of reports (i.e. test result report file format) does this extension generate?

Either enhanced TestNG XML reports or Xray JSON reports.
If using `XrayListener` listener, eventually together with `XrayReportListener` listener, then a evolved TestNG XML report will be generated.
If the `XrayJsonReportListener` listener is used instead, then a Xray JSON report is generated instead.

## Contact

You may find me on [Twitter](https://twitter.com/darktelecom).
Any questions related with this code, please raise issues in this GitHub project. Feel free to contribute and submit PR's.
For Xray specific questions, please contact [Xray's support team](https://jira.getxray.app/servicedesk/customer/portal/2).

## References

- [Example using TestNG with Xray on Jira server/DC](https://docs.getxray.app/display/XRAY/Testing+using+TestNG+in+Java)
- [Example using TestNG with Xray on Jira Cloud](https://docs.getxray.app/display/XRAYCLOUD/Testing+using+TestNG+in+Java)
- [original discussion on customization of TestNG XML report](https://github.com/cbeust/testng/issues/2171)
- [enhanced TestNG XML report file format](https://docs.getxray.app/display/XRAYCLOUDDRAFT/Taking+advantage+of+TestNG+XML+reports)
- [TestNG XML endpoints for submiting results sto Xray Cloud](https://docs.getxray.app/display/XRAYCLOUD/Import+Execution+Results+-+REST#ImportExecutionResultsREST-TestNGXMLresults)
- [TestNG XML endpoints for submiting results to Xray server/DC](https://docs.getxray.app/display/XRAY/Import+Execution+Results+-+REST#ImportExecutionResultsREST-XrayJSONresults)
- [Xray JSON format and endpoint for submiting results to Xray cloud](https://docs.getxray.app/display/XRAYCLOUDDRAFT/Import+Execution+Results+-+REST+v2#ImportExecutionResultsRESTv2-XrayJSONresults)
- [Xray JSON format and endpoint for submiting results to Xray server/DC](https://docs.getxray.app/display/XRAY/Import+Execution+Results+-+REST#ImportExecutionResultsREST-XrayJSONresults)
- [tutorials showcasing uploading test results from CI/CD tools to Xray cloud](https://docs.getxray.app/display/XRAYCLOUD/Integrations)
- [tutorials showcasing uploading test results from CI/CD tools to Xray server/DC](https://docs.getxray.app/display/XRAY/Integrations)

## LICENSE

[MIT](LICENSE)
