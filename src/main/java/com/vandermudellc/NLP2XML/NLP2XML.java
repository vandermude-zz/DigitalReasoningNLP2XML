package NLP2XML;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 
 * @author Antony Van der Mude
 * 
 *         Natural Language Processing To XML format
 * 
 *         This class is given an input file and possible output file. It parses
 *         the text and outputs the parsed text in XML format
 */
public class NLP2XML {

	/**
	 * Main program: Read in input file name and possible output file name Read
	 * the text into a byte array and parse it
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		byte[] rawText;
		Document doc;
		if (args.length == 0) {
			System.err.println("ERROR: Input file name not given");
			System.exit(1);
		}
		System.out.format("Input: %s\n", args[0]);
		rawText = readRawData(args[0]);
		doc = parseData(rawText);
		if (args.length == 1) {
			outputXML(null, doc);
		} else {
			System.out.format("Output: %s\n", args[1]);
			outputXML(args[1], doc);
		}
		System.out.println("Done");
	}

	/**
	 * Open text file and read into byte array
	 * 
	 * @param fileName
	 *            input file name
	 * @return byte array
	 */
	static byte[] readRawData(String fileName) {
		File inputFile = new File(fileName);
		if (!inputFile.canRead()) {
			System.err.format("ERROR: Can't read input file %s", fileName);
			System.exit(1);
		}
		byte[] inputBytes = new byte[(int) inputFile.length()];
		try {
			DataInputStream inputIs = new DataInputStream(new FileInputStream(
					fileName));
			inputIs.readFully(inputBytes);
			inputIs.close();
		} catch (IOException e) {
			System.err.format("ERROR: ReadRawData IOException=%s\n",
					e.getMessage());
			e.printStackTrace();
		}
		return inputBytes;
	}

	/**
	 * Parse byte array into XML document. This has two steps: first tokenize
	 * raw data into list of String tokens. Next parse the text, into paragraphs
	 * and sentences and other data structures
	 * 
	 * @param rawText
	 *            byte array
	 * @return XML document
	 */
	private static Document parseData(byte[] rawText) {
		Document doc = null;
		DocumentBuilder docBuilder;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("Document");
			doc.appendChild(rootElement);
			Tokenizer tokenizer = new Tokenizer();
			List<String> list = tokenizer.makeTokens(rawText);
			Parser parser = new Parser();
			parser.parse(doc, rootElement, list);
		} catch (ParserConfigurationException e) {
			System.err.format(
					"ERROR: Process ParserConfigurationException=%s\n",
					e.getMessage());
			e.printStackTrace();
		}
		return doc;
	}

	/**
	 * Output XML document to file or stdout if no name given
	 * 
	 * @param fileName
	 *            output file name
	 * @param doc
	 *            XML document
	 */
	static void outputXML(String fileName, Document doc) {
		PrintStream out;
		out = System.out;
		if (fileName != null) {
			try {
				out = new PrintStream(fileName);
			} catch (FileNotFoundException e) {
				System.err.format(
						"ERROR: OutputXML FileNotFoundException=%s\n",
						e.getMessage());
				e.printStackTrace();
			}
		}
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			System.err.format(
					"ERROR: OutputXML TransformerConfigurationException=%s\n",
					e.getMessage());
			e.printStackTrace();
		} catch (TransformerException e) {
			System.err.format("ERROR: OutputXML TransformerException=%s\n",
					e.getMessage());
			e.printStackTrace();
		}
	}

}
