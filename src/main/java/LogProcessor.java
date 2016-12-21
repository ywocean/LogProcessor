import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class LogProcessor {

	public void run(String accessLogFilename, String schemaJsonFilename) throws IOException, ParseException {

		List<Schema> schemas = getSchemas(schemaJsonFilename);
		processAccessLog(accessLogFilename, schemas);
		
	}

	private List<Schema> getSchemas(String schemaJsonFilename)
			throws FileNotFoundException, IOException, ParseException {
		List<Schema> schemas = new ArrayList<Schema>();

		JSONParser parser = new JSONParser();
		JSONArray schemaJSONArray = (JSONArray) parser.parse(new FileReader(schemaJsonFilename));
		for(Object object : schemaJSONArray) {
			JSONObject schemaJSONObject = (JSONObject)object;
			String name = (String)schemaJSONObject.get("name");
			List<String> keys = new ArrayList<String>();
			for(Object keyObj : (JSONArray)schemaJSONObject.get("keys")) {
				keys.add((String)keyObj);
			}
			List<String> metrics = new ArrayList<String>();
			for(Object metricObj : (JSONArray)schemaJSONObject.get("metrics")) {
				metrics.add((String)metricObj);
			}
			schemas.add(new Schema(name, keys, metrics));
		}

		return schemas;
	}
	
	private void aggregate(HashMap<String, int[]> aggregatedData, Schema schema, AccessLog log) {
		StringBuilder keyBuilder = new StringBuilder();
		for(String key : schema.getKeys()) {
			keyBuilder.append(log.getValue(key));
			keyBuilder.append('\t');
		}
		String keyValue = keyBuilder.toString();
		int[] metricValues = aggregatedData.get(keyValue);
		if(metricValues == null) {
			metricValues = new int[schema.getMetrics().size()];
			aggregatedData.put(keyValue, metricValues);
		}
		for(int i=0; i<schema.getMetrics().size(); i++) {
			String metric = schema.getMetrics().get(i);
			if(metric.equals("hits"))
				metricValues[i] ++;
			else if(metric.equals("request_duration"))
				metricValues[i] += Integer.parseInt((String)log.getValue("request_duration"));
			else if(metric.equals("success_hits")) {
				int statusCode = Integer.parseInt((String)log.getValue("status_code"));
				if(statusCode/100 == 2 || statusCode/100 == 3){
					metricValues[i] ++;
				}
			}
			else if(metric.equals("failed_hits")) {
				int statusCode = Integer.parseInt((String)log.getValue("status_code"));
				if(statusCode/100 == 4 || statusCode/100 == 5){
					metricValues[i] ++;
				}
			}
		}
	}
	
	private void printToTSV(String schemaName, HashMap<String, int[]> aggregatedData) throws IOException {
		String filename = "/tmp/" + schemaName + ".tsv";
		FileWriter fw = new FileWriter(filename);
		BufferedWriter bw = new BufferedWriter(fw);

		Iterator it = aggregatedData.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			bw.write((String)entry.getKey());
			int[] metricValues = (int[])entry.getValue();
			for(Integer value : metricValues) {
				bw.write('\t');
				bw.write(String.valueOf(value));
			}
			bw.write('\n');
		}
		bw.close();
		fw.close();
	}
	
	private void processAccessLog(String accessLogFilename, List<Schema> schemas) throws IOException {
		HashMap<String, HashMap<String, int[]>> aggregatedDataForAllSchemas = new HashMap<String, HashMap<String, int[]>>();
		for(Schema schema : schemas) {
			aggregatedDataForAllSchemas.put(schema.getName(), new HashMap<String, int[]>());
		}
		
		InputStream fis = new FileInputStream(accessLogFilename);
		InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
		BufferedReader br = new BufferedReader(isr);
		String line;

		while ((line = br.readLine()) != null) {
			AccessLog accessLog = AccessLog.parseLogLine(line);
			for(Schema schema : schemas) {
				aggregate((HashMap<String, int[]>)aggregatedDataForAllSchemas.get(schema.getName()), schema, accessLog);
			}
		}
		
		for(Schema schema : schemas) {
			printToTSV(schema.getName(), (HashMap<String, int[]>)aggregatedDataForAllSchemas.get(schema.getName()));
		}

		br.close();
	}

	public static void main(final String[] args) throws Exception {

		if (args.length < 2) {
			System.out.println("Please pass in access log filename and schema configuration file.");
			return;
		}

		String accessLogFilename = args[0];
		String schemaJsonFilename = args[1];

		new LogProcessor().run(accessLogFilename, schemaJsonFilename);
	}
}
