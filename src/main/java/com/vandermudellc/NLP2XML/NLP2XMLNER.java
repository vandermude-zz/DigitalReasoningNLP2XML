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
import org.w3c.dom.NodeList;

/**
 * 
 * @author Antony Van der Mude
 * 
 *         Natural Language Processing To XML format with Named Entity
 *         Recognition
 * 
 *         This class is given an input file and possible output and named
 *         entity file. It parses the text and outputs the parsed text in XML
 *         format and the named entities in the named entity file. Also has a -p
 *         option to include a part of speech dictionary
 */
public class NLP2XMLNER {

	/**
	 * Main program: Read in input file name and possible output file name and
	 * named entity file name. Check for parts of speech dictionary. Read the
	 * text into a byte array and parse it. Look for named entities.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		byte[] rawText;
		Document doc;
		String inputFileName = null;
		String outputFileName = null;
		String nerFileName = null;
		String posFileName = null;
		// No fancy option stuff, just looking for -p or --pos options
		if (args.length == 0) {
			System.err.println("ERROR: Input file name not given");
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-p") || args[i].equals("--pos")) {
				i++;
				posFileName = args[i];
				System.out.format("PartsOfSpeech: %s\n", posFileName);
			} else if (inputFileName == null) {
				inputFileName = args[i];
			} else if (outputFileName == null) {
				outputFileName = args[i];
			} else {
				nerFileName = args[i];
			}
		}
		System.out.format("Input: %s\n", inputFileName);
		rawText = readRawData(inputFileName);
		doc = parseData(rawText);
		NamedEntityRecognizer ner = new NamedEntityRecognizer(posFileName);
		ner.recognize(doc);
		if (outputFileName != null) {
			System.out.format("Output: %s\n", outputFileName);
		}
		outputXML(outputFileName, doc);
		if (nerFileName != null) {
			System.out.format("NER: %s\n", nerFileName);
		}
		outputNER(nerFileName, doc);
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
		PrintStream out = System.out;
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

	/**
	 * Output named entities to file or stdout if no name given
	 * 
	 * @param fileName
	 *            output file name
	 * @param doc
	 *            XML document
	 */
	static void outputNER(String fileName, Document doc) {
		PrintStream out = System.out;
		if (fileName != null) {
			try {
				out = new PrintStream(fileName);
			} catch (FileNotFoundException e) {
				System.err.format(
						"ERROR: OutputNER FileNotFoundException=%s\n",
						e.getMessage());
				e.printStackTrace();
			}
		}
		NodeList nodeList = doc.getElementsByTagName("Word");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element node = (Element) nodeList.item(i);
			if (node.hasAttribute("NER")) {
				out.print(node.getAttribute("text") + "\n");
			}
		}
		out.close();
	}

}
