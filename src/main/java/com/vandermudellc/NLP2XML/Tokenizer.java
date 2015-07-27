package NLP2XML;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tokenizer groups characters into tokens
 * 
 * Groups letters into words
 * 
 * Tokenizes punctuation
 * 
 * Removes spaces
 * 
 * Removes duplicate carriage returns
 * 
 * Groups ellipses and em-dash
 * 
 * NOTE: this tokenizer works only on ASCII characters (0x20 - 0x7e). It does
 * not deal with Unicode characters. The reason for this is that there are a lot
 * of special Unicode characters to handle, and not enough time to go into
 * detail.
 * 
 * NOTE: for the purposes of this exercise, flip [."] -> [".] hard-coded This
 * should be handled for elegantly in a real tokenizer
 */
public class Tokenizer {

	/**
	 * Turn byte string into tokens. Flip some characters so terminating period,
	 * question mark or exclamation is at end of sentence. Process possessive
	 * and hyphenated words. Process special punctuation character strings.
	 * Suppress multiple carriage returns
	 * 
	 * @param rawText byte array
	 * @return list of token strings
	 */
	List<String> makeTokens(byte[] rawText) {
		List<String> list = new ArrayList<String>();
		int tokenStart = 0;
		for (int i = 0; i < rawText.length; i++) {
			flipChars(rawText, i, '.', '"');
			flipChars(rawText, i, '!', '"');
			flipChars(rawText, i, '?', '"');
			flipChars(rawText, i, '.', '\'');
			flipChars(rawText, i, '!', '\'');
			flipChars(rawText, i, '?', '\'');
			// Add a possessive to a word
			if (((i + 1) < rawText.length) && rawText[i] == '\''
					&& rawText[i + 1] == 's') {
				continue;
			}
			// Add a single dash to a word
			if (((i + 1) < rawText.length) && rawText[i] == '-'
					&& Character.isLetterOrDigit(rawText[i + 1])) {
				continue;
			}
			// End of word. Add word to list and process any punctuation
			if (!Character.isLetterOrDigit(rawText[i])) {
				// Ellipses "..."
				if (((i + 3) < rawText.length) && rawText[i] == '.'
						&& rawText[i + 1] == '.' && rawText[i + 2] == '.') {
					i += 3;
					tokenStart = i;
					list.add("...");
				}
				// Em-dash "--"
				if (((i + 2) < rawText.length) && rawText[i] == '-'
						&& rawText[i + 1] == '-') {
					i += 2;
					tokenStart = i;
					list.add("--");
				}
				// Suppress multiple carriage returns
				// Assuming a paragraph ends with a carriage return
				if (((i + 1) < rawText.length) && rawText[i] == '\n'
						&& rawText[i + 1] == '\n') {
					i += 1;
					tokenStart = i;
				}
				if (tokenStart < i) {
					list.add(new String(Arrays.copyOfRange(rawText, tokenStart,
							i)));
				}
				if (rawText[i] != ' ') {
					list.add(new String(Arrays.copyOfRange(rawText, i, i + 1)));
				}
				tokenStart = i + 1;
			}
		}
		return list;
	}

	/**
	 * Flip two characters starting at location i only if characters c1 and c2 
	 * are found at location i and i + 1
	 * @param rawText byte array
	 * @param i location in array
	 * @param c1 character 1
	 * @param c2 character 2
	 */
	void flipChars(byte[] rawText, int i, char c1, char c2) {
		if (((i + 1) < rawText.length) && rawText[i] == c1
				&& rawText[i + 1] == c2) {
			rawText[i] = (byte) c2;
			rawText[i + 1] = (byte) c1;
		}

	}
}
