/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at

 *      http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.developerstudio.eclipse.esb.synapse.unit.test.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class JavaScriptLineStyler implements LineStyleListener {
	JavaScanner scanner = new JavaScanner();
	int[] tokenColors;
	Color[] colors;
	Vector blockComments = new Vector();

	public static final int EOF = -1;
	public static final int EOL = 10;

	public static final int WORD = 0;
	public static final int WHITE = 1;
	public static final int KEY = 2;
	public static final int COMMENT = 3;
	public static final int STRING = 5;
	public static final int OTHER = 6;
	public static final int NUMBER = 7;

	public static final int XML_TAG = 8;
	public static final int XML_PROPERTY = 9;

	public static final int MAXIMUM_TOKEN = 10;

	public JavaScriptLineStyler() {
		initializeColors();
		scanner = new JavaScanner();
	}

	Color getColor(int type) {
		if (type < 0 || type >= tokenColors.length) {
			return null;
		}
		return colors[tokenColors[type]];
	}

	boolean inBlockComment(int start, int end) {
		for (int i = 0; i < blockComments.size(); i++) {
			int[] offsets = (int[]) blockComments.elementAt(i);
			// start of comment in the line
			if ((offsets[0] >= start) && (offsets[0] <= end))
				return true;
			// end of comment in the line
			if ((offsets[1] >= start) && (offsets[1] <= end))
				return true;
			if ((offsets[0] <= start) && (offsets[1] >= end))
				return true;
		}
		return false;
	}

	void initializeColors() {
		Display display = Display.getDefault();
		colors = new Color[] { new Color(display, new RGB(0, 0, 0)),
				new Color(display, new RGB(0, 128, 0)),
				new Color(display, new RGB(0, 0, 225)),
				new Color(display, new RGB(128, 0, 0)),
				new Color(display, new RGB(255, 0, 0)),
				new Color(display, new RGB(128, 0, 225))
		};
		tokenColors = new int[MAXIMUM_TOKEN];
		tokenColors[WORD] = 0;
		tokenColors[WHITE] = 0;
		tokenColors[KEY] = 3;
		tokenColors[COMMENT] = 1;
		tokenColors[STRING] = 2;
		tokenColors[OTHER] = 0;
		tokenColors[NUMBER] = 0;
		tokenColors[XML_TAG] = 3;
		tokenColors[XML_PROPERTY] = 4;
	}

	void disposeColors() {
		for (int i = 0; i < colors.length; i++) {
			colors[i].dispose();
		}
	}

	/**
	 * Event.detail line start offset (input) Event.text line text (input)
	 * LineStyleEvent.styles Enumeration of StyleRanges, need to be in order.
	 * (output) LineStyleEvent.background line background color (output)
	 */
	public void lineGetStyle(LineStyleEvent event) {
		Vector styles = new Vector();
		int token;
		StyleRange lastStyle;
		// If the line is part of a block comment, create one style for the
		// entire line.
		if (inBlockComment(event.lineOffset, event.lineOffset
				+ event.lineText.length())) {
			styles.addElement(new StyleRange(event.lineOffset, event.lineText
					.length(), getColor(COMMENT), null));
			event.styles = new StyleRange[styles.size()];
			styles.copyInto(event.styles);
			return;
		}
		Color defaultFgColor = ((Control) event.widget).getForeground();
		scanner.setRange(event.lineText);
		token = scanner.nextToken();
		while (token != EOF) {
			if (token == OTHER) {
				// do nothing for non-colored tokens
			} else if (token != WHITE) {
				Color color = getColor(token);
				// Only create a style if the token color is different than the
				// widget's default foreground color and the token's style is
				// not
				// bold. Keywords are bolded.
				if ((!color.equals(defaultFgColor)) || (token == KEY)) {
					StyleRange style = new StyleRange(scanner.getStartOffset()
							+ event.lineOffset, scanner.getLength(), color,
							null);
					if (token == KEY) {
						style.fontStyle = SWT.BOLD;
					}
					if (styles.isEmpty()) {
						styles.addElement(style);
					} else {
						// Merge similar styles. Doing so will improve
						// performance.
						lastStyle = (StyleRange) styles.lastElement();
						if (lastStyle.similarTo(style)
								&& (lastStyle.start + lastStyle.length == style.start)) {
							lastStyle.length += style.length;
						} else {
							styles.addElement(style);
						}
					}
				}
			} else if ((!styles.isEmpty())
					&& ((lastStyle = (StyleRange) styles.lastElement()).fontStyle == SWT.BOLD)) {
				int start = scanner.getStartOffset() + event.lineOffset;
				lastStyle = (StyleRange) styles.lastElement();
				// A font style of SWT.BOLD implies that the last style
				// represents a java keyword.
				if (lastStyle.start + lastStyle.length == start) {
					// Have the white space take on the style before it to
					// minimize the number of style ranges created and the
					// number of font style changes during rendering.
					lastStyle.length += scanner.getLength();
				}
			}
			token = scanner.nextToken();
		}
		event.styles = new StyleRange[styles.size()];
		styles.copyInto(event.styles);
	}

	public void parseBlockComments(String text) {
		blockComments = new Vector();
		StringReader buffer = new StringReader(text);
		int ch;
		boolean blkComment = false;
		int cnt = 0;
		int pre_ch = -1;
		int[] offsets = new int[2];
		boolean done = false;

		try {
			while (!done) {
				switch (ch = buffer.read()) {
				case -1: {
					if (blkComment) {
						offsets[1] = cnt;
						blockComments.addElement(offsets);
					}
					done = true;
					break;
				}
				case '/': {
					ch = buffer.read();
					if ((ch == '*') && (!blkComment)) {
						offsets = new int[2];
						offsets[0] = cnt;
						blkComment = true;
						cnt++;
					} else if ((pre_ch == '*') && (blkComment)) {
						blkComment = false;
						offsets[1] = cnt;
						blockComments.addElement(offsets);
						cnt++;
					} else {
						cnt++;
					}
					pre_ch = ch;
					cnt++;
					break;
				}
				default: {
					pre_ch = ch;
					cnt++;
					break;
				}
				}
			}
		} catch (IOException e) {
			// ignore errors
		}
	}

	/**
	 * A simple fuzzy scanner for Java
	 */
	public class JavaScanner {

		protected Hashtable fgKeys = null;
		protected StringBuffer fBuffer = new StringBuffer();
		protected String fDoc;
		protected int fPos;
		protected int fEnd;
		protected int fStartToken;
		protected boolean fEofSeen = false;

		boolean xml_tag = false;
		boolean xml_cdata = false;
		private String[] fgKeywords = {
				"abstract", "boolean", "break", "byte", "case", "catch",
				"char", "class", "const", "continue", "debugger", "default",
				"delete", "do", "double", "else", "enum", "export", "extends",
				"final", "finally", "float", "for", "function", "goto", "if",
				"implements", "import", "in", "instanceof", "int", "interface",
				"long", "native", "new", "package", "private", "protected",
				"public", "return", "short", "static", "super", "switch",
				"synchronized", "this", "throw", "throws", "transient", "try",
				"typeof", "var", "void", "volatile", "while", "with", "true",
				"false", "prototype",
				// sql
				"select", "update","delete","distinct","set", "from", "group by", "where", "and", "or",
				"order by", "sum"

		};

		public JavaScanner() {
			initialize();
		}

		/**
		 * Returns the ending location of the current token in the document.
		 */
		public final int getLength() {
			return fPos - fStartToken;
		}

		/**
		 * Initialize the lookup table.
		 */
		void initialize() {
			fgKeys = new Hashtable();
			Integer k = new Integer(KEY);
			for (int i = 0; i < fgKeywords.length; i++)
				fgKeys.put(fgKeywords[i], k);
		}

		/**
		 * Returns the starting location of the current token in the document.
		 */
		public final int getStartOffset() {
			return fStartToken;
		}

		/**
		 * Returns the next lexical token in the document.
		 */
		public int nextToken() {
			int c;
			fStartToken = fPos;
			while (true) {
				if (xml_tag) {
					c = read();
					if (Character.isWhitespace((char) c)) {
						do {
							c = read();
						} while (Character.isWhitespace((char) c));
						unread(c);
						return WHITE;
					} else if (c == '>') {
						xml_tag = false;
						unread(c);
						return OTHER;
					} else if(Character.isJavaIdentifierPart((char) c)){
						do {
							c = read();
							if (c == '=' || (c == EOF) || (c == EOL)
									|| Character.isWhitespace((char) c))
								break;
						} while (true);
						unread(c);
						return XML_PROPERTY;
					}
					else{
						unread(c);
					}
				}

				switch (c = read()) {
				case EOF:
					return EOF;
				case ']':
					xml_cdata = false;
					return OTHER;	
				case '<': // comment
					c = read();
					if (c != '>' && c != '!') {
						if(xml_cdata){
							unread(c);
							return OTHER;
						}
						xml_tag = true;
						while (true) {
							c = read();
							if ((c == EOF) || (c == EOL)) {
								unread(c);
								return XML_TAG;
							} else if (Character.isWhitespace((char) c)) {
								unread(c);
								return XML_TAG;
							} else if (c == '>') {
								xml_tag = false;
								unread(c);
								return XML_TAG;
							}
						}
					}else if(c == '!'){
						c = read();
						if(c=='-'){
							while (true) {
								c = read();
								if ((c == EOF) || (c == EOL)) {
									unread(c);
									return COMMENT;
								}
							}
						}
						else if(c=='['){
							xml_cdata = true;
						}
						unread(c);
					}
					unread(c);
					return OTHER;
				case '/': // comment
					c = read();
					if (c == '/') {
						while (true) {
							c = read();
							if ((c == EOF) || (c == EOL)) {
								unread(c);
								return COMMENT;
							}
						}
					}
					unread(c);
					return OTHER;
				case '\'': // char const
					while (true) {
						c = read();
						switch (c) {
						case '\'':
							return STRING;
						case EOF:
							unread(c);
							return STRING;
						case '\\':
							c = read();
							break;
						}
					}

				case '"': // string
					while (true) {
						c = read();
						switch (c) {
						case '"':
							return STRING;
						case EOF:
							unread(c);
							return STRING;
						case '\\':
							c = read();
							break;
						}
					}

				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					do {
						c = read();
					} while (Character.isDigit((char) c));
					unread(c);
					return NUMBER;
				default:
					if (Character.isWhitespace((char) c)) {
						do {
							c = read();
						} while (Character.isWhitespace((char) c));
						unread(c);
						return WHITE;
					}
					if (Character.isJavaIdentifierStart((char) c)) {
						fBuffer.setLength(0);
						do {
							fBuffer.append((char) c);
							c = read();
						} while (Character.isJavaIdentifierPart((char) c));
						unread(c);
						Integer i = (Integer) fgKeys.get(fBuffer.toString().toLowerCase());
						if (i != null)
							return i.intValue();
						return WORD;
					}
					return OTHER;
				}
			}
		}

		/**
		 * Returns next character.
		 */
		protected int read() {
			if (fPos <= fEnd) {
				return fDoc.charAt(fPos++);
			}
			return EOF;
		}

		public void setRange(String text) {
			fDoc = text;
			fPos = 0;
			fEnd = fDoc.length() - 1;
		}

		protected void unread(int c) {
			if (c != EOF)
				fPos--;
		}
	}

}