/** 
Licensed Materials - Property of IBM

IBM Cognos Products: DOCS

(C) Copyright IBM Corp. 2005, 2008

US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with
IBM Corp.
*/
/**
 * reportrunner.java
 *
 * Copyright (C) 2008 Cognos ULC, an IBM Company. All rights reserved.
 * Cognos (R) is a trademark of Cognos ULC, (formerly Cognos Incorporated).
 *
 */

/*import java.io.FileOutputStream;*/

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import org.apache.axis.AxisFault;

import Common.CognosBIException;

import com.cognos.developer.schemas.bibus._3.AsynchDetailReportOutput;
import com.cognos.developer.schemas.bibus._3.AsynchOptionEnum;
import com.cognos.developer.schemas.bibus._3.AsynchOptionInt;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.OutputEncapsulationEnum;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.ParmValueItem;
import com.cognos.developer.schemas.bibus._3.ReportService_PortType;
import com.cognos.developer.schemas.bibus._3.ReportService_ServiceLocator;
import com.cognos.developer.schemas.bibus._3.RunOptionBoolean;
import com.cognos.developer.schemas.bibus._3.RunOptionEnum;
import com.cognos.developer.schemas.bibus._3.RunOptionOutputEncapsulation;
import com.cognos.developer.schemas.bibus._3.RunOptionStringArray;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.SimpleParmValueItem;


/**
 * Run a report from the samples deployment archive, and save its output
 * as HTML.
 *
 * Note that this application does no error handling; errors will cause
 * ugly exception stack traces to appear on the console, and the
 * application will exit ungracefully.
 */
public class reportrunner
{
	
	private String serverHost = "";
	private String serverPort = "";
	private String userteam = "";
	private String overview_id = "overview=";
	private String report_id = "";
	//private static final String reportPath = "/content/folder[@name='PMQ_WELL']/folder[@name='OVERVIEW']/interactiveReport[@name='overview']";
	private String reportPath = "/content/folder[@name='zy_test']/interactiveReport[@name='overview']";
	public reportrunner(String aserverHost, String aserverPort, String auserteam, String aoverview_id){
		serverHost = aserverHost;
		serverPort = aserverPort;
		userteam = auserteam;
		overview_id += aoverview_id;
		reportPath = getOverviewName(reportPath,auserteam);
		System.out.println(reportPath);
	}	 
	
	private String getOverviewName(final String path, final String auserteam){
		String new_name = "";
		String pattern = "interactiveReport\\[@name='(.+?)'\\]";
    	Pattern reg = Pattern.compile(pattern);
    	Matcher m = reg.matcher(path);
    	while(m.find()){
    		new_name = path.replace(m.group(1), m.group(1)+"_"+auserteam);
    	}
    	return new_name;
    	
	}

