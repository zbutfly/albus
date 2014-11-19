package net.butfly.bus.serialize;

import org.apache.http.entity.ContentType;

public abstract class HTTPStreamingSupport {
	public static final ContentType DEFAULT_CONTENT_TYPE = ContentType.TEXT_PLAIN;

	public boolean supportHTTPStream() {
		return true;
	}

	public ContentType[] getSupportedContentTypes() {
		return new ContentType[] { DEFAULT_CONTENT_TYPE };
	}

	public ContentType getOutputContentType() {
		return this.getSupportedContentTypes()[0];
	};
}
