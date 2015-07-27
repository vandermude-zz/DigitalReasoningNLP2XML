package NLP2XML;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Antony Van der Mude
 * 
 *         Rudimentary parser. Breaks text into paragraphs and sentences. Words
 *         are listed in sentences. If words are enclosed with (word word word),
 *         [word word word], {word word word}, or "word word word", or 'word
 *         word word', the parser aggregates the words also
 */
public class Parser {
	Map<String, String> punctuation = new HashMap<String, String>();
	Map<String, String> grouping = new HashMap<String, String>();

	/**
	 * Initializer sets up punctuation and grouping maps. NOTE: This only works
	 * for ASCII text. No allowance is made for Unicode
	 */
	Parser() {
		// Multiple punctuation marks tokenized
		punctuation.put("...", "Ellipses");
		punctuation.put("--", "EmDash");
		// The ASCII table - control characters
		punctuation.put("\n", "CarriageReturn");
		punctuation.put("\f", "FormFeed");
		// The ASCII table - printable characters (not alphanumeric)
		punctuation.put("!", "ExclamationMark");
		punctuation.put("\"", "DoubleQuotes");
		punctuation.put("#", "Number");
		punctuation.put("$", "Dollar");
		punctuation.put("%", "Percent");
		punctuation.put("&", "Ampersand");
		punctuation.put("'", "SingleQuote");
		punctuation.put("(", "OpenParenthesis");
		punctuation.put(")", "CloseParenthesis");
		punctuation.put("*", "Asterisk");
		punctuation.put("+", "Plus");
		punctuation.put(",", "Comma");
		punctuation.put("-", "Hyphen");
		punctuation.put(".", "Period");
		punctuation.put("/", "Slash");
		punctuation.put(":", "Colon");
		punctuation.put(";", "Semicolon");
		punctuation.put("<", "LessThan");
		punctuation.put("=", "Equals");
		punctuation.put(">", "GreaterThan");
		punctuation.put("?", "QuestionMark");
		punctuation.put("@", "AtSymbol");
		punctuation.put("[", "OpeningBracket");
		punctuation.put("\\", "Backslash");
		punctuation.put("]", "ClosingBracket");
		punctuation.put("^", "Caret");
		punctuation.put("_", "Underscore");
		punctuation.put("`", "GraveAccent");
		punctuation.put("{", "OpeningBrace");
		punctuation.put("|", "VerticalBar");
		punctuation.put("}", "ClosingBrace");
		punctuation.put("~", "Tilde");
		// Grouping punctuation
		grouping.put("DoubleQuotes", "DoubleQuotes");
		grouping.put("SingleQuote", "SingleQuote");
		grouping.put("OpenParenthesis", "CloseParenthesis");
		grouping.put("OpeningBracket", "ClosingBracket");
		grouping.put("OpeningBrace", "ClosingBrace");
	}

	/**
	 * Main parse routine. Sets up first paragraph and sentence then parses each
	 * token in turn
	 * 
	 * @param doc
	 *            XML document
	 * @param rootElement
	 *            root node of document
	 * @param list
	 *            list of tokens
	 */
	void parse(Document doc, Element rootElement, List<String> list) {
		Element paragraph = doc.createElement("Paragraph");
		rootElement.appendChild(paragraph);
		Element sentence = doc.createElement("Sentence");
		paragraph.appendChild(sentence);
		Element element = sentence;
		for (String s : list) {
			element = parseToken(doc, element, s);
		}
	}

	/**
	 * Parse each token. convert punctuation into keyword. Do the following:
	 * Parse paragraphs (assumed to be separated by carriage returns). Parse
	 * sentences (ending punctuation - period, question mark or exclamation
	 * moved to end of sentence in tokenizer). Groups words in parentheses or
	 * quotes.
	 * 
	 * @param doc
	 *            XML document
	 * @param element
	 *            current node in document
	 * @param string
	 *            token (word or punctuation)
	 * @return
	 */
	Element parseToken(Document doc, Element element, String string) {
		String punctuationName = "";
		if (punctuation.containsKey(string)) {
			punctuationName = punctuation.get(string);
		}
		if (punctuationName.equals("CarriageReturn")
				|| punctuationName.equals("FormFeed")) {
			// Remove empty sentence
			Node node = getParentToken((Node) element, "Paragraph");
			if (element.getNodeName().equals("Sentence")
					&& !element.hasChildNodes()) {
				node.removeChild(element);
			}
			Node parent = node.getParentNode();
			Element paragraph = doc.createElement("Paragraph");
			parent.appendChild(paragraph);
			Element sentence = doc.createElement("Sentence");
			paragraph.appendChild(sentence);
			element = sentence;
		} else if (punctuationName.equals("Period")
				|| punctuationName.equals("ExclamationMark")
				|| punctuationName.equals("QuestionMark")) {
			makeWordOrPunct(doc, element, string, false);
			Node node = getParentToken((Node) element, "Sentence");
			Element parent = (Element) node.getParentNode();
			Element sentence = doc.createElement("Sentence");
			parent.appendChild(sentence);
			element = sentence;
		} else if (grouping.containsKey(punctuationName)
				&& punctuationName.equals(grouping.get(punctuationName))) {
			// grouping with the same token, e.g. "this is a quote"
			Node node = getParentToken((Node) element, punctuationName);
			if (node == null) {
				Element group = doc.createElement(punctuationName);
				element.appendChild(group);
				element = group;
			} else {
				element = (Element) node.getParentNode();
			}
		} else if (grouping.containsKey(punctuationName)) {
			// start grouping with different tokens, e.g. '(' in (a
			// parenthetical remark)
			Element group = doc.createElement(punctuationName);
			element.appendChild(group);
			element = group;
		} else if (grouping.containsValue(punctuationName)) {
			// end grouping with different tokens, e.g. ')' in (a parenthetical
			// remark)
			for (Entry<String, String> entry : grouping.entrySet()) {
				if (punctuationName.equals(entry.getValue())) {
					punctuationName = entry.getKey();
				}
			}
			Node node = getParentToken((Node) element, punctuationName);
			if (node != null) {
				element = (Element) node.getParentNode();
			}
		} else {
			makeWordOrPunct(doc, element, string,
					Character.isLetterOrDigit(string.charAt(0)));
		}
		return element;
	}

	/**
	 * Make word or punctuation keyword into an XML node
	 * 
	 * @param doc
	 *            XML document
	 * @param element
	 *            current node in document
	 * @param string
	 *            token (word or punctuation)
	 * @param isWord
	 *            true is word, false if punctuation
	 */
	void makeWordOrPunct(Document doc, Element element, String string,
			Boolean isWord) {
		String type;
		String attribute;
		String value;
		if (isWord) {
			type = "Word";
			attribute = "text";
			value = string;
		} else {
			type = "Punctuation";
			attribute = "type";
			if (punctuation.containsKey(string)) {
				value = punctuation.get(string);
			} else {
				value = "UNKNOWN:"
						+ Integer.toHexString((int) string.charAt(0));
			}
		}
		Element newElement = doc.createElement(type);
		element.appendChild(newElement);
		Attr attr = doc.createAttribute(attribute);
		attr.setValue(value);
		newElement.setAttributeNode(attr);
	}

	/**
	 * Search up document tree for a parent with given node name
	 * 
	 * @param node
	 *            base node
	 * @param token
	 *            node name searching for
	 * @return parent if found else nil
	 */
	Node getParentToken(Node node, String token) {
		while (node != null && !node.getNodeName().equals(token)) {
			node = node.getParentNode();
		}
		return node;
	}
}