	public void runReport()
	{
		// Create a connection to a report server.
		String Cognos_URL =
			"http://"
				+ serverHost
				+ ":"
				+ serverPort
				+ "/p2pd/servlet/dispatch";

		System.out.println(
			"Creating connection to "
				+ serverHost
				+ ":"
				+ serverPort
				+ "...");
		System.out.println("Server URL: " + Cognos_URL);
		ReportService_ServiceLocator reportServiceLocator =
			new ReportService_ServiceLocator();

		ReportService_PortType repService = null;

		try
		{
			repService = reportServiceLocator.getreportService(new URL(Cognos_URL));
		}
		catch (MalformedURLException ex)
		{
			System.out.println(
				"Caught a MalformedURLException:\n" + ex.getMessage());
			System.out.println("Server URL was: " + Cognos_URL);
			System.exit(-1);
		}
		catch (ServiceException ex)
		{
			System.out.println("Caught a ServiceException: " + ex.getMessage());
			ex.printStackTrace();
			System.exit(-1);
		}

		System.out.println("... done.");
		
		ParameterValue parameters[] = getParameter(overview_id);

		Option runOptions[] = new Option[5];

		RunOptionBoolean saveOutput = new RunOptionBoolean();
		saveOutput.setName(RunOptionEnum.saveOutput);
		saveOutput.setValue(true);
		runOptions[0] = saveOutput;

		// TODO: Output format should be specified on the command-line
		RunOptionStringArray outputFormat = new RunOptionStringArray();
		outputFormat.setName(RunOptionEnum.outputFormat);
		outputFormat.setValue(new String[] { "HTML" });
		runOptions[1] = outputFormat;

		RunOptionOutputEncapsulation outputEncapsulation =
			new RunOptionOutputEncapsulation();
		outputEncapsulation.setName(RunOptionEnum.outputEncapsulation);
		outputEncapsulation.setValue(OutputEncapsulationEnum.none);
		runOptions[2] = outputEncapsulation;

		AsynchOptionInt primaryWait = new AsynchOptionInt();
		primaryWait.setName(AsynchOptionEnum.primaryWaitThreshold);
		primaryWait.setValue(0);
		runOptions[3] = primaryWait;

		AsynchOptionInt secondaryWait = new AsynchOptionInt();
		secondaryWait.setName(AsynchOptionEnum.secondaryWaitThreshold);
		secondaryWait.setValue(0);
		runOptions[4] = secondaryWait;
		
		// Now, run the report.
		try
		{
			System.out.println("Running the report...");
			System.out.println("Report search path is:");
			System.out.println(reportPath);
			
			AsynchReply res =
				repService.run(new SearchPathSingleObject(reportPath), parameters, runOptions);

			System.out.println("... done.");

			// The report is finished, let's fetch the results and save them to
			// a file.
			if (res.getStatus() == AsynchReplyStatusEnum.complete)

			{
				AsynchDetailReportOutput reportOutput = null;

				for (int i = 0; i < res.getDetails().length; i++)
				{
					if (res.getDetails()[i]
						instanceof AsynchDetailReportOutput)
					{
						reportOutput =
							(AsynchDetailReportOutput)res.getDetails()[i];
						setReport_id(reportOutput.getOutputObjects()[0].getStoreID().getValue().get_value());
						break;
					}
				}

				System.out.println("... done.");
			}
		}
		catch (AxisFault ex)
		{
			String ex_str = CognosBIException.convertToString(ex);
			System.out.println("SOAP exception!\n");
			System.out.println(ex_str);
		}
		catch (Exception ex)
		{
			System.out.println("Unhandled exception!");
			System.out.println("Message: \n" + ex.getMessage());
			System.out.println("Stack trace:");
			ex.printStackTrace();
		}
	}
	
	public static ParameterValue[] getParameter(String para) {
        String pm[] = para.split("\\|");
        ParameterValue[] parameters = new ParameterValue[pm.length];
        if (pm.length > 0) {
                for (int i = 0; i < pm.length; i++) {
                        int splitNum = pm[i].indexOf("=", 1);
                        String paraname = pm[i].substring(0, splitNum);
                        String paravalue = pm[i]
                                        .substring(splitNum + 1, pm[i].length());
                        SimpleParmValueItem item = new SimpleParmValueItem();

                        item.setUse(paravalue);
                        item.setDisplay(paravalue);
                        item.setInclusive(true);
                        ParmValueItem[] pvi = new ParmValueItem[1];
                        pvi[0] = item;
                        parameters[i] = new ParameterValue();
                        parameters[i].setName(paraname);
                        parameters[i].setValue(pvi);
                }
        }
        return parameters;
}


	public String getServerHost() {
		return serverHost;
	}


	public String getServerPort() {
		return serverPort;
	}

	public String getUserteam() {
		return userteam;
	}

	public String getOverview_id() {
		return overview_id;
	}

	public String getReport_id() {
		return report_id;
	}
	
	private void setReport_id(final String id) {
		report_id = id;
	}
	
	public static void main(String[] args){
		//Sample
		/**
		 * Create a report runner instance. 
		 * 
		 * @param serverHost 
		 * @param serverPort
		 * @param userteam
		 * @param overview_id: according to userteam, find the newest overview_id in that day
		 */
		reportrunner r = new reportrunner("9.110.83.168","9080","T01","104");

		/**
		 * Run a new report for the userteam as a overview for that day. 
		 * 
		 * @throws MalformedURLException if CognosURL of server is wrong.
		 * @throws ServiceException if no cognos service exists.
		 * @throws AxisFault if a.reportPath is wrong; b.no userteam overview exists in Cognos folder. 
		 * @throws Exception if unknown exceptions occur.s
		 */
		r.runReport();
		
		/**
		 * Get new report id.
		 */
		String report_id = r.getReport_id(); //In outer code, this id should be written in /home/PMQ_WELL/CognosReportID.properties
	}
}
