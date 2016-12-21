import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessLog {
	private HashMap<String, Object> data;
	
	public AccessLog(String ipAddress, String userName, String timestamp, String requestMethod, String requestPath,
			String protocolVersion, String userAgent, String statusCode, String responseSize, String requestTime) {
		data = new HashMap<String, Object>();
		data.put("ip_addr", ipAddress);
		data.put("user_name", userName);
		data.put("timestamp", timestamp);
		data.put("request_method", requestMethod);
		data.put("request_path", requestPath);
		data.put("protocol_version", protocolVersion);
		data.put("user_agent", userAgent);
		data.put("status_code", statusCode);
		data.put("response_size", responseSize);
		data.put("request_duration", requestTime);
	}
	
	public Object getValue(String key) {
		return data.get(key);
	}
	
	// Example Apache log line:
	// 127.0.0.1 alice [21/Jul/2014:9:55:27 -0800] "GET /home.html HTTP/1.1" "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, likeGecko) Chrome/18.0.1025.5 Safari/535.19" 200 2048 36000
	private static String LOG_ENTRY_PATTERN =
	// 1:IP 2:user 3:date time 4:method 5:request path 6:protocol 7:user agent 8:status code 9:size 10:duration
	"^(\\S+) (\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(\\S+) (\\S+) (\\S+)\" \"([^\"]*)\" (\\d{3}) (\\d+) (\\d+)";
	private static Pattern PATTERN = Pattern.compile(LOG_ENTRY_PATTERN);
	
	public static void constructLogEntryPattern(String[] logFormat) {
		StringBuilder builder = new StringBuilder();
		boolean requestMethodQuoteStarted = false;
		boolean requestMethodQuoteEnded = false;
		String prefix = "^";
		for(String format : logFormat) {
			// ip_addr,user_name,timestamp,request_method,request_path,protocol_version,user_agent,status_code,response_size,request_duration
			if(format.equals("request_method")
					|| format.equals("request_path")
					|| format.equals("protocol_version")){
				builder.append(prefix);
				if(!requestMethodQuoteStarted){
					builder.append("\"");
					requestMethodQuoteStarted = true;
				}
				builder.append("(\\S+)");
			}
			else{ 
				if(requestMethodQuoteStarted && !requestMethodQuoteEnded){
					builder.append("\"");
					requestMethodQuoteEnded = true;
				}
				builder.append(prefix);
				if (format.equals("ip_addr") || format.equals("user_name"))
					builder.append("(\\S+)");
				else if(format.equals("timestamp"))
					builder.append("\\[([\\w:/]+\\s[+\\-]\\d{4})\\]");
				else if(format.equals("user_agent"))
					builder.append("\"([^\"]*)\"");
				else if(format.equals("status_code"))
					builder.append("(\\d{3})");
				else if(format.equals("response_size"))
					builder.append("(\\d+)");
				else if(format.equals("request_duration"))
					builder.append("(\\d+)");
			}
			prefix = " ";
		}
		LOG_ENTRY_PATTERN = builder.toString();
		PATTERN = Pattern.compile(LOG_ENTRY_PATTERN);
	}

	public static AccessLog parseLogLine(String log) {
		Matcher m = PATTERN.matcher(log);
		if (!m.find()) {
			System.out.println("Cannot parse log: " + log);
			throw new RuntimeException("Error parsing logline");
		}

		return new AccessLog(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5), m.group(6), m.group(7),
				m.group(8), m.group(9), m.group(10));
	}
}
