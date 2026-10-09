package com.covercompare.insurer.redbrick;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Reads and writes simple XML messages. DOCTYPE declarations are rejected outright, which blocks
 * XML external entity (XXE) attacks such as an entity that reads {@code file:///etc/passwd}.
 */
final class SecureXml {

	private SecureXml() {
	}

	static Document parse(String xml) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setErrorHandler(null);
			return builder.parse(new InputSource(new StringReader(xml)));
		}
		catch (ParserConfigurationException | SAXException | IOException ex) {
			throw new InvalidXmlException("Request is not well-formed XML", ex);
		}
	}

	static String requiredText(Document document, String elementName) {
		NodeList nodes = document.getDocumentElement().getElementsByTagName(elementName);
		if (nodes.getLength() != 1 || nodes.item(0).getTextContent().isBlank()) {
			throw new InvalidXmlException("Expected exactly one " + elementName + " element", null);
		}
		return nodes.item(0).getTextContent().strip();
	}

	/** Writes {@code <root><key>value</key>...</root>}; values are escaped by the XML serialiser. */
	static String write(String rootName, Map<String, String> children) {
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element root = document.createElement(rootName);
			document.appendChild(root);
			children.forEach((name, value) -> {
				Element child = document.createElement(name);
				child.setTextContent(value);
				root.appendChild(child);
			});
			TransformerFactory factory = TransformerFactory.newInstance();
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
			Transformer transformer = factory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			StringWriter out = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(out));
			return out.toString();
		}
		catch (ParserConfigurationException | TransformerException ex) {
			throw new IllegalStateException("Could not write XML", ex);
		}
	}

	static class InvalidXmlException extends RuntimeException {

		InvalidXmlException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
