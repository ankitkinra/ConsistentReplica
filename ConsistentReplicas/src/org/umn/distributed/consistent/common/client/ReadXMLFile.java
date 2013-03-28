package org.umn.distributed.consistent.common.client;

/**
 * http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
 */
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;

public class ReadXMLFile {

	public static void main(String argv[]) {

		try {

			File fXmlFile = new File("test1.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			// optional, but recommended
			// read this -
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			System.out.println("Root element :"
					+ doc.getDocumentElement().getNodeName());

			NodeList roundsList = doc.getElementsByTagName("rounds");

			System.out.println("----------------------------");

			for (int temp = 0; temp < roundsList.getLength(); temp++) {

				Node roundDetails = roundsList.item(temp);

				System.out.println("\nCurrent Element :" + roundDetails.getNodeName());

				if (roundDetails.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) roundDetails;

					System.out.println("round : "
							+ eElement.getAttribute("name"));
					System.out.println("First Name : "
							+ eElement.getElementsByTagName("")
									.item(0).getTextContent());
					System.out.println("Last Name : "
							+ eElement.getElementsByTagName("lastname").item(0)
									.getTextContent());
					System.out.println("Nick Name : "
							+ eElement.getElementsByTagName("nickname").item(0)
									.getTextContent());
					System.out.println("Salary : "
							+ eElement.getElementsByTagName("salary").item(0)
									.getTextContent());

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}