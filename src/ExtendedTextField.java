import java.util.Vector;

import javax.microedition.lcdui.*;

import java.util.*;
import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;

// TODO: scroll bar
// TODO: view filename

public class ExtendedTextField extends Canvas implements Runnable, CommandListener
{
	private Command exit, save, saveAs, open, eval, output, cls;
	
	public Hajime midlet;
	
	private FileBrowser fileBrowser;
	
	private String currentDirName = null;
	private String currentFileName = null;

	static final String[] keyChars =
	{
		"0+-*/\\%=",
		".,-_?!:;&/\\^|~1",
		"abc2#",
		"def3<>",
		"ghi4()",
		"jkl5[]",
		"mno6{}",
		"pqrs7$",
		"tuv8\"'@",
		"wxyz9&|!",
	};
	
	boolean isUppercase = false;
	
	int clearKeyCode = Integer.MIN_VALUE;
	
	int lastPressedKey = Integer.MIN_VALUE;
	int currentKeyStep = 0;
	
	Font inputFont = null;
	int inputWidth = 0;
	int inputHeight = 0;
	int linesOnScreen = 0;
	
	long lastKeyTimestamp = 0;
	long maxKeyDelay = 500L;
	
	int caretLeft = 0;
	boolean caretBlinkOn = true;
	long caretBlinkDelay = 500L;
	long lastCaretBlink = 0;
	boolean goToNextChar = true;
	
	char[] currentChars;
	
	// TODO: manual keyboard
	public static final int DELETE = -8;
	
	boolean isOutput = false;
	String currentCode;
	int currentX = 0;
	int currentY = 0;
	
	private class FileBrowserStatus
	{
		public static final short None = 0;
		public static final short Open = 1; 
		public static final short Save = 2;
	};
	
	private short fileBrowserStatus = FileBrowserStatus.None;
	
	public ExtendedTextField()
	{
		setFullScreenMode(true);
		
		setCommandListener(this);
		
		exit = new Command("Exit", Command.EXIT, 1);
		open = new Command("Open", Command.ITEM, 2);
		save = new Command("Save", Command.ITEM, 3);
		saveAs = new Command("Save As", Command.ITEM, 4);
		eval = new Command("Eval", Command.ITEM, 5);
		output = new Command("Output", Command.ITEM, 6);
		cls = new Command("Clear", Command.ITEM, 7);
		
		addCommand(exit);
		addCommand(open);
		addCommand(save);
		addCommand(saveAs);
		addCommand(eval);
		addCommand(output);
		addCommand(cls);
		
		new Thread(this).start();
		
		// TODO: change font size
		inputFont = Font.getDefaultFont();
		inputWidth = getWidth();
		inputHeight = inputFont.getHeight();
		linesOnScreen = getHeight()/inputHeight;
		
		addNewLine(0);
	}
	
	public void commandAction(Command c, Displayable s)
	{
		if(c == exit)
		{
			// Cleanup and notify that the MIDlet has exited
			midlet.destroyApp(false);
			midlet.notifyDestroyed();
		}
		else if(c == save)
		{
			fileBrowser = new FileBrowser(this, midlet.display);
			
			fileBrowser.setCurrentDir(currentDirName);
			
			fileBrowser.setCurrentFile(currentFileName);
			
			Save();
		}
		else if(c == saveAs)
		{
			fileBrowserStatus = FileBrowserStatus.Save;
			
			fileBrowser = new FileBrowser(this, midlet.display);
			
			fileBrowser.setCurrentDir(currentDirName);
			
			fileBrowser.browse();
		}
		else if(c == open)
		{
			fileBrowserStatus = FileBrowserStatus.Open;
			
			fileBrowser = new FileBrowser(this, midlet.display);
			
			fileBrowser.setCurrentDir(currentDirName);
			
			fileBrowser.browse();
		}
		
		else if(c == FileBrowser.select)
		{
			currentDirName = fileBrowser.getCurrentDir();
			currentFileName = fileBrowser.getCurrentFile();
			switch(fileBrowserStatus)
			{
				case FileBrowserStatus.Open:
					Open();
					break;
				case FileBrowserStatus.Save:
					Save();
					break;
				case FileBrowserStatus.None:
					break;
				default: break;
			}
			
			fileBrowserStatus = FileBrowserStatus.None;
			
			midlet.display.setCurrent(this);
		}
		else if(c == FileBrowser.cancel)
		{
			// currentDirName = fileBrowser.getCurrentDir();
			// currentFileName = fileBrowser.getCurrentFile();
			midlet.display.setCurrent(this);
		}
		else if(c == eval)
		{
			Runnable r =
			new Runnable()
			{
				public void run()
				{
					midlet.runScript(getText());
				};
			};
			
			(new Thread(r)).start();
		}
		else if(c == output)
		{
			isOutput = !isOutput;
			if(isOutput)
			{
				removeCommand(output);
				output = new Command("Code", Command.ITEM, 5);
				addCommand(output);
				
				currentX = getCursorX();
				currentY = getCursorY();
				setCursorPosition(0, 0);
				updateCaretPosition();
				
				currentCode = getText();
				setText(midlet.getOutput());
			}
			else
			{
				removeCommand(output);
				output = new Command("Output", Command.ITEM, 5);
				addCommand(output);
				
				setText(currentCode);
				setCursorPosition(currentX, currentY);
				updateCaretPosition();
			}
		}
		else if(c == cls)
		{
			setCursorPosition(0, 0);
			updateCaretPosition();
			midlet.clearOutput();
			setText("");
		}
	}

