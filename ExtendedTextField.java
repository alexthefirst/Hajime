// import javax.microedition.lcdui.Canvas;
// import javax.microedition.lcdui.Font;
// import javax.microedition.lcdui.Graphics;
import java.util.Vector;

import javax.microedition.lcdui.*;
// import javax.microedition.midlet.*;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
/*
import org.hecl.Interp;
import org.hecl.HeclException;
import org.hecl.HeclTask;
import org.hecl.ListThing;
import org.hecl.ObjectThing;
import org.hecl.Thing;
import org.hecl.midp20.MidletCmd;
import org.hecl.misc.HeclUtils;
import org.hecl.net.HttpCmd;
import org.hecl.net.Base64Cmd;
import org.hecl.rms.RMSCmd;
*/

public class ExtendedTextField extends Canvas implements Runnable, CommandListener
{
	private Command exitCommand, saveCommand, loadCommand;
	
	public SimpleTextEditor midlet;
	/* TODO:
	" 0",
	".,?!1@'-_():/*%#+<>=",
	"abc2",
	"def3",
	"ghi4",
	"jkl5",
	"mno6",
	"pqrs7",
	"tuv8",
	"wxyz9",
	*/
	static final char[] KEY_NUM1_CHARS = {'.', ',', '_', '1', ':', ';', '=', '?', '!', '&', '\\', '@'}; // {'.', '?', '!'};
	static final char[] KEY_NUM2_CHARS = {'a', 'b', 'c', '2', '#'};
	static final char[] KEY_NUM3_CHARS = {'d', 'e', 'f', '3', '<', '>'};
	static final char[] KEY_NUM4_CHARS = {'g', 'h', 'i', '4', '(', ')'};
	static final char[] KEY_NUM5_CHARS = {'j', 'k', 'l', '5', '[', ']'};
	static final char[] KEY_NUM6_CHARS = {'m', 'n', 'o', '6', '{', '}'};
	static final char[] KEY_NUM7_CHARS = {'p', 'q', 'r', 's', '7', '$'};
	static final char[] KEY_NUM8_CHARS = {'t', 'u', 'v', '8', '"', '\''};
	static final char[] KEY_NUM9_CHARS = {'w', 'x', 'y', 'z', '9'};
	static final char[] KEY_NUM0_CHARS = {'0', '+', '-', '*', '/', '%'};
	
	// static final char[] KEY_STAR_CHARS
	
	static final char[] KEY_POUND_CHARS = {' '};
	
	boolean isUppercase = false;
	
	int clearKeyCode = Integer.MIN_VALUE;
	
	int lastPressedKey = Integer.MIN_VALUE;
	int currentKeyStep = 0;
	
	Font inputFont = null;
	int inputWidth = 0;
	int inputHeight = 0;
	
	long lastKeyTimestamp = 0;
	long maxKeyDelay = 500L;
	// int caretIndex = 0;
	int caretLeft = 0;
	boolean caretBlinkOn = true;
	long caretBlinkDelay = 500L;
	long lastCaretBlink = 0;
	boolean goToNextChar = true;
	
	char[] currentChars;
	
	public static final int DELETE = -8;
	
	public ExtendedTextField()
	{
		setFullScreenMode(true);
		
		setCommandListener(this);
		
		exitCommand = new Command("Exit", Command.EXIT, 1);
		saveCommand = new Command("Save", Command.ITEM, 2);
		loadCommand = new Command("Load", Command.ITEM, 3);
		
		addCommand(saveCommand);
		addCommand(loadCommand);
		addCommand(exitCommand);
		
		new Thread(this).start();
		inputFont = Font.getDefaultFont();
		inputWidth = getWidth();
		inputHeight = inputFont.getHeight();
		
		addNewLine(0);
	}
	
