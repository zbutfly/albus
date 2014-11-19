package net.butfly.bus.hessian;

import java.io.IOException;

import com.caucho.hessian.io.AbstractHessianOutput;

public class HessianExceptionHandler {
	private AbstractHessianOutput out;

	public HessianExceptionHandler(AbstractHessianOutput out) {
		super();
		this.out = out;
	}

	public void handle(String title, String message, Throwable cause) {
		try {
			out.writeFault(title, escapeMessage(message), cause);
		} catch (IOException e1) {}
	}

	private String escapeMessage(String msg) {
		if (msg == null) return null;

		StringBuilder sb = new StringBuilder();

		int length = msg.length();
		for (int i = 0; i < length; i++) {
			char ch = msg.charAt(i);

			switch (ch) {
			case '<':
				sb.append("&lt;");
				break;
			case '>':
				sb.append("&gt;");
				break;
			case 0x0:
				sb.append("&#00;");
				break;
			case '&':
				sb.append("&amp;");
				break;
			default:
				sb.append(ch);
				break;
			}
		}

		return sb.toString();
	}
}
