package net.butfly.bus.comet.facade.dto;

import net.butfly.bus.dto.Request;

public class CometEchoRequest extends Request<CometEchoRequest> {
	private static final long serialVersionUID = -1427669599681580376L;
	private String value;
	private long[] values;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public long[] getValues() {
		return values;
	}

	public void setValues(long[] values) {
		this.values = values;
	}
}