	public void commandAction(Command c, Displayable s)
	{
		if(c == exitCommand)
		{
			// Cleanup and notify that the MIDlet has exited
			midlet.destroyApp(false);
			midlet.notifyDestroyed();
		}
		else if(c == saveCommand)
		{
			Runnable r =
			new Runnable()
			{
				public void run()
				{
					String text = getText();
					try
					{
						FileConnection fc = (FileConnection)Connector.open("file:///e:/textEditor.txt", Connector.WRITE);

						fc.truncate(0);
						
						OutputStream os = fc.openOutputStream(0);
						
						os.flush();
						
						os.write(text.getBytes(), 0, text.getBytes().length);
						
						os.flush();
						
						os.close();
						
						fc.close();
					}
					catch(Exception ex)
					{
						System.out.println("Exception: " + ex.toString());
					}
				};
			};
			
			(new Thread(r)).start();
		}
		else if(c == loadCommand)
		{
			String text = "";
			try
			{
				FileConnection fc = (FileConnection)Connector.open("file:///e:/textEditor.txt");

				if(fc.exists())
				{
					byte[] b = new byte[1024];
					InputStream is = fc.openInputStream();
					int length = is.read(b, 0, 1024);

					is.close();

					if(length > 0)
					{
						text = new String(b, 0, length);
					}
				}
				fc.close();
			}
			catch(Exception ex)
			{
				System.out.println("Exception: " + ex.toString());
			}

			setText(text);
		}
	}
	
	public char[] getChars(int keyCode)
	{
		switch(keyCode)
		{
			case Canvas.KEY_NUM1: return KEY_NUM1_CHARS;
			case Canvas.KEY_NUM2: return KEY_NUM2_CHARS;
			case Canvas.KEY_NUM3: return KEY_NUM3_CHARS;
			case Canvas.KEY_NUM4: return KEY_NUM4_CHARS;
			case Canvas.KEY_NUM5: return KEY_NUM5_CHARS;
			case Canvas.KEY_NUM6: return KEY_NUM6_CHARS;
			case Canvas.KEY_NUM7: return KEY_NUM7_CHARS;
			case Canvas.KEY_NUM8: return KEY_NUM8_CHARS;
			case Canvas.KEY_NUM9: return KEY_NUM9_CHARS;
			case Canvas.KEY_NUM0: return KEY_NUM0_CHARS;
			case Canvas.KEY_POUND:return KEY_POUND_CHARS;
		}
		return null;
	}
	
	public char[] getNumbers(int keyCode)
	{
		char[] chars = new char[1];
		switch(keyCode)
		{
			case Canvas.KEY_NUM1: chars[0] = '1';return chars;
			case Canvas.KEY_NUM2: chars[0] = '2';return chars;
			case Canvas.KEY_NUM3: chars[0] = '3';return chars;
			case Canvas.KEY_NUM4: chars[0] = '4';return chars;
			case Canvas.KEY_NUM5: chars[0] = '5';return chars;
			case Canvas.KEY_NUM6: chars[0] = '6';return chars;
			case Canvas.KEY_NUM7: chars[0] = '7';return chars;
			case Canvas.KEY_NUM8: chars[0] = '8';return chars;
			case Canvas.KEY_NUM9: chars[0] = '9';return chars;
			case Canvas.KEY_NUM0: chars[0] = '0';return chars;
			case Canvas.KEY_POUND:return KEY_POUND_CHARS;
		}
		return null;
	}
	
	void updateCaretPosition()
	{
		int x = getCursorX();
		int y = getCursorY();
		
		caretLeft = inputFont.substringWidth(vectorLines.elementAt(y).toString(), 0, x);
		/*
		if(caretLeft + inputTranslationX < 0)
		{
			inputTranslationX = - caretLeft;
		}
		else if(caretLeft + inputTranslationX > inputWidth)
		{
			inputTranslationX = inputWidth - caretLeft;
		}
		*/
	}
	
