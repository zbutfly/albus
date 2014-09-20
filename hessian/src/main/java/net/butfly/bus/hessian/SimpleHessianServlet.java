package net.butfly.bus.hessian;

/*
 * Copyright (c) 2001-2008 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.butfly.bus.deploy.BusServlet;
import net.butfly.bus.hessian.HessianSkeleton.Invoker;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.HessianFactory;
import com.caucho.hessian.io.HessianInputFactory;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.services.server.ServiceContext;

public abstract class SimpleHessianServlet extends BusServlet {
	private static final long serialVersionUID = 9188009675865365763L;
	private SerializerFactory _serializerFactory;
	private HessianInputFactory _inputFactory;
	private HessianFactory _hessianFactory;
	private HessianSkeleton _skeleton;
	protected Object _service;

	public SimpleHessianServlet(HessianSkeleton skeleton) {
		super();
		this._skeleton = skeleton;
		this._serializerFactory = new SerializerFactory();
	}

	public void putServiceTarget(Object service) {
		this._service = service;
	}

	@Override
	public String getServletInfo() {
		return "Simple Hessian Servlet";
	}

	public SerializerFactory getSerializerFactory() {
		return _serializerFactory;
	}

	public void setSendCollectionType(boolean sendType) {
		_serializerFactory.setSendCollectionType(sendType);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			this._inputFactory = new HessianInputFactory();
			this._hessianFactory = new HessianFactory();
			if ("false".equals(getInitParameter("send-collection-type"))) setSendCollectionType(false);
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/**
	 * Execute a request. The path-info of the request selects the bean. Once
	 * the bean's selected, it will be applied.
	 */

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ServiceContext.begin(request, response, null, null);
		try {
			response.setContentType("x-application/hessian");
			this.invoke(request.getInputStream(), response.getOutputStream());
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new ServletException(e);
		} finally {
			ServiceContext.end();
		}
	}

	private void invoke(InputStream is, OutputStream os) throws IOException {
		HessianInputFactory.HeaderType header = _inputFactory.readHeader(is);
		AbstractHessianInput in = this.getHessianInput(is, header);
		AbstractHessianOutput out = this.getHessianOutput(os, header);
		if (_serializerFactory != null) {
			in.setSerializerFactory(_serializerFactory);
			out.setSerializerFactory(_serializerFactory);
		}
		HessianExceptionHandler handler = new HessianExceptionHandler(out);
		Invoker invoker = this._skeleton.getInvoker(in, handler);
		in.completeCall();
		in.close();
		if (null != invoker) this.invoke(out, invoker, handler);
	}

	protected void invoke(AbstractHessianOutput out, Invoker invoker, HessianExceptionHandler handler) throws IOException {
		try {
			out.writeReply(invoker.method.invoke(_service, invoker.args));
		} catch (Exception e) {
			Throwable ex = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
			handler.handle("HessianInvokerException", ex.getMessage(), ex);
		} finally {
			this.handleStreamAfterInvoking(out);
		}
	}

	protected void handleStreamAfterInvoking(AbstractHessianOutput out) throws IOException {
		out.close();
	}

	protected AbstractHessianInput getHessianInput(InputStream is, HessianInputFactory.HeaderType header) throws IOException {
		switch (header) {
		case CALL_1_REPLY_1:
			return _hessianFactory.createHessianInput(is);
		case CALL_1_REPLY_2:
			return _hessianFactory.createHessianInput(is);
		case HESSIAN_2:
			AbstractHessianInput in = _hessianFactory.createHessian2Input(is);
			in.readCall();
			return in;
		default:
			throw new IllegalStateException(header + " is an unknown Hessian call");
		}

	}

	protected AbstractHessianOutput getHessianOutput(OutputStream os, HessianInputFactory.HeaderType header) throws IOException {
		switch (header) {
		case CALL_1_REPLY_1:
			return _hessianFactory.createHessianOutput(os);
		case CALL_1_REPLY_2:
			return _hessianFactory.createHessian2Output(os);
		case HESSIAN_2:
			return _hessianFactory.createHessian2Output(os);
		default:
			throw new IllegalStateException(header + " is an unknown Hessian call");
		}
	}

}
