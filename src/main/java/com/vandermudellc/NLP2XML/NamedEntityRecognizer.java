package NLP2XML;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 
 * @author Antony Van der Mude
 * 
 *         Named Entity Recognizer
 * 
 *         The instructions said: "Write your solution in the Java programming
 *         language, using only the standard libraries. Do not use any
 *         third-­party libraries for natural language processing"
 * 
 *         But that does not on the face of it eliminate an open-source
 *         dictionary.
 * 
 *         This code is implemented two ways
 * 
 *         (1) Without a dictionary - use a best guess of what are named
 *         entities
 * 
 *         (2) With a dictionary of parts of speech - use that to improve the
 *         algorithm in part 1.
 * 
 *         This code will produce a file NER.txt with the list of most likely
 *         named entities. The XML document will group words into named
 *         entities. It will also score each word on how likely it is to be a
 *         named entity. The determination of named entities will be based on
 *         likelihood scores, depending on a variety of features. The naive
 *         algorithm (1) contains the following parameters:
 * 
 *         a. Is the word capitalized?
 * 
 *         b. Does the word begin with a digit?
 * 
 *         c. Does the word or words follow "a" or "the"?
 * 
 *         d. How long is the word? Longer words tend to be named entities.
 * 
 *         e. If two or more words with high scores follow one another, they
 *         will be grouped into a multiword named entity.
 * 
 *         Normally, using scores on a vector of properties like this would be
 *         calculated by a probabilistic analysis on a tagged corpus of data. I
 *         don't have that, so I'll use my best guess.
 * 
 *         It would be nice to:
 * 
 *         f. Flag words surrounded by single or double quotes if the phrase is
 *         short.
 * 
 *         g. If a Named Entity is identified, go through the whole file and
 *         flag all other occurrences
 * 
 *         But I've run out of time.
 * 
 *         Here is the legend for the dictionary of Parts of Speech.
 * 
 *         Nouns, noun phrases and nominatives will be considered Named Entities
 * 
 *         Noun N
 * 
 *         Plural p
 * 
 *         Noun Phrase h
 * 
 *         Verb (usu participle) V
 * 
 *         Verb (transitive) t
 * 
 *         Verb (intransitive) i
 * 
 *         Adjective A
 * 
 *         Adverb v
 * 
 *         Conjunction C
 * 
 *         Preposition P
 * 
 *         Interjection !
 * 
 *         Pronoun r
 * 
 *         Definite Article D
 * 
 *         Indefinite Article I
 * 
 *         Nominative o
 */
public class NamedEntityRecognizer {
	Map<String, String> partsOfSpeech = new HashMap<String, String>();
	static double CUTOFF = 0.15;
	static final DecimalFormat df = new DecimalFormat("#.000");

	/**
	 * Initialization reads the Parts of Speech file, if it is given Otherwise
	 * partsOfSpeech is empty
	 * 
	 * @param posFile
	 *            Parts of Speech file name
	 */
	NamedEntityRecognizer(String posFile) {
		if (posFile != null) {
			FileReader fileReader;
			try {
				fileReader = new FileReader(posFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line = null;
				// parts of speech delimited by ASCII value 0xD7
				String delimiter = Character.toString((char) 0xD7);
				while ((line = bufferedReader.readLine()) != null) {
					String[] data = line.split(delimiter);
					partsOfSpeech.put(data[0], data[1]);
				}
				bufferedReader.close();
			} catch (FileNotFoundException e) {
				System.err.format("ERROR: NamedEntityRecognizer FileNotFoundException=%s\n",
						e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.format("ERROR: NamedEntityRecognizer IOException=%s\n",
						e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Traverse the XML tree and score the words
	 * 
	 * @param doc
	 *            XML document
	 */
	public void recognize(Document doc) {
		dfsNER(doc, null);
	}

	/**
	 * Recursive Depth First Search of document
	 * 
	 * @param parentNode
	 *            Current node being traversed
	 * @param prevNode
	 *            Node previous to this in transversal (NOTE: may not be
	 *            necessary as parameter)
	 */
	void dfsNER(Node parentNode, Node prevNode) {
		prevNode = parentNode;
		Node nextNode = null;
		for (Node node = parentNode.getFirstChild(); node != null; node = nextNode) {
			nextNode = node.getNextSibling();
			if (node.getNodeName().equals("Word")) {
				Element prevWord = null;
				if (prevNode.getNodeName().equals("Word")) {
					prevWord = (Element) prevNode;
				}
				NERScore(node, prevWord);
				// e. If two or more words with high scores follow one another,
				// they will be
				// grouped into a multiword named entity.
				// Don't move the prevNode if two nodes are combined.
				if (prevWord != null && prevWord.hasAttribute("NER")
						&& ((Element) node).hasAttribute("NER")) {
					prevWord.setAttribute("text", prevWord.getAttribute("text")
							+ " " + ((Element) node).getAttribute("text"));
					parentNode.removeChild(node);
				} else {
					prevNode = node;
				}
			} else {
				dfsNER(node, prevNode);
			}
		}
	}

	/**
	 * Score each word in context.
	 * 
	 * @param word
	 *            current word
	 * @param prevWord
	 *            previous word
	 */
	void NERScore(Node word, Node prevWord) {
		double[] scoreLength = { 0.0, 0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.6,
				0.7 };
		double score = 1.0;
		String text = ((Element) word).getAttribute("text");
		String prevText = "";
		if (prevWord != null) {
			prevText = ((Element) prevWord).getAttribute("text");
		}
		// a. Is the word capitalized?
		// b. Does the word begin with a digit?
		if (Character.isUpperCase(text.charAt(0))
				|| Character.isDigit(text.charAt(0))) {
			score *= 0.9;
		} else {
			score *= 0.6;
		}
		// c. Does the word or words follow "a" or "the"?
		if (prevText.equals("a") || prevText.equals("A")
				|| prevText.equals("the") || prevText.equals("The")) {
			score *= 0.9;
		} else {
			score *= 0.6;
		}
		// d. How long is the word? Longer words tend to be named entities.
		int length = text.length();
		if (length >= scoreLength.length) {
			length = scoreLength.length - 1;
		}
		score *= scoreLength[length];
		if (partsOfSpeech.size() > 0) {
			// The following parts of speech are considered to bring the score
			// above the cutoff
			// Noun N
			// Noun Phrase h
			// Nominative o
			// There is no penalty for other parts of speech
			if (partsOfSpeech.containsKey(text)
					&& partsOfSpeech.get(text).matches("[Nho]")) {
				if (score < CUTOFF) {
					score = CUTOFF;
				}
			}
		}
		if (score >= CUTOFF) {
			// only print the score for Named entities
			((Element) word).setAttribute("NER", df.format(score));
		}
	}

}
