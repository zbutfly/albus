package net.butfly.bus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.butfly.albacore.exception.SystemException;
import net.butfly.bus.Constants;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Parsing Objects from XMLs
 * 
 * @version *
 */
@SuppressWarnings("deprecation")
public class XMLUtils {
	private final static String DEFAULT_KEY = "type";
	private final static String DEFAULT_TYPE = "String";
	private final static String INTEGER_TYPE = "Integer";
	private final static String DOUBLE_TYPE = "Double";
	private final static String BIGDECIMAL_TYPE = "BigDecimal";
	private final static String BOOLEAN_TYPE = "boolean";
	private final static String DATE_TYPE = "Date";
	private final static String MAP_TYPE = "java.util.Map";
	private final static String LIST_TYPE = "java.util.List";

	public static Object parse(String filename) throws IllegalAccessException, InvocationTargetException,
			InstantiationException, ClassNotFoundException, IOException, DocumentException {
		InputStream is = new FileInputStream(new File(filename));
		String xmlStr = IOUtils.toString(is);

		Element element = getRootElement(xmlStr);
		String type = element.attributeValue(DEFAULT_KEY);
		Object object = null;

		String value = element.getTextTrim();

		if (type.startsWith(LIST_TYPE)) {
			object = new ArrayList<Object>();
		} else if (type.startsWith(MAP_TYPE)) {
			object = new HashMap<String, Object>();
		} else if (INTEGER_TYPE.equalsIgnoreCase(type)) {
			return new Integer(value);
		} else if (DOUBLE_TYPE.equalsIgnoreCase(type)) {
			return new Double(value);
		} else if (BIGDECIMAL_TYPE.equalsIgnoreCase(type)) {
			return new BigDecimal(value);
		} else if (BOOLEAN_TYPE.equalsIgnoreCase(type)) {
			return new Boolean(value);
		} else if (DATE_TYPE.equalsIgnoreCase(type)) {
			return parseDate(value);
		} else if (!value.equals("")) {
			return new String(value);
		} else {
			object = Class.forName(type).newInstance();
		}
		Object result = getElementList(element, object, type);
		return result;
	}

	private static Element getRootElement(String srcXml) throws DocumentException {
		Document srcdoc = DocumentHelper.parseText(srcXml);
		Element elem = srcdoc.getRootElement();
		return elem;
	}

	@SuppressWarnings("unchecked")
	private static Object getElementList(Element element, Object object, String type) throws IllegalAccessException,
			InvocationTargetException, InstantiationException, ClassNotFoundException {
		List<?> elements = element.elements();
		for (Iterator<?> it = elements.iterator(); it.hasNext();) {
			Element elem = (Element) it.next();
			String elemType = elem.attributeValue(DEFAULT_KEY);
			if (null == elemType) elemType = DEFAULT_TYPE;

			if (DEFAULT_TYPE.equalsIgnoreCase(elemType) || INTEGER_TYPE.equalsIgnoreCase(elemType)
					|| DOUBLE_TYPE.equalsIgnoreCase(elemType) || BIGDECIMAL_TYPE.equalsIgnoreCase(elemType)
					|| BOOLEAN_TYPE.equalsIgnoreCase(elemType) || DATE_TYPE.equalsIgnoreCase(elemType)) {
				if (DATE_TYPE.equals(elemType)) {
					// date/time type
					BeanUtils.setProperty(object, elem.getName(), parseDate(elem.getTextTrim()));
				} else {
					// common primitive type
					BeanUtils.setProperty(object, elem.getName(), elem.getTextTrim());
				}
			} else {
				// non-primitive, create instance before assigning
				Object elemObject = null;
				if (elemType.startsWith(LIST_TYPE)) {
					// List
					elemObject = new ArrayList<Object>();
				} else if (elemType.startsWith(MAP_TYPE)) {
					// Map
					elemObject = new HashMap<Object, Object>();
				} else {
					elemObject = Class.forName(elemType).newInstance();
				}

				// checking before assigning
				if (type.startsWith(LIST_TYPE)) {
					List<Object> l = (List<Object>) object;
					l.add(getElementList(elem, elemObject, elemType));
				} else if (type.startsWith(MAP_TYPE)) {
					Map<String, Object> map = (Map<String, Object>) object;
					map.put(elem.getName(), getElementList(elem, elemObject, elemType));
				} else {
					BeanUtils.setProperty(object, elem.getName(), getElementList(elem, elemObject, elemType));
				}
			}
		}
		return object;
	}

