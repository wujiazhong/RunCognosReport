package Common;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;

import org.apache.axis.AxisFault;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CognosBIException
{
	private AxisFault _exception = null;

	/**
	 * Create a CognosBIException object.
	 * 
	 * @param ex An AxisFault thrown by an IBM Cognos method call.
	 */
	public CognosBIException(AxisFault ex)
	{
		_exception = ex;
	}

	/**
	 * Return the exception message.
	 * 
	 * @return The exception's message string.
	 */
	public String getMessage()
	{
		return _exception.getMessage();
	}

	/**
	 * Return the exception severity.
	 * 
	 * @return The exception severity string.
	 */
	public String getSeverity()
	{
		try
		{
			Node n =
				getSingleNode(
					"(//*[namespace-uri()=\"http://developer.cognos.com/schemas/bibus/3/\" and local-name()=\"severity\"])[1]");
			
			return new String(n.getFirstChild().getNodeValue());
		}
		catch (Exception ex)
		{
			return new String("");
		}
	}

	/**
	 * Return the exception errorCode.
	 * 
	 * @return The exception errorCode string.
	 */
	public String getErrorCode()
	{
		try
		{
			Node n =
				getSingleNode(
					"(//*[namespace-uri()=\"http://developer.cognos.com/schemas/bibus/3/\" and local-name()=\"errorCode\"])[1]");
			
			return new String(n.getFirstChild().getNodeValue());
		}
		catch (Exception ex)
		{
			return new String("");
		}
	}

	/**
	 * Return the exception's messageStrings.
	 * 
	 * @return The exception messageString array of strings.
	 */
	public String[] getDetails()
	{
		try
		{
			NodeList nodes =
				getNodeList(
					"//*[namespace-uri()=\"http://developer.cognos.com/schemas/bibus/3/\" and local-name()=\"messageString\"]");

			String retval[] = new String[nodes.getLength()];
			for (int idx = 0; idx < nodes.getLength(); idx++)
			{
				retval[idx] =
					new String(nodes.item(idx).getFirstChild().getNodeValue());
			}

			return retval;
		}
		catch (Exception ex)
		{
			return new String[] { "" };
		}
	}

	/**
	 * Convert this CognosBIException into a string for printing.
	 * 
	 * @return A string representation of the CognosBIException.
	 */
	public String toString()
	{
		StringBuffer str = new StringBuffer();

		str.append("Message:   ").append(getMessage()).append("\n");
		str.append("Severity:  ").append(getSeverity()).append("\n");
		str.append("ErrorCode: ").append(getErrorCode()).append("\n");
		str.append("Details:\n");
		String details[] = getDetails();
		for (int i = 0; i < details.length; i++)
		{
			str.append("\t").append(details[i]).append("\n");
		}

		return str.toString();
	}
	
	/**
	 * Return a Node from the exception based on supplied search path intended to return a single Node.
	 *
	 * @param searchString An XPath expression 
	 * 
	 * @return Node n.
	 */
	public Node getSingleNode(String searchString) throws XPathFactoryConfigurationException, XPathExpressionException
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
			
		XPathExpression xPathExpr = xpath.compile(searchString);
			
		Node n = (Node)xPathExpr.evaluate(_exception.getFaultDetails()[0].getParentNode(), XPathConstants.NODE);
			
		return n;
		
	}
	
	/**
	 * Return a NodeList from the exception based on supplied search path intended to return one or more Nodes.
	 *
	 * @param searchString An XPath expression 
	 * 
	 * @return NodeList nl.
	 */
	public NodeList getNodeList(String searchString) throws XPathFactoryConfigurationException, XPathExpressionException
	{
		XPath xpath = XPathFactory.newInstance().newXPath();
			
		XPathExpression xPathExpr = xpath.compile(searchString);
			
		NodeList nl = (NodeList)xPathExpr.evaluate(_exception.getFaultDetails()[0].getParentNode(), XPathConstants.NODESET);
			
		return nl;
		
	}

	/**
	 * Convert a SoapException into a CognosBIException string.
	 * 
	 * This is the same as creating a CognosBIException and calling
	 * its ToString() method.
	 * 
	 * @param ex The AxisFault to format.
	 * @return A string representation.
	 */
	static public String convertToString(AxisFault ex)
	{
		CognosBIException exception = new CognosBIException(ex);
		return exception.toString();
	}
}
// sn_dg_sdk_exception_end_1
