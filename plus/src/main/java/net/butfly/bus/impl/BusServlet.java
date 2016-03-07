package net.butfly.bus.impl;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class BusServlet extends HttpServlet implements Servlet {
	private static final long serialVersionUID = -422605653544749492L;

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		super.service(request, response);
	}
}
