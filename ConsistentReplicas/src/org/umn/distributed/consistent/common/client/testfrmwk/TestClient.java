package org.umn.distributed.consistent.common.client.testfrmwk;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.umn.distributed.consistent.common.LoggingUtils;

import com.thoughtworks.xstream.XStream;

public class TestClient {
	private Logger logger = Logger.getLogger(TestClient.class);
	private ArrayList<Round> rounds = new ArrayList<Round>();
	private HashMap<Integer, ArticlesPublished> articlesPublishedRecord = new HashMap<Integer, ArticlesPublished>();
	
	
	
	public static void main(String[] args) {
		String xmlTestFile = args[0];
		Round newRound = getParsedRound(xmlTestFile);
		TestClient tc = new TestClient();
		
		
		
		System.out.println(newRound);
	}

	private static Round getParsedRound(String filePath) {
		BufferedReader in = null;
		StringBuilder sb = new StringBuilder();
		try {
			in = new BufferedReader(new FileReader(filePath));
			String str = null;
			while ((str = in.readLine()) != null) {
				sb.append(str);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {

				}
			}
		}
		XStream xstream = new XStream();
		xstream.alias("round", Round.class);
		xstream.alias("operation", Operation.class);
		
		Round newRound = (Round) xstream.fromXML(sb.toString());
		
		return newRound;
	}
}