	private static Date parseDate(String str) {
		try {
			str = str.replace(" ", "-");
			str = str.replace(":", "-");
			String[] tempStr = str.split("-");
			int y = Integer.parseInt(tempStr[0]) - 1900;
			int m = Integer.parseInt(tempStr[1]) - 1;
			int d = Integer.parseInt(tempStr[2]);
			int hour = Integer.parseInt(tempStr[3]);
			int min = Integer.parseInt(tempStr[4]);
			int sec = Integer.parseInt(tempStr[5]);
			return new Date(y, m, d, hour, min, sec);
		} catch (Exception e) {
			throw new SystemException(Constants.BusinessError.BUSI_INSTANCE, "日期格式出错", e);
		}
	}

	public static String format(String unformattedXml) {
		try {
			final org.w3c.dom.Document document = parseXmlFile(unformattedXml);

			OutputFormat format = new OutputFormat(document);
			format.setLineWidth(65);
			format.setIndenting(true);
			format.setIndent(4);
			Writer out = new StringWriter();
			XMLSerializer serializer = new XMLSerializer(out, format);
			serializer.serialize(document);

			return out.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static org.w3c.dom.Document parseXmlFile(String in) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(in));
			return db.parse(is);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String prettyFormat(String input, int indent) {
		try {
			Source xmlInput = new StreamSource(new StringReader(input));
			StringWriter stringWriter = new StringWriter();
			StreamResult xmlOutput = new StreamResult(stringWriter);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			// This statement works with JDK 6
			transformerFactory.setAttribute("indent-number", indent);

			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(xmlInput, xmlOutput);
			return xmlOutput.getWriter().toString();
		} catch (Throwable e) {
			// You'll come here if you are using JDK 1.5
			// you are getting an the following exeption
			// java.lang.IllegalArgumentException: Not supported: indent-number
			// Use this code (Set the output property in transformer.
			try {
				Source xmlInput = new StreamSource(new StringReader(input));
				StringWriter stringWriter = new StringWriter();
				StreamResult xmlOutput = new StreamResult(stringWriter);
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
				transformer.transform(xmlInput, xmlOutput);
				return xmlOutput.getWriter().toString();
			} catch (Throwable t) {
				return input;
			}
		}
	}

	public static String prettyFormat(String input) {
		return prettyFormat(input, 4);
	}

	public static Object parseObject(Element element) {
		Object value = parseAttributes(element);
		return null == value ? parseInnerText(element) : value;
	}

	public static Map<String, String> parseAttributes(Element element) {
		if (element == null || element.attributeCount() == 0) return null;
		Map<String, String> rtn = new HashMap<String, String>();
		Iterator<?> it = element.attributeIterator();
		while (it.hasNext()) {
			Attribute at = (Attribute) it.next();
			rtn.put(at.getName(), at.getValue());
		}
		return rtn;
	}

	public static String parseInnerText(Element element) {
		if (null == element) return null;
		String tt = element.getTextTrim();
		return tt != null && !tt.equals("") ? tt : null;
	}

	public static Map<String, Object> parseElementToMap(Element element) {
		if (element == null) return null;
		Map<String, Object> map = new HashMap<String, Object>();

		String txt = element.getTextTrim();
		if (txt != null && !txt.equals("")) safeAdd(map, element.getName(), txt);

		for (int i = 0; i < element.attributeCount(); i++) {
			Attribute att = element.attribute(i);
			safeAdd(map, att.getName(), att.getValue());
		}
		for (int i = 0; i < element.nodeCount(); i++) {
			Element node = (Element) element.node(i);
			safeAdd(map, node.getName(), parseElementToMap(node));
		}
		return map;
	}

	@SuppressWarnings("rawtypes")
	private static Map<String, Object> safeAdd(Map<String, Object> map, String key, Object value) {
		if (!map.containsKey(key)) map.put(key, value);
		else {
			Object old = map.get(key);
			Class<? extends Object> clazz = old.getClass();
			if (clazz.isArray()) {
				Object[] val = Arrays.copyOf((Object[]) old, Array.getLength(old) + 1);
				val[val.length] = value;
				map.put(key, val);
			} else if (Iterable.class.isAssignableFrom(clazz)) {
				List<Object> val = new ArrayList<Object>();
				for (Object obj : (Iterable) old)
					val.add(obj);
				val.add(value);
				map.put(key, val.toArray());
			} else map.put(key, new Object[] { old, value });
		}
		return map;
	}
}
