# Log Parser

This program will read a log file with the given log file format as shown below, as well as a json file listing schemas that define the aggregation of the metrics by dimensions. It will parse the log file based on the format and output the aggregated data to the <schema name>.tsv based each schema's definition.

## Log format

	<ip_addr> <user_name> <timestamp> <request_method> <request_path> <protocol_version> <user_agent> <status_code> <response_size> <request_time>

Sample log line:

	127.0.0.1 alice [21/Jul/2014:9:55:27 -0800] "GET /home.html HTTP/1.1" "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, likeGecko) Chrome/18.0.1025.5 Safari/535.19" 200 2048 36000


## Schema Json

E.g.

	[
		{
			"name" : "url_stats",
			"keys": ["request_path", "status_code"],
			"metrics": ["hits"]
		},
		{
			"name": "useragent_stats",
			"keys": ["request_path", "user_agent"],
			"metrics": ["hits", "success_hits", "failed_hits", "request_duration"]
		},
		{
			"name": "user_stats",
			"keys": ["ip_addr", "user_name"],
			"metrics": ["success_hits", "failed_hits"]
		}
	]