	protected void keyPressed(int keyCode)
	{
		int gameAction = getGameAction(keyCode);
		// System.out.println(cursorX + " " + cursorY);
		if(keyCode == DELETE)
		{
			removeCharacter();
			updateCaretPosition();
		}
		else if((keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) || (keyCode == KEY_POUND))
		{
			writeKeyPressed(keyCode, false);
			updateCaretPosition();
		}
		else if(keyCode == KEY_STAR)
		{
			isUppercase = !isUppercase;
		}
		else if(gameAction == Canvas.UP)
		{
			moveCursor(CURSOR_UP);
			updateCaretPosition();
		}
		else if(gameAction == Canvas.DOWN)
		{
			moveCursor(CURSOR_DOWN);
			updateCaretPosition();
		}
		else if(gameAction == Canvas.RIGHT)
		{
			moveCursor(CURSOR_RIGHT);
			updateCaretPosition();
		}
		else if(gameAction == Canvas.LEFT)
		{
			moveCursor(CURSOR_LEFT);
			updateCaretPosition();
		}
		else if(gameAction == Canvas.FIRE)
		{
			enterLine();
			updateCaretPosition();
		}
	}
	
	protected void keyReleased(int keyCode)
	{
	}

	protected void keyRepeated(int keyCode)
	{
		int gameAction = getGameAction(keyCode);
		// System.out.println(cursorX + " " + cursorY);
		if(keyCode == DELETE)
		{
			removeCharacter();
			updateCaretPosition();
		}
		else if((keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) || (keyCode == KEY_POUND))
		{
			writeKeyPressed(keyCode, true);
			updateCaretPosition();
		}
		else if(gameAction == Canvas.UP)
		{
			moveCursor(CURSOR_UP);
			updateCaretPosition();
		}
		else if(gameAction == Canvas.DOWN)
		{
			moveCursor(CURSOR_DOWN);
			updateCaretPosition();
		}
		else if(gameAction == Canvas.RIGHT)
		{
			moveCursor(CURSOR_RIGHT);
			updateCaretPosition();
		}
		else if(gameAction == Canvas.LEFT)
		{
			moveCursor(CURSOR_LEFT);
			updateCaretPosition();
		}
		else if(gameAction == Canvas.FIRE)
		{
			enterLine();
			updateCaretPosition();
		}
	}
	
	public void writeKeyPressed(int keyCode, boolean isNumeric)
	{
		if(goToNextChar || keyCode != lastPressedKey)
		{
			goToNextChar = true;
			lastPressedKey = keyCode;
			currentKeyStep = 0;
		}
		else
		{
			currentKeyStep++;
		}
		
		if(isNumeric)
		{
			currentChars = getNumbers(keyCode);
		}
		else
		{
			currentChars = getChars(keyCode);
		}
		
		if(currentChars != null)
		{
			if(currentKeyStep >= currentChars.length)
			{
				currentKeyStep -= currentChars.length;
			}
			
			char ch = currentChars[currentKeyStep];

			if(isUppercase)
			{
				ch = String.valueOf(ch).toUpperCase().charAt(0);
			}

			if(goToNextChar)
			{
				addCharacter(ch);
			}
			else
			{
				// System.out.println(currentKeyStep);
				updateCharacter(ch);
			}
			
			lastKeyTimestamp = System.currentTimeMillis();
			goToNextChar = false;
		}
	}
	
	public void checkTimestamps()
	{
		long currentTime = System.currentTimeMillis();
		if(lastCaretBlink + caretBlinkDelay < currentTime)
		{
			caretBlinkOn = !caretBlinkOn;
			lastCaretBlink = currentTime;
		}
		if(!goToNextChar && lastKeyTimestamp + maxKeyDelay < currentTime)
		{
			goToNextChar = true;
		}
	}
	
