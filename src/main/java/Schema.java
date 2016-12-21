import java.util.List;

public class Schema {
	private String name;
	private List<String> keys;
	private List<String> metrics;
	
	public Schema(String name, List<String> keys, List<String> metrics) {
		this.name = name;
		this.keys = keys;
		this.metrics = metrics;
	}

	public String getName() {
		return name;
	}

	public List<String> getKeys() {
		return keys;
	}

	public List<String> getMetrics() {
		return metrics;
	}
	
	
}
