/** 
Licensed Materials - Property of IBM

IBM Cognos Products: DOCS

(C) Copyright IBM Corp. 2005, 2008

US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with
IBM Corp.
*/
/**
 * RunReport.java
 *
 *
 * Copyright (C) 2008 Cognos ULC, an IBM Company. All rights reserved.
 * Cognos (R) is a trademark of Cognos ULC, (formerly Cognos Incorporated).
 *
 * Description: This code sample demonstrates how to run different types of reports using the following
 *	           methods:
 *	           - run
 *	             Use this method to run a report, query, or report view.
 *	           - wait
 *	             Use this method to notify the server that the issuer of the request is still
 *	             waiting for the output, and to request that the processing be continued.
 *	           - query
 *	             Use this method to request objects from Content Manager.
 *			   - getOutput
 *				 Use this method to request that the output be sent to the issuer
 *				 of the request.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import org.apache.axis.encoding.Base64;

import Common.BaseClassWrapper;
import Common.CRNConnect;
import Common.ReportParameters;

import com.cognos.developer.schemas.bibus._3.AsynchDetailReportOutput;
import com.cognos.developer.schemas.bibus._3.AsynchDetailReportStatus;
import com.cognos.developer.schemas.bibus._3.AsynchDetailReportStatusEnum;
import com.cognos.developer.schemas.bibus._3.AsynchOptionBoolean;
import com.cognos.developer.schemas.bibus._3.AsynchOptionEnum;
import com.cognos.developer.schemas.bibus._3.AsynchReply;
import com.cognos.developer.schemas.bibus._3.AsynchReplyStatusEnum;
import com.cognos.developer.schemas.bibus._3.AsynchSecondaryRequest;
import com.cognos.developer.schemas.bibus._3.AuthoredReport;
import com.cognos.developer.schemas.bibus._3.BaseClass;
import com.cognos.developer.schemas.bibus._3.BaseParameter;
import com.cognos.developer.schemas.bibus._3.DeliveryOptionEnum;
import com.cognos.developer.schemas.bibus._3.DeliveryOptionString;
import com.cognos.developer.schemas.bibus._3.Graphic;
import com.cognos.developer.schemas.bibus._3.Option;
import com.cognos.developer.schemas.bibus._3.Output;
import com.cognos.developer.schemas.bibus._3.Page;
import com.cognos.developer.schemas.bibus._3.Parameter;
import com.cognos.developer.schemas.bibus._3.ParameterValue;
import com.cognos.developer.schemas.bibus._3.PropEnum;
import com.cognos.developer.schemas.bibus._3.QueryOptions;
import com.cognos.developer.schemas.bibus._3.RunOptionBoolean;
import com.cognos.developer.schemas.bibus._3.RunOptionEnum;
import com.cognos.developer.schemas.bibus._3.RunOptionStringArray;
import com.cognos.developer.schemas.bibus._3.SearchPathMultipleObject;
import com.cognos.developer.schemas.bibus._3.SearchPathSingleObject;
import com.cognos.developer.schemas.bibus._3.Sort;

public class RunReport
{
	private static final int REP_HTML = 0;
	private static final int REP_XML = 1;
	private static final int REP_PDF = 2;
	private static final int REP_CSV = 3;
	private static final int REP_HTML_FRAG = 4;
	private static final int REP_MHT = 5;
	private static final int REP_XLWA = 6;

	static int tmpFileCounter = 0;

	private CRNConnect my_connection;
	private AsynchReply my_rsr;

	/**
		* runReport will execute query the content store of the report
		* or query and thenpass it off to be executed in the default browser.
		*
		* @param connect
		*	     Specifies the Connection to Server.
		* @param report
		*		 Specifies the name of the report or query.
		* @param reportType
		*		  Specifies the type of report.
		* @return
		*		 Returns the text message indicating whether the report or query ran successfully.
		*/
	public String runReport(
		CRNConnect connect,
		BaseClassWrapper report,
		int reportType,
		boolean doBurst)
		throws java.rmi.RemoteException
	{
		String output = new String();
		if ((connect != null)
			&& (report != null)
			&& (connect.getDefaultSavePath() != null))
		{
			my_connection = connect;
			// sn_dg_prm_smpl_runreport_P1_start_0
			ParameterValue emptyParameterValues[] = new ParameterValue[] {};
			ParameterValue reportParameterValues[] = null;

			ReportParameters repParms = new ReportParameters();
			BaseParameter[] parametersInReportSpec = new Parameter[] {};

			try
			{
				parametersInReportSpec =
					repParms.getReportParameters(report, connect);
			}
			// sn_dg_prm_smpl_runreport_P1_end_0
			catch (java.rmi.RemoteException remoteEx)
			{
				System.out.println("Caught Remote Exception:\n");
				remoteEx.printStackTrace();
			}

			if (parametersInReportSpec != null
				&& parametersInReportSpec.length > 0)
			{
				reportParameterValues =
					ReportParameters.setReportParameters(
						parametersInReportSpec);
			}

			if ((reportParameterValues == null)
				|| (reportParameterValues.length <= 0))
			{
				reportParameterValues = emptyParameterValues;
			}

			output
				+= executeReport(
					report,
					connect,
					reportType,
					reportParameterValues,
					doBurst,
					null);
		}
		return output;
	}

	/**
		* runReportAt will set up the report or query to be run at the
		* specified time.
		*
		* @param connect
		*	     Specifies the Connection to Server.
		* @param report
		*		 Specifies the name of the report or query.
		* @param reportType
		*		  Specifies the type of report.
		* @param execTime
		*		  Specifies the time to run.
		* @return
		*		 Returns the text message indicating whether the report or query ran successfully.
		*/
	public String runReportAt(
		CRNConnect connect,
		BaseClassWrapper report,
		int reportType,
		Calendar execTime)
		throws java.rmi.RemoteException
	{
		String output = "runReportAt did not complete as expected";
		String resultEventID = new String();
		Option reportRunOptions[];

		String rSearchPath =
			report.getBaseClassObject().getSearchPath().getValue();

		if ((connect != null)
			&& (report != null)
			&& (connect.getDefaultSavePath() != null))
		{
			//Prepare the runOptions
			reportRunOptions = getDefaultRunOptions(reportType, null);

			ParameterValue emptyParameterValues[] = new ParameterValue[] {};
			ParameterValue reportParameterValues[] = null;

			ReportParameters repParms = new ReportParameters();
			BaseParameter[] parametersInReportSpec = new Parameter[] {};

			try
			{
				parametersInReportSpec =
					repParms.getReportParameters(report, connect);
			}
			catch (java.rmi.RemoteException remoteEx)
			{
				System.out.println("Caught Remote Exception:\n");
				remoteEx.printStackTrace();
			}

			if (parametersInReportSpec != null
				&& parametersInReportSpec.length > 0)
			{
				reportParameterValues =
					ReportParameters.setReportParameters(
						parametersInReportSpec);
			}

			if ((reportParameterValues == null)
				|| (reportParameterValues.length <= 0))
			{
				reportParameterValues = emptyParameterValues;
			}

			try
			{
				// sn_dg_sdk_method_eventManagementService_runAt_start_0
				resultEventID =
					connect.getEventMgmtService().runAt(
						execTime,
						new SearchPathSingleObject(rSearchPath),
						reportParameterValues,
						reportRunOptions);
				// sn_dg_sdk_method_eventManagementService_runAt_end_0

				output = "runAt set up event ID" + resultEventID;
			}
			catch (java.rmi.RemoteException remoteEx)
			{
				System.out.println("Caught Remote Exception:\n");
				remoteEx.printStackTrace();
			}
		}
		return output;
	}

	/**
	* This Java method executes the specified report and returns a boolean value
	* indicating whether the report or query ran successfully.
	* @param report
	*		 Specifies the report.
	* @param connect
	*	     Specifies the Connection to Server.
	* @param reportType
	*		  Specifies the output format of report: HTML, XML, PDF.
	* @param paramValueArray
	* 			Specifies the parameter values, if any, to use for the report
	* @param runOptions
	* 			Specifies the options, if any, to use for the report
	* @return
	*		 Returns true if the report was executed successfully and false if the report did not run.
	*/
	// sn_dg_prm_smpl_runreport_P4_start_0
	public String executeReport(
		BaseClassWrapper report,
		CRNConnect connect,
		int reportType,
		ParameterValue paramValueArray[],
		boolean doBurst,
		Option runOptions[])
		throws java.rmi.RemoteException
	{
		Option execReportRunOptions[];
		AsynchReply rsr = null;
        
        //check for advanced routing server group
        String serverGroup = ((AuthoredReport)report.getBaseClassObject()).getRoutingServerGroup().getValue();
        if(serverGroup == null) {
            serverGroup = "";
        }
        
        String rSearchPath =
			report.getBaseClassObject().getSearchPath().getValue();
		String ERR_MESG =
			"run() failed to return a valid report in this format";

		//Prepare the runOptions
		if(doBurst)
		{
			execReportRunOptions = getBurstRunOptions(report, reportType, runOptions);
		}
		else
		{
			execReportRunOptions = getDefaultRunOptions(reportType, runOptions);
		}

		// sn_dg_sdk_method_reportService_run_start_1
		//Call run()
		rsr =
			connect.getReportService(true, serverGroup).run(
				new SearchPathSingleObject(rSearchPath),
				paramValueArray,
				execReportRunOptions);

		// sn_dg_sdk_method_reportService_run_end_1

		// If response is not immediately complete, call wait until complete
		if (!rsr.getStatus().equals(AsynchReplyStatusEnum.complete)&&!rsr.getStatus().equals(AsynchReplyStatusEnum.conversationComplete))
		{
			while (!rsr.getStatus().equals(AsynchReplyStatusEnum.complete)&&!rsr.getStatus().equals(AsynchReplyStatusEnum.conversationComplete))
			{
				//before calling wait, double check that it is okay
				if (!hasSecondaryRequest(rsr, "wait"))
				{
						return ERR_MESG;
				}
				rsr =
					connect.getReportService().wait(
						rsr.getPrimaryRequest(),
						new ParameterValue[] {},
						new Option[] {});
			}

			//After calling wait() it is necessary to check to make sure
			//the output is ready before retrieving it
			if (outputIsReady(rsr))
			{
				rsr =
					connect.getReportService().getOutput(
						rsr.getPrimaryRequest(),
						new ParameterValue[] {},
						new Option[] {});
			}
			else
			{
				return ERR_MESG;
			}
		}
		// sn_dg_prm_smpl_runreport_P4_end_0

		//rsr grab a copy of this final rsr for local use later
		my_rsr = rsr;

		if(doBurst)
		{
			return ("Bursted");
		}

		//Do something with the output
		return textOrBinaryOutput(connect, rsr, report.toString(), reportType);

	}

	public String textOrBinaryOutput(
		CRNConnect connect,
		AsynchReply rsr,
		String report,
		int reportType)
	{
		String textOutput = null;

		if (reportType == REP_PDF
			|| reportType == REP_CSV
			|| reportType == REP_XLWA)
		{
			textOutput =
				saveBinaryOutput(connect, rsr, report.toString(), reportType);
		}
		else if (
			reportType == REP_HTML
				|| reportType == REP_XML
				|| reportType == REP_HTML_FRAG
				|| reportType == REP_MHT)
		{
			textOutput =
				getOutputPage(connect, rsr, report.toString(), reportType);
		}

		if (textOutput == null)
		{
			return "The server failed to return valid report output in this format";
		}
		return textOutput;
	}

	public boolean outputIsReady(AsynchReply response)
	{
		for (int i = 0; i < response.getDetails().length; i++)
		{
			if ((response.getDetails()[i] instanceof AsynchDetailReportStatus)
				&& (((AsynchDetailReportStatus)response.getDetails()[i])
					.getStatus()
					== AsynchDetailReportStatusEnum.responseReady)
				&& (hasSecondaryRequest(response, "getOutput")))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean hasSecondaryRequest(
		AsynchReply response,
		String secondaryRequest)
	{
		AsynchSecondaryRequest[] secondaryRequests =
			response.getSecondaryRequests();
		for (int i = 0; i < secondaryRequests.length; i++)
		{
			if (secondaryRequests[i].getName().compareTo(secondaryRequest)
				== 0)
			{
				return true;
			}
		}
		return false;
	}

	public String saveBinaryOutput(
		CRNConnect connection,
		AsynchReply response,
		String reportString,
		int reportType)
	{
		byte binaryOutput[] = null;
		BaseClass additionalOutputObject = null;
		AsynchDetailReportOutput reportOutput = null;

		try
		{

			for (int i = 0; i < response.getDetails().length; i++)
			{
				if (response.getDetails()[i]
					instanceof AsynchDetailReportOutput)
				{
					reportOutput =
						(AsynchDetailReportOutput)response.getDetails()[i];
					break;
				}
			}

			// decode the PDF/CSV output to array of bytes.
			// binary output is not split into multiple pages, there will be only one
			if(reportType == REP_PDF
					|| reportType == REP_CSV)
			{
				binaryOutput = Base64.decode(reportOutput.getOutputPages()[0]);
			}
			else
			{
				binaryOutput=(reportOutput.getOutputPages()[0]).getBytes();
			}

			if (binaryOutput == null)
			{
				return "Server failed to return a valid report in this format.";
			}

			//For some Binary output types (Excel) there may be additional output objects
			//that must be retrieved and saved with the "main" report output
			//
			//For the references to work correctly, the file name must be consistent
			//with the name used in the content store
			if (reportOutput.getOutputObjects().length > 0)
			{
				additionalOutputObject = reportOutput.getOutputObjects()[0];
				if (additionalOutputObject != null)
				{
					PropEnum[] props =
						new PropEnum[] {
							PropEnum.searchPath,
							PropEnum.defaultName,
							PropEnum.dataType,
							PropEnum.data,
							PropEnum.objectClass };

					SearchPathMultipleObject aopSearchPath = new SearchPathMultipleObject();
					aopSearchPath.set_value(additionalOutputObject.getSearchPath().getValue() + "/page");
					BaseClass[] additionalOutputPages =
						connection.getCMService().query(
							aopSearchPath,
							props,
							new Sort[] {},
							new QueryOptions());
					for (int i = 0; i < additionalOutputPages.length; i++)
					{
						if (additionalOutputPages[i] instanceof Page)
						{
							File oFile =
								new File(
									connection.getDefaultSavePath()
										+ "/"
										+ additionalOutputPages[i]
											.getDefaultName()
											.getValue());
							FileOutputStream fos = new FileOutputStream(oFile);

							fos.write(
								((Page)additionalOutputPages[i]).getData().getValue());
							fos.flush();
							fos.close();
						}
					}
				}
			}

			//write the binary output to a file
			File oFile = getNewTempFile(null, reportString, reportType);
			FileOutputStream fos = new FileOutputStream(oFile);

			fos.write(binaryOutput);
			fos.flush();
			fos.close();

			//return filename where the main output was written
			return oFile.getAbsolutePath();

		}
		catch (java.io.FileNotFoundException fileNotFoundEx)
		{
			//fileNotFoundEx.printStackTrace();
			return fileNotFoundEx.toString();
		}
		catch (java.io.IOException ioEx)
		{
			//ioEx.printStackTrace();
			return ioEx.toString();
		}
	}

	public String getOutputPage(
		CRNConnect connect,
		AsynchReply response,
		String reportString,
		int reportType)
	{

		AsynchDetailReportOutput reportOutput = null;
		for (int i = 0; i < response.getDetails().length; i++)
		{
			if (response.getDetails()[i] instanceof AsynchDetailReportOutput)
			{
				reportOutput =
					(AsynchDetailReportOutput)response.getDetails()[i];
				break;
			}
		}

		if (reportOutput == null)
		{
			return "Server failed to return a valid report in this format.";
		}

		String textOutput = "";

		//If there are graphics in the output, it must be handled
		//differently than text based output
		if (reportOutput.getOutputObjects().length > 0)
		{
			textOutput = replaceLocalGraphicsInOutput(connect, reportOutput);
		}
		else
		{
			//text based output is split into pages -- return the current page
			String[] pages = reportOutput.getOutputPages();
			textOutput = pages[0].toString();
		}

		File oFile = getNewTempFile(null, reportString, reportType);
		try
		{

			//write the updated html output to a file
			FileOutputStream fos = new FileOutputStream(oFile);

			fos.write(textOutput.getBytes());
			fos.flush();
			fos.close();
		}
		catch (java.io.IOException ioEx)
		{}

		return oFile.getAbsolutePath();

	}

	public String replaceLocalGraphicsInOutput(
		CRNConnect connect,
		AsynchDetailReportOutput reportOutput)
	{
		// this section deals with executing reports with charts or graphics.
		// the img tag contains credentials that are not valid when the report
		// is not run through report or query studio therefore we must extract
		// the img from content mgr and then save it locally, then replace the
		// img tag returned from the server with a path to the local graphic.
		// TODO Only the first graphic is handled. For reports with multiple
		// graphics, a for loop to process all of them would be appropriate.
		BaseClass bcGraphic[];

		try
		{

			SearchPathMultipleObject graphicSearchPath = new SearchPathMultipleObject();
			graphicSearchPath.set_value(reportOutput.getOutputObjects()[0].getSearchPath().getValue());
			bcGraphic =
				connect.getCMService().query(
					graphicSearchPath,
					new PropEnum[] { PropEnum.searchPath },
					new Sort[] {},
					new QueryOptions());
			Output out = null;
			if ((bcGraphic.length > 0) && (bcGraphic[0] instanceof Output))
			{
				SearchPathMultipleObject outSearchPath = new SearchPathMultipleObject();
				out = (Output)bcGraphic[0];
				outSearchPath.set_value(out.getSearchPath().getValue() + "/graphic");
				Graphic g =
					(Graphic)connect.getCMService().query(
						outSearchPath,
						new PropEnum[] {
							PropEnum.searchPath,
							PropEnum.data,
							PropEnum.dataType },
						new Sort[] {},
						new QueryOptions())[0];

				String graphicFile =
					connect.getDefaultSavePath()
						+ "/"
						+ System.getProperty("file.separator")
						+ "graphicToDisplay.png";

				// save graphic locally
				File gFile = new File(graphicFile);
				FileOutputStream fos = new FileOutputStream(gFile);
				fos.write(g.getData().getValue());
				fos.flush();
				fos.close();

				// return the HTML output, search&replace img tag with local image.
				String[] pages = reportOutput.getOutputPages();
				String html = pages[0].toString();

				String start = null;
				String end = null;
				int index = 0;
				index = html.indexOf("<img", 0);
				start = html.substring(0, index);
				end = html.substring(html.indexOf(">", index) + 1);
				html = start + "<img src='graphicToDisplay.png'>" + end;

				return html;
			}

		}
		catch (java.io.FileNotFoundException fileNotFoundEx)
		{
			return "Unable to open or create file to save graphics output";
		}
		catch (java.rmi.RemoteException remoteEx)
		{
			remoteEx.printStackTrace();
			return remoteEx.toString();
		}
		catch (java.io.IOException ioEx)
		{
			ioEx.printStackTrace();
			return ioEx.toString();
		}

		return "run() failed to return a valid report in this format";
	}

	// sn_dg_prm_smpl_runreport_P3_start_0
	public Option[] getDefaultRunOptions(int reportType, Option[] options)
	{
	// sn_dg_prm_smpl_runreport_P3_end_0
		if (options != null)
		{
			//check for mandatory options
			for (int i = 0; i < options.length; i++)
			{
				//ouputFormat
				if (options[i] instanceof RunOptionStringArray)
				{
					RunOptionStringArray tmpOption =
						(RunOptionStringArray)options[i];
					if (tmpOption.getName() == RunOptionEnum.prompt)
					{
						return options;
					}
				}
			}

			RunOptionStringArray outputFormat = new RunOptionStringArray();
			outputFormat.setName(RunOptionEnum.outputFormat);
			outputFormat.setValue(setFormatByType(reportType));

			Option[] newOptions = new Option[options.length + 2];
			for (int i = 0; i < options.length; i++)
			{
				newOptions[i] = options[i];
			}
			newOptions[options.length + 1] = outputFormat;

			return newOptions;
		}
		else
		{
			// sn_dg_prm_smpl_runreport_P3_start_1
			Option execReportRunOptions[] = new Option[6];
			RunOptionBoolean saveOutputRunOption = new RunOptionBoolean();
			RunOptionStringArray outputFormat = new RunOptionStringArray();
			RunOptionBoolean promptFlag = new RunOptionBoolean();
			AsynchOptionBoolean includePrimaryRequest = new AsynchOptionBoolean();

			//Set the option for saving the output to false
			saveOutputRunOption.setName(RunOptionEnum.saveOutput);
			saveOutputRunOption.setValue(false);

			//What format do we want the report in: PDF, HTML, or XML?
			outputFormat.setName(RunOptionEnum.outputFormat);
			String[] reportFormat = null;
			reportFormat = setFormatByType(reportType);
			outputFormat.setValue(reportFormat);

			//Set the report not to prompt as we pass the parameter (if any)
			promptFlag.setName(RunOptionEnum.prompt);
			promptFlag.setValue(false);

			//Set the option to always have the primaryRequest in the response
			includePrimaryRequest.setName(
				AsynchOptionEnum.alwaysIncludePrimaryRequest);
			includePrimaryRequest.setValue(true);
			// sn_dg_prm_smpl_runreport_P3_end_1

			//			AsynchOptionInt primaryWaitThreshold = new AsynchOptionInt();
			//			AsynchOptionInt secondaryWaitThreshold = new AsynchOptionInt();
			//			primaryWaitThreshold.setName(AsynchOptionEnum.primaryWaitThreshold);
			//			primaryWaitThreshold.setValue(1);
			//			secondaryWaitThreshold.setName(AsynchOptionEnum.secondaryWaitThreshold);
			//			secondaryWaitThreshold.setValue(1);

			// Fill the array with the rest of the run options.
			// sn_dg_prm_smpl_runreport_P3_start_2
			execReportRunOptions[0] = saveOutputRunOption;
			execReportRunOptions[1] = outputFormat;
			execReportRunOptions[2] = promptFlag;
			execReportRunOptions[3] = includePrimaryRequest;
			// sn_dg_prm_smpl_runreport_P3_end_2
			//			execReportRunOptions[4] = primaryWaitThreshold;
			//			execReportRunOptions[5] = secondaryWaitThreshold;

			// sn_dg_prm_smpl_runreport_P3_start_3
			return execReportRunOptions;
			// sn_dg_prm_smpl_runreport_P3_end_3
		}

	}

	public Option[] getBurstRunOptions(BaseClassWrapper report, int reportType, Option[] options)
	{
		if (options != null)
		{
			//check for mandatory options
			for (int i = 0; i < options.length; i++)
			{
				//ouputFormat
				if (options[i] instanceof RunOptionStringArray)
				{
					RunOptionStringArray tmpOption =
						(RunOptionStringArray)options[i];
					if (tmpOption.getName() == RunOptionEnum.prompt)
					{
						return options;
					}
				}
			}

			RunOptionStringArray outputFormat = new RunOptionStringArray();
			outputFormat.setName(RunOptionEnum.outputFormat);
			outputFormat.setValue(setFormatByType(reportType));

			Option[] newOptions = new Option[options.length + 2];
			for (int i = 0; i < options.length; i++)
			{
				newOptions[i] = options[i];
			}
			newOptions[options.length + 1] = outputFormat;

			return newOptions;
		}
		else
		{
			Option execReportRunOptions[] = new Option[8];
			RunOptionBoolean saveOutputRunOption = new RunOptionBoolean();
			RunOptionBoolean burstRunOption = new RunOptionBoolean();
			RunOptionBoolean sendByEmailRunOption = new RunOptionBoolean();
			DeliveryOptionString emailSubjectRunOption = new DeliveryOptionString();
			RunOptionBoolean emailAsAttachmentRunOption = new RunOptionBoolean();
			RunOptionStringArray outputFormat = new RunOptionStringArray();
			RunOptionBoolean promptFlag = new RunOptionBoolean();
			AsynchOptionBoolean includePrimaryRequest = new AsynchOptionBoolean();

			//Set the option for saving the output to false
			saveOutputRunOption.setName(RunOptionEnum.saveOutput);
			saveOutputRunOption.setValue(false);

			// Set the option for burst the report to true
			burstRunOption.setName(RunOptionEnum.burst);
			burstRunOption.setValue(true);

			// Set the option for send by email to true
			sendByEmailRunOption.setName(RunOptionEnum.email);
			sendByEmailRunOption.setValue(true);

			// Specifies the subject line of the emailed output
			emailSubjectRunOption.setName(DeliveryOptionEnum.subject);
			emailSubjectRunOption.setValue("Report: " + report.toString());

			// Set the option for an email attachment to true
			emailAsAttachmentRunOption.setName(RunOptionEnum.emailAsAttachment);
			emailAsAttachmentRunOption.setValue(true);

			// What format do we want the report in: PDF, HTML, or XML?
			outputFormat.setName(RunOptionEnum.outputFormat);
			String[] reportFormat = null;
			reportFormat = setFormatByType(reportType);
			outputFormat.setValue(reportFormat);

			// Set the report not to prompt as we pass the parameter (if any)
			promptFlag.setName(RunOptionEnum.prompt);
			promptFlag.setValue(false);

			//Set the option to always have the primaryRequest in the response
			includePrimaryRequest.setName(
				AsynchOptionEnum.alwaysIncludePrimaryRequest);
			includePrimaryRequest.setValue(true);

			//			AsynchOptionInt primaryWaitThreshold = new AsynchOptionInt();
			//			AsynchOptionInt secondaryWaitThreshold = new AsynchOptionInt();
			//			primaryWaitThreshold.setName(AsynchOptionEnum.primaryWaitThreshold);
			//			primaryWaitThreshold.setValue(1);
			//			secondaryWaitThreshold.setName(AsynchOptionEnum.secondaryWaitThreshold);
			//			secondaryWaitThreshold.setValue(1);

			// Fill the array with the rest of the run options.
			execReportRunOptions[0] = saveOutputRunOption;
			execReportRunOptions[1] = burstRunOption;
			execReportRunOptions[2] = sendByEmailRunOption;
			execReportRunOptions[3] = emailSubjectRunOption;
			execReportRunOptions[4] = emailAsAttachmentRunOption;
			execReportRunOptions[5] = outputFormat;
			execReportRunOptions[6] = promptFlag;
			execReportRunOptions[7] = includePrimaryRequest;

			return execReportRunOptions;
		}
	}


	public String nextPage(BaseClassWrapper report, int reportType)
		throws java.rmi.RemoteException
	{
		return getNextPage(my_connection, my_rsr, report, reportType);
	}

	/**
		* Next Page of a Report
		*
		* @param connection Specifies the object that provides the connection to
		*             		the server.
		*
		* @param rsr 		AsynchReply from previous request in conversation
		*
		* @param report 	report being run
		*
		* @param reportType output format for report
		*
		*/
	public String getNextPage(
		CRNConnect connection,
		AsynchReply rsr,
		BaseClassWrapper report,
		int reportType)
		throws java.rmi.RemoteException
	{
		if ((connection == null) || (rsr == null))
		{
			return null;
		}

		// sn_dg_sdk_method_reportService_nextPage_start_1
		rsr =
			connection.getReportService().nextPage(
				rsr.getPrimaryRequest(),
				new ParameterValue[] {},
				new Option[] {});
		// sn_dg_sdk_method_reportService_nextPage_end_1

		if (rsr.getStatus() != AsynchReplyStatusEnum.complete)
		{
			// sn_dg_sdk_method_reportService_wait_start_1
			while (rsr.getStatus() != AsynchReplyStatusEnum.complete)
			{
				rsr =
					connection.getReportService().wait(
						rsr.getPrimaryRequest(),
						new ParameterValue[] {},
						new Option[] {});
			}
			// sn_dg_sdk_method_reportService_wait_end_1

			rsr =
				connection.getReportService().getOutput(
					rsr.getPrimaryRequest(),
					new ParameterValue[] {},
					new Option[] {});
		}
		my_rsr = rsr;

		return textOrBinaryOutput(
			connection,
			rsr,
			report.toString(),
			reportType);
	}

	public String previousPage(BaseClassWrapper report, int reportType)
		throws java.rmi.RemoteException
	{
		return getPreviousPage(my_connection, my_rsr, report, reportType);
	}

	/**
		* Previous Page of a Report
		*
		* @param connection Specifies the object that provides the connection to
		*             		the server.
		*
		* @param rsr 		AsynchReply from previous request in conversation
		*
		* @param report 	report being run
		*
		* @param reportType output format for report
		*
		*/
	public String getPreviousPage(
		CRNConnect connection,
		AsynchReply rsr,
		BaseClassWrapper report,
		int reportType)
		throws java.rmi.RemoteException
	{
		if ((connection == null) || (rsr == null))
		{
			return "Invalid parameters passed to getPreviousPage()";
		}

		// sn_dg_sdk_method_reportService_previousPage_start_1
		rsr =
			connection.getReportService().previousPage(
				rsr.getPrimaryRequest(),
				new ParameterValue[] {},
				new Option[] {});
		// sn_dg_sdk_method_reportService_previousPage_end_1

		if (rsr.getStatus() != AsynchReplyStatusEnum.complete)
		{
			while (rsr.getStatus() != AsynchReplyStatusEnum.complete)
			{
				rsr =
					connection.getReportService().wait(
						rsr.getPrimaryRequest(),
						new ParameterValue[] {},
						new Option[] {});
			}
			rsr =
				connection.getReportService().getOutput(
					rsr.getPrimaryRequest(),
					new ParameterValue[] {},
					new Option[] {});
		}
		my_rsr = rsr;
		return textOrBinaryOutput(
			connection,
			rsr,
			report.toString(),
			reportType);
	}

	public String firstPage(BaseClassWrapper report, int reportType)
		throws java.rmi.RemoteException
	{
		return getFirstPage(my_connection, my_rsr, report, reportType);
	}

	/**
		* First Page of a Report
		*
		* @param connection Specifies the object that provides the connection to
		*             		the server.
		*
		* @param rsr 		AsynchReply from previous request in conversation
		*
		* @param report 	report being run
		*
		* @param reportType output format for report
		*
		*/
	public String getFirstPage(
		CRNConnect connection,
		AsynchReply rsr,
		BaseClassWrapper report,
		int reportType)
		throws java.rmi.RemoteException
	{
		if ((connection == null) || (rsr == null))
		{
			return "Invalid parameters passed to getFirstPage()";
		}

		// sn_dg_sdk_method_reportService_firstPage_start_1
		rsr =
			connection.getReportService().firstPage(
				rsr.getPrimaryRequest(),
				new ParameterValue[] {},
				new Option[] {});
		// sn_dg_sdk_method_reportService_firstPage_end_1

		if (rsr.getStatus() != AsynchReplyStatusEnum.complete)
		{
			while (rsr.getStatus() != AsynchReplyStatusEnum.complete)
			{
				rsr =
					connection.getReportService().wait(
						rsr.getPrimaryRequest(),
						new ParameterValue[] {},
						new Option[] {});
			}
			// sn_dg_sdk_method_reportService_getOutput_start_1
			rsr =
				connection.getReportService().getOutput(
					rsr.getPrimaryRequest(),
					new ParameterValue[] {},
					new Option[] {});
			// sn_dg_sdk_method_reportService_getOutput_end_1

		}
		my_rsr = rsr;
		return textOrBinaryOutput(
			connection,
			rsr,
			report.toString(),
			reportType);
	}

	public String lastPage(BaseClassWrapper report, int reportType)
		throws java.rmi.RemoteException
	{
		return getLastPage(my_connection, my_rsr, report, reportType);
	}

	/**
		* Last Page of a Report
		*
		* @param connection Specifies the object that provides the connection to
		*             		the server.
		*
		* @param rsr 		AsynchReply from previous request in conversation
		*
		* @param report 	report being run
		*
		* @param reportType output format for report
		*
		*/
	public String getLastPage(
		CRNConnect connection,
		AsynchReply rsr,
		BaseClassWrapper report,
		int reportType)
		throws java.rmi.RemoteException
	{
		if ((connection == null) || (rsr == null))
		{
			return "Invalid parameters passed to getLastPage()";
		}

		// sn_dg_sdk_method_reportService_lastPage_start_1
		rsr =
			connection.getReportService().lastPage(
				rsr.getPrimaryRequest(),
				new ParameterValue[] {},
				new Option[] {});
		// sn_dg_sdk_method_reportService_lastPage_end_1

		if (rsr.getStatus() != AsynchReplyStatusEnum.complete)
		{
			while (rsr.getStatus() != AsynchReplyStatusEnum.complete)
			{
				rsr =
					connection.getReportService().wait(
						rsr.getPrimaryRequest(),
						new ParameterValue[] {},
						new Option[] {});
			}
			rsr =
				connection.getReportService().getOutput(
					rsr.getPrimaryRequest(),
					new ParameterValue[] {},
					new Option[] {});

		}
		my_rsr = rsr;
		return textOrBinaryOutput(
			connection,
			rsr,
			report.toString(),
			reportType);
	}

	public boolean hasNextPage() throws java.rmi.RemoteException
	{
		return hasNextPage(my_connection, my_rsr);
	}

	/**
		*
		*
		*
		*/
	public boolean hasNextPage(CRNConnect connection, AsynchReply rsr)
		throws java.rmi.RemoteException
	{
		if ((connection == null) || (rsr == null))
		{
			return false;
		}

		int numSecondaryRequests = rsr.getSecondaryRequests().length;
		for (int i = 0; i < numSecondaryRequests; i++)
		{
			if ((rsr.getSecondaryRequests()[i].getName().toString())
				.compareTo("nextPage")
				== 0)
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasPreviousPage() throws java.rmi.RemoteException
	{
		return this.hasPrevioustPage(my_connection, my_rsr);
	}

	/**
		*
		*
		*
		*/
	public boolean hasPrevioustPage(CRNConnect connection, AsynchReply rsr)
		throws java.rmi.RemoteException
	{
		if ((connection == null) || (rsr == null))
		{
			return false;
		}

		int numSecondaryRequests = rsr.getSecondaryRequests().length;
		for (int i = 0; i < numSecondaryRequests; i++)
		{
			if ((rsr.getSecondaryRequests()[i].getName().toString())
				.compareTo("previousPage")
				== 0)
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasFirstPage() throws java.rmi.RemoteException
	{
		return hasFirstPage(my_connection, my_rsr);
	}

	/**
		*
		*
		*
		*/
	public boolean hasFirstPage(CRNConnect connection, AsynchReply rsr)
		throws java.rmi.RemoteException
	{
		if ((connection == null) || (rsr == null))
		{
			return false;
		}

		int numSecondaryRequests = rsr.getSecondaryRequests().length;
		for (int i = 0; i < numSecondaryRequests; i++)
		{
			if ((rsr.getSecondaryRequests()[i].getName().toString())
				.compareTo("firstPage")
				== 0)
			{
				return true;
			}
		}
		return false;
	}

	public boolean hasLastPage() throws java.rmi.RemoteException
	{
		return hasLastPage(my_connection, my_rsr);
	}

	/**
		*
		*
		*
		*/
	public boolean hasLastPage(CRNConnect connection, AsynchReply rsr)
		throws java.rmi.RemoteException
	{
		if ((connection == null) || (rsr == null))
		{
			return false;
		}

		int numSecondaryRequests = rsr.getSecondaryRequests().length;
		for (int i = 0; i < numSecondaryRequests; i++)
		{
			if ((rsr.getSecondaryRequests()[i].getName().toString())
				.compareTo("lastPage")
				== 0)
			{
				return true;
			}
		}
		return false;
	}

	public String[] setFormatByType(int fileType)
	{
		switch (fileType)
		{
			case REP_HTML :
				return (new String[] { "HTML" });

			case REP_XML :
				return (new String[] { "XML" });

			case REP_HTML_FRAG :
				return new String[] { "HTMLFragment" };

			case REP_MHT :
				return new String[] { "MHT" };

			case REP_XLWA :
				return new String[] { "XLWA" };

			case REP_PDF :
				return (new String[] { "PDF" });

			case REP_CSV :
				return (new String[] { "CSV" });

			default :
				System.out.println("Incorrect report type passed.");
				return null;
		}
	}

	public String setFilenameByType(
		String filePath,
		String reportName,
		int fileType)
	{

		switch (fileType)
		{
			case REP_HTML :
				return filePath
					+ System.getProperty("file.separator")
					+ reportName
					+ ++tmpFileCounter
					+ "_output.html";

			case REP_XML :
				return filePath
					+ System.getProperty("file.separator")
					+ reportName
					+ ++tmpFileCounter
					+ "_output.xml";

			case REP_PDF :
				return filePath
					+ System.getProperty("file.separator")
					+ reportName
					+ ++tmpFileCounter
					+ "_output.pdf";

			case REP_CSV :
				return filePath
					+ System.getProperty("file.separator")
					+ reportName
					+ ++tmpFileCounter
					+ "_output.csv";

			case REP_HTML_FRAG :
				return filePath
					+ System.getProperty("file.separator")
					+ reportName
					+ ++tmpFileCounter
					+ "_output.html";

			case REP_MHT :
				return filePath
					+ System.getProperty("file.separator")
					+ reportName
					+ ++tmpFileCounter
					+ "_output.mht";

			case REP_XLWA :
				return filePath
					+ System.getProperty("file.separator")
					+ reportName
					+ ++tmpFileCounter
					+ "_output.xls";

			default :
				System.out.println("Incorrect report type passed");
				return null;
		}
	}

	public File getNewTempFile(
		File oldTempFile,
		String reportName,
		int fileType)
	{
		File newTempFile = null;

		if (oldTempFile != null)
		{
			oldTempFile.delete();
		}
		newTempFile =
			new File(
				setFilenameByType(
					my_connection.getDefaultSavePath(),
					reportName,
					fileType));
		return newTempFile;
	}
}