	public void run()
	{
		while(true)
		{
			checkTimestamps();
			repaint();
			try
			{
				synchronized(this)
				{
					wait(50L);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void paint(Graphics g)
	{
		g.setFont(inputFont);
		g.setColor(0xffffff);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(0x000000);
		
		int x = getCursorX();
		int y = getCursorY();
		
		// inputFont.charWidth(currentChars[i])+ 4, inputHeight + 4
		
		if(caretLeft > (getWidth() - 5))
		{
			g.translate(-(caretLeft - (getWidth() - 5)), 0);
		}
		
		if(y*inputHeight > (getHeight() - 40)) // (inputHeight + 4)
		{
			g.translate(0, -(y*inputHeight - (getHeight() - 40))); // (inputHeight + 4)
		}
		
		displayLines(g);
		if(caretBlinkOn && goToNextChar)
		{
			displayCursor(g);
		}
		
		
		if(caretLeft > (getWidth() - 5))
		{
			g.translate(+(caretLeft - (getWidth() - 5)), 0);
		}
		
		if(y*inputHeight > (getHeight() - 40)) // (inputHeight + 4)
		{
			g.translate(0, +(y*inputHeight - (getHeight() - 40))); // (inputHeight + 4)
			// caretLeft, (y)*inputHeight
			// g.translate(inputTranslationX, 0);
		}
		
		if(!goToNextChar)
		{
			displayCharacterMap(g);
		}
	}
	
	/////////////////////////////
	
	// characters
	public void addCharacter(char ch)
	{
		int x = getCursorX();
		int y = getCursorY();
		
		if(addToLine(x, y, ch))
		{
			setCursorPosition(x+1, y); // TODO: wrap
		}
	}
	
	public void updateCharacter(char ch)
	{
		int x = getCursorX();
		int y = getCursorY();
		
		if(isEmptyLine(y))
		{
			addCharacter(ch);
		}
		else
		{
			if(updateToLine(x, y, ch))
			{
				// setCursorPosition(x, y);
			}
		}
	}
	
	public void removeCharacter()
	{
		int x = getCursorX();
		int y = getCursorY();
		
		if(x != 0) // not first cursor position
		{
			if(removeFromLine(x, y))
			{
				if(y != 0) // not first line
				{
					/*
					if(isEmptyLine(y))
					{
						if(removeLine(y))
						{
							int maxX = getLineLength(y-1);
							setCursorPosition(maxX, y-1);
						}
					}
					else
					{
					*/
					setCursorPosition(x-1, y);
					/* } */
				}
				else
				{
					setCursorPosition(x-1, y);
				}
			}
		}
		else
		{
			if(y != 0) // not beginning of the document
			{
				if(isEmptyLine(y))
				{
					if(removeLine(y))
					{
						int maxX = getLineLength(y-1);
						setCursorPosition(maxX, y-1);
					}
				}
				else
				{
					StringBuffer currentLine = getLine(y); // (StringBuffer)vectorLines.elementAt(y);
					
					if(removeLine(y))
					{
						int maxX = getLineLength(y-1);
						addToLine(maxX, y-1, currentLine.toString());
						setCursorPosition(maxX, y-1);
					}
				}
			}
		}
	}
	
	// cursor
	int cursorX = 0;
	int cursorY = 0;
	
	public static final int CURSOR_UP = 1;
	public static final int CURSOR_DOWN = 2;
	public static final int CURSOR_LEFT = 3;
	public static final int CURSOR_RIGHT = 4;
	
	public void moveCursor(int direction)
	{
		int xCurrent = getCursorX();
		int yCurrent = getCursorY();
		int x = xCurrent;
		int y = yCurrent;
		
		if(direction == CURSOR_UP)
		{
			y--;
			if(y >= 0) // we can go up
			{
				if(getLineLength(y) < x)
				{
					x = getLineLength(y); // cursor moves at the end of upper line
				}
				
				setCursorPosition(x, y);
			}
		}
		else if(direction == CURSOR_DOWN)
		{
			y++;
			if(y < getLinesCount()) // we can go down
			{
				if(getLineLength(y) < x)
				{
					x = getLineLength(y); // cursor moves at the end of lower line
				}
				
				setCursorPosition(x, y);
			}
			else
			{
				// if(!isEmptyLine(yCurrent)) // ? current line not empty so we can add new line
				{
					addLastLine(); // ?
				}
			}
		}
		else if(direction == CURSOR_RIGHT)
		{
			x++;
			if(x > 0 && x <= getLineLength(y))
			{
				setCursorPosition(x, y);
			}
			else if(x == getLineLength(y) + 1)
			{
				addCharacter(' ');
				setCursorPosition(x, y);
			}
		}
		else if(direction == CURSOR_LEFT)
		{
			x--;
			if(x < getLineLength(y) && x >= 0)
			{
				setCursorPosition(x, y);
			}
			else if(x < 0 && y > 0)
			{
				setCursorPosition(getLineLength(y - 1), y - 1);
			}
		}
	}
	
	void setCursorPosition(int x, int y)
	{
		cursorX = x;
		cursorY = y;
	}
	
	int getCursorX()
	{
		return cursorX;
	}
	
	int getCursorY()
	{
		return cursorY;
	}
	
	
	// lines
	Vector vectorLines = new Vector(1);
	
	boolean addToLine(int x, int y, char ch)
	{
		try
		{
			// getLine
			StringBuffer currentLine = (StringBuffer)vectorLines.elementAt(y);
			
			currentLine.insert(x, ch);
			
			if(vectorLines.size() < y)
			{
				vectorLines.addElement(currentLine);
			}
			else
			{
				// setLine
				vectorLines.setElementAt(currentLine, y);
			}
			
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in addToLine()");
			return false;
		}
	}
	
	boolean addToLine(int x, int y, Object obj)
	{
		try
		{
			// getLine
			StringBuffer currentLine = (StringBuffer)vectorLines.elementAt(y);
			
			currentLine.insert(x, obj);
			
			if(vectorLines.size() < y)
			{
				//
				vectorLines.addElement(currentLine);
			}
			else
			{
				// setLine
				vectorLines.setElementAt(currentLine, y);
			}
			
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in addToLine()");
			return false;
		}
	}
	
	boolean updateToLine(int x, int y, char ch)
	{
		try
		{
			// getLine
			StringBuffer currentLine = (StringBuffer)vectorLines.elementAt(y);
			
			currentLine.setCharAt(x-1, ch);
			// setLine
			vectorLines.setElementAt(currentLine, y);
			
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in updateToLine()");
			return false;
		}
	}
	
	boolean removeFromLine(int x, int y)
	{
		try
		{
			// getLine
			StringBuffer currentLine = (StringBuffer)vectorLines.elementAt(y);
			
			currentLine.deleteCharAt(x - 1);
			// setLine
			vectorLines.setElementAt(currentLine, y);
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in removeFromLine()");
			return false;
		}
	}
	
	public void enterLine()
	{
		int x = getCursorX();
		int y = getCursorY();
		
		if(divideLine(x, y))
		{
			setCursorPosition(0, y+1);
		}
	}
	
	boolean divideLine(int x, int y) // fire <=> enter + insert when # pressed
	{
		try
		{
			// getLine
			StringBuffer currentLine = (StringBuffer)vectorLines.elementAt(y);
			
			String newLine = currentLine.toString().substring(x, currentLine.toString().length());
			
			currentLine.delete(x, currentLine.length());
			// setLine
			vectorLines.setElementAt(currentLine, y);
			
			if(shiftDown(y))
			{
				vectorLines.setElementAt(new StringBuffer(newLine), y+1);
			}
			
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in divideLine()");
			return false;
		}
	}
	
	StringBuffer getLine(int y)
	{
		try
		{
			return (StringBuffer)vectorLines.elementAt(y);
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in getLine()");
			return new StringBuffer("");
		}
	}
	
	boolean copyLine(int sourceY, int destinationY)
	{
		try
		{
			// getLine
			StringBuffer currentLine = (StringBuffer)vectorLines.elementAt(sourceY);
			// setLine
			vectorLines.setElementAt(currentLine, destinationY);
			
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in copyLine()");
			return false;
		}
	}
	
	boolean moveLine(int sourceY, int destinationY)
	{
		try
		{
			// getLine
			StringBuffer currentLine = (StringBuffer)vectorLines.elementAt(sourceY);
			
			vectorLines.setElementAt(currentLine, destinationY);
			// removeLine
			vectorLines.removeElementAt(sourceY);
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in moveLine()");
			return false;
		}
	}
	
	boolean shiftDown(int y)
	{
		try
		{
			addNewLine(getLinesCount() + 1 - 1);
			
			// tricky! -1 because of beginning from 0, -1 because next line, -1 because getLinesCount() is already updated (+1)
			for(int sourceY = getLinesCount()-2; sourceY >= y; sourceY--) // -1
			{
				copyLine(sourceY, sourceY+1);
			}
			
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in shiftDown()");
			return false;
		}
	}
	
	boolean setLine(int y, StringBuffer stringBuffer)
	{
		try
		{
			vectorLines.setElementAt(stringBuffer, y);
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in setLine()");
			return false;
		}
	}
	
	boolean isEmptyLine(int y) // TODO: throws
	{
		return (((StringBuffer)vectorLines.elementAt(y)).length() == 0);
	}
	
	boolean removeLine(int y)
	{
		try
		{
			vectorLines.removeElementAt(y);
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in removeLine()");
			return false;
		}
	}
	
	int getLineLength(int y)
	{
		try
		{
			return ((StringBuffer)vectorLines.elementAt(y)).length();
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString() + " in getLineLength()");
			return 0;
		}
	}
	
	public void addLastLine()
	{
		int x = getCursorX();
		int y = getCursorY();
		
		if(addNewLine(y+1))
		{
			setCursorPosition(0, y+1);
		}
	}
	
	boolean addNewLine(int y)
	{
		try
		{
			vectorLines.insertElementAt(new StringBuffer(""), y);
			return true;
		}
		catch(Exception ex)
		{
			System.out.println(ex.toString());
			return false;
		}
	}
	
	int getLinesCount()
	{
		return vectorLines.size();
	}
	
	public String getText()
	{
		String text = "";
		
		for(int y = 0; y < vectorLines.size(); y++)
		{
			text += vectorLines.elementAt(y).toString() + '\n';
		}
		
		return text;
	}

	public void setText(String text)
	{
		vectorLines.removeAllElements();
		
		char[] chars = text.toCharArray();
		
		StringBuffer stringBuffer = new StringBuffer("");
		
		for(int i = 0; i < chars.length; i++)
		{
			if(chars[i] == '\n' || chars[i] == '\r')
			{
				if(chars[i] == '\n')
				{
					vectorLines.addElement(stringBuffer);
					
					stringBuffer = new StringBuffer();
				}
				continue;
			}
			else if(i == chars.length - 1)
			{
				vectorLines.addElement(stringBuffer);
			}
			
			stringBuffer.append(chars[i]);
		}
		
		setCursorPosition(0, 0);
	}
	
	// display
	void displayLines(Graphics g)
	{
		for(int y = 0; y < vectorLines.size(); y++)
		{
			g.drawString(vectorLines.elementAt(y).toString(), 0, y*inputHeight, Graphics.LEFT | Graphics.TOP);
		}
	}
	
	void displayCursor(Graphics g)
	{
		int x = getCursorX();
		int y = getCursorY();
		
		if(x >= 0 && x <= vectorLines.elementAt(y).toString().length())
		{
			g.drawLine(caretLeft, (y)*inputHeight, caretLeft, (y+1)*inputHeight);
		}
	}
	
	void displayCharacterMap(Graphics g)
	{
		if(currentChars != null)
		{
			g.setColor(0xffffff);
			g.fillRect(0, getHeight() - inputHeight, getWidth(), inputHeight);
			g.setColor(0x000000);
			
			for(int i = 0; i < currentChars.length; i++)
			{
				char ch = currentChars[i];
				if(isUppercase)
				{
					ch = String.valueOf(currentChars[i]).toUpperCase().charAt(0);
				}
				g.drawChar(ch, i*12, getHeight() - inputHeight, Graphics.LEFT | Graphics.TOP);
				if(currentChars[currentKeyStep] == currentChars[i])
				{
					g.drawRect(i*12 - 2, getHeight() - inputHeight - 2, inputFont.charWidth(ch)+ 4, inputHeight + 4);
				}
			}
		}
	}
	
	// scroll
	/*
	void ScrollUp()
	{
	}
	
	void ScrollDown()
	{
	}
	// ?
	void ScrollLeft()
	{
	}
	// ?
	void ScrollRight()
	{
	}
	*/
}