	private void Save()
	{
		Runnable r =
		new Runnable()
		{
			public void run()
			{
				try
				{
					fileBrowser.writeFile(getText());
				}
				catch(Exception e)
				{
					System.out.println(e);
				}
			};
		};
		
		(new Thread(r)).start();
	}
	
	private void Open()
	{
		Runnable r =
		new Runnable()
		{
			public void run()
			{
				try
				{
					setText(fileBrowser.readFile());
				}
				catch(Exception e)
				{
					System.out.println(e);
				}
			};
		};
		
		(new Thread(r)).start();
	}

	public char[] getChars(int keyCode)
	{
		try
		{
			if(keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9)
			{
				return keyChars[keyCode - Canvas.KEY_NUM0].toCharArray();
			}
			else if(keyCode == KEY_POUND)
			{
				char[] chars = new char[1];

				chars[0] = ' ';

				return chars;
			}
		}
		catch(Exception ex)
		{

		}
		return null;
	}

	public char[] getNumbers(int keyCode)
	{
		char[] chars = new char[1];
		if(keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9)
		{
			chars[0] = String.valueOf(keyCode - Canvas.KEY_NUM0).charAt(0);
			return chars;
		}
		else if(keyCode == KEY_POUND)
		{
			chars[0] = ' ';

			return chars;
		}
		return null;
	}
	
	void updateCaretPosition()
	{
		int x = getCursorX();
		int y = getCursorY();
		
		caretLeft = inputFont.substringWidth(vectorLines.elementAt(y).toString(), 0, x);
	}
	
	protected void keyPressed(int keyCode)
	{
		int gameAction = getGameAction(keyCode);
		
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
		currentKeyStep = 0;
		int gameAction = getGameAction(keyCode);
		
		if(keyCode == DELETE)
		{
			removeCharacter();
			updateCaretPosition();
		}
		else if((keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9))
		{
			writeKeyPressed(keyCode, true);
			updateCaretPosition();
		}
		else if(keyCode == KEY_POUND)
		{
			writeKeyPressed(keyCode, true);
			updateCaretPosition();
			goToNextChar = true;
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
	
	public void writeKeyPressed(int keyCode, boolean isRepeated)
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
		
		if(isRepeated)
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
		
		int translationX = caretLeft - (getWidth() - 5);
		int translationY = y*inputHeight - (getHeight() - 40);
		
		if(translationX > 0)
		{
			g.translate(-translationX, 0);
		}
		
		if(translationY > 0)
		{
			g.translate(0, -translationY);
			
			// System.out.println((g.getTranslateY())/inputHeight);
			// getRelativeY() (g.getTranslateY())/inputHeight
			
			displayLines(g, y - linesOnScreen);
		}
		else
		{
			displayLines(g, 0);
		}
		
		if(caretBlinkOn && goToNextChar)
		{
			displayCursor(g);
		}
		
		
		if(translationX > 0)
		{
			g.translate(translationX, 0);
		}
		
		if(translationY > 0)
		{
			g.translate(0, translationY);
		}
		
		if(!goToNextChar)
		{
			displayCharacterMap(g);
		}
	}
	
	// display
	// not used
	void displayLines(Graphics g)
	{
		for(int y = 0; y < vectorLines.size(); y++)
		{
			g.drawString(vectorLines.elementAt(y).toString(), 0, y*inputHeight, Graphics.LEFT | Graphics.TOP);
		}
	}
	
	void displayLines(Graphics g, int yStart)
	{
		// (1 + 2, depends on font size)
		for(int y = Math.max(0, yStart); y < Math.min(yStart + linesOnScreen + 1 + 2, vectorLines.size()); y++)
		{
			g.drawString(vectorLines.elementAt(y).toString(), 0, y*inputHeight, Graphics.LEFT | Graphics.TOP);
		}
	}
	
	// not used
	int getRelativeY()
	{
		int y = getCursorY();
		return (y - (y/linesOnScreen)*y);
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
				
				// TODO: if i*12 > getWidth() ?
				
				g.drawChar(ch, i*12, getHeight() - inputHeight, Graphics.LEFT | Graphics.TOP);
				if(currentChars[currentKeyStep] == currentChars[i])
				{
					g.drawRect(i*12 - 2, getHeight() - inputHeight - 2, inputFont.charWidth(ch)+ 4, inputHeight + 4);
				}
			}
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
	
	// TODO: throws
	boolean isEmptyLine(int y)
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
		
		if(chars.length == 0)
		{
			vectorLines.addElement(stringBuffer);
		}
		
		setCursorPosition(0, 0);
		updateCaretPosition();
	}
}