
package app.getxray.xray.testng.listeners;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import app.getxray.xray.testng.annotations.Requirement;
import app.getxray.xray.testng.annotations.XrayTest;

/**
 * The listener interface for receiving events related to execution of tests, and process Xray related annotations.
 * The listener can be automatically invoked when TestNG tests are run by using ServiceLoader mechanism.
 * You can also add this listener to a TestNG Test class by adding
 * <code>@Listeners({app.getxray.testng.XrayListener.class})</code>
 * before the test class
 *
 * @see XrayTest
 * @see Requirement
 */
public class XrayListener implements IInvokedMethodListener, ITestListener  {
    
    boolean testSuccess = true;
    
    
    /* (non-Javadoc)
     * @see org.testng.IInvokedMethodListener#beforeInvocation(org.testng.IInvokedMethod, org.testng.ITestResult)
     */
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        String summary = null;
        String description = null;
        String testDescription = null;
        String xrayTestDescription = null;
        String xrayTestSummary = null;

        if (method.isTestMethod()) {
            testDescription = method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(Test.class).description();
            if (annotationPresent(method, XrayTest.class) ) {
                xrayTestDescription = method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(XrayTest.class).description();
                xrayTestSummary = method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(XrayTest.class).summary();
    
                testResult.setAttribute("test", method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(XrayTest.class).key());
                testResult.setAttribute("labels", method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(XrayTest.class).labels());
            }

            if (!emptyString(xrayTestSummary)) {
                summary = xrayTestSummary;
            } else if (!emptyString(xrayTestDescription)) {
                summary = xrayTestDescription;
            } else if (!emptyString(testDescription)) {
                summary = xrayTestDescription;
            }

            if (!emptyString(xrayTestDescription)) {
                description = xrayTestDescription;
            } else if (!emptyString(testDescription)) {
                description = testDescription;
            }

            if (!emptyString(summary)) {
                testResult.setAttribute("summary", summary);
            }
            if (!emptyString(description)) {
                testResult.setAttribute("description", description);
            }
    
            if (annotationPresent(method, Requirement.class) ) {
                testResult.setAttribute("requirement", method.getTestMethod().getConstructorOrMethod().getMethod().getAnnotation(Requirement.class).key());  
            }
        } 
    }

    
    private boolean annotationPresent(IInvokedMethod method, Class clazz) {
        return method.getTestMethod().getConstructorOrMethod().getMethod().isAnnotationPresent(clazz);
    }

    private boolean emptyString(String string) {
        return (string == null || string.isEmpty() || string.trim().isEmpty());
    }
    
    /* (non-Javadoc)
     * @see org.testng.IInvokedMethodListener#afterInvocation(org.testng.IInvokedMethod, org.testng.ITestResult)
     */
    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod() && !testSuccess) {
                testResult.setStatus(ITestResult.FAILURE);
        }
    }  

}
