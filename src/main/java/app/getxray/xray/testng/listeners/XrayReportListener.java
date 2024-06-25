package app.getxray.xray.testng.listeners;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.reporters.XMLReporter;
import org.testng.reporters.XMLStringBuffer;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * @deprecated This class is deprecated and should not be used.
 */
@Deprecated
public class XrayReportListener extends XMLReporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(XrayReportListener.class);
  public static final String TAG_ATTACHMENTS = "attachments";
  public static final String TAG_ATTACHMENT = "attachment";
  public static final String TAG_ATTACHMENT_RAWCONTENT = "rawcontent";
  public static final String ATTR_NAME = "name";
  public static final String ATTR_FILENAME = "filename";
  public static final String ATTR_SRC = "src";

  @Override
  public void addCustomTagsFor(XMLStringBuffer xmlBuffer, ITestResult testResult) {
  	xmlBuffer.push(XrayReportListener.TAG_ATTACHMENTS);
    Base64.Encoder enc= Base64.getEncoder();

    File[] attachments = (File[])testResult.getAttribute("attachments");

    if (attachments != null){
      for (File file : attachments)  {
        Properties valueAttrs = new Properties();
        valueAttrs.setProperty(XrayReportListener.ATTR_FILENAME, file.getName());
        valueAttrs.setProperty(XrayReportListener.ATTR_SRC, file.getAbsolutePath());

        xmlBuffer.push(XrayReportListener.TAG_ATTACHMENT, valueAttrs);

        try {
          byte[] fileContent = Files.readAllBytes(file.toPath());
          byte[] encoded = enc.encode(fileContent);
          String encodedStr = new String(encoded, StandardCharsets.UTF_8);

          xmlBuffer.push(XrayReportListener.TAG_ATTACHMENT_RAWCONTENT);
          xmlBuffer.addCDATA(encodedStr);
          xmlBuffer.pop();
        } catch (Exception ex) {
          LOGGER.error("Error reading file attachment content", ex);
        }

        xmlBuffer.pop();
      }

    }

    xmlBuffer.pop();
  }
}
