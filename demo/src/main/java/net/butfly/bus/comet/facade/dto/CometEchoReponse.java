package net.butfly.bus.comet.facade.dto;

import java.util.HashMap;
import java.util.Map;

import net.butfly.bus.dto.Response;

public class CometEchoReponse extends Response<CometEchoReponse> {
	private static final long serialVersionUID = 7265884157307146816L;

	private String title;
	private long[] values;
	private Map<String, String> context;

	public CometEchoReponse(String echo, long[] values) {
		this.title = echo;
		this.values = values;
		this.context = new HashMap<String, String>();
		this.context.put("a", "AAA");
		this.context.put("b", "ABB");
		this.context.put("c", "ACC");
		this.context.put("d", "ADD");
	}

	public CometEchoReponse(String echo) {
		this(echo, new long[] { 1, 2, 3, 4, 5 });
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public long[] getValues() {
		return values;
	}

	public void setValues(long[] values) {
		this.values = values;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(title).append("\n").append("values: ");
		for (long i : values)
			sb.append(i).append(",");
		sb.append("\ncontext: ").append(context.toString()).append(".");
		return sb.toString();
	}
}
