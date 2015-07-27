package NLP2XML;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
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
 *         Recognition - Threaded Task
 * 
 *         This class is given an input file and possible output and named
 *         entity file. It parses the text and outputs the parsed text in XML
 *         format and the named entities in the named entity file. Also has a -p
 *         option to include a part of speech dictionary
 */
public class NLP2XMLTask implements Runnable {
	private InputStream stream;
	private String taskName;
	private String outputFileName;
	private String nerFileName;
	private String posFileName;

	/**
	 * The main NLP2XML Task initializer. Store the inputs and outputs
	 * 
	 * @param stream
	 *            input stream
	 * @param taskName
	 *            name of task
	 * @param outputFileName
	 *            output file name
	 * @param nerFileName
	 *            named entity file name
	 * @param posFileName
	 *            part of speec file name
	 */
	NLP2XMLTask(InputStream stream, String taskName, String outputFileName,
			String nerFileName, String posFileName) {
		this.stream = stream;
		this.taskName = taskName;
		this.outputFileName = outputFileName;
		this.nerFileName = nerFileName;
		this.posFileName = posFileName;
	}

	/**
	 * Run Task: Read in input into byte array. Check for parts of speech
	 * dictionary. Read the text into a byte array and parse it. Look for named
	 * entities. Ouput XML and named entities to files.
	 */
	@Override
	public void run() {
		byte[] rawText;
		Document doc;
		try {
			rawText = new byte[(int) stream.available()];
			DataInputStream dataIs = new DataInputStream(stream);
			dataIs.readFully(rawText);
			doc = parseData(rawText);
			NamedEntityRecognizer ner = new NamedEntityRecognizer(posFileName);
			ner.recognize(doc);
			outputXML(outputFileName, doc);
			outputNER(nerFileName, doc);
			System.out.format("Task %s Done\n", taskName);
		} catch (IOException e) {
			System.err.format("ERROR: NLP2XMLTask run IOException=%s\n",
					e.getMessage());
			e.printStackTrace();
		}
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
	private Document parseData(byte[] rawText) {
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
	void outputXML(String fileName, Document doc) {
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
