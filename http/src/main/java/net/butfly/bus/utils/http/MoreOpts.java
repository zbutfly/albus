package net.butfly.bus.utils.http;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.butfly.albacore.utils.Reflections;
import net.butfly.albacore.utils.async.Options;
import net.butfly.albacore.utils.async.Opts;

import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;

public class MoreOpts extends Opts {
	@Override
	public String format(Options options) {
		String mode = null;
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		for (Field f : Options.class.getDeclaredFields())
			try {
				f.setAccessible(true);
				if (f.getName().equals("mode")) mode = f.get(options).toString();
				else params.add(new BasicNameValuePair(f.getName(), f.get(options).toString()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		ContentType ct = ContentType.create(mode, params.toArray(new NameValuePair[params.size()]));
		return ct.toString();
	}

	@Override
	public Options parse(String contentType) {
		Options opts = new Options();
		ContentType ct = ContentType.parse(contentType);
		mode(opts, ct.getMimeType());
		String v;
		v = ct.getParameter("timeout");
		Reflections.set(opts, "timeout", Long.parseLong(v));
		v = ct.getParameter("unblock");
		Reflections.set(opts, "unblock", Boolean.parseBoolean(ct.getParameter("unblock")));
		v = ct.getParameter("repeat");
		Reflections.set(opts, "repeat", Integer.parseInt(ct.getParameter("repeat")));
		v = ct.getParameter("retry");
		Reflections.set(opts, "retry", Integer.parseInt(ct.getParameter("retry")));
		v = ct.getParameter("concurrence");
		Reflections.set(opts, "concurrence", Integer.parseInt(ct.getParameter("concurrence")));
		v = ct.getParameter("interval");
		Reflections.set(opts, "interval", Long.parseLong(ct.getParameter("interval")));
		return opts;
	}

	public void main(String[] args) {
		Options op = new Options().continuous(50).concurrence(5);
		assert (op.equals(parse(format(op))));
	}
}
