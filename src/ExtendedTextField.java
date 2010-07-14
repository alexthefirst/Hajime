import java.util.Vector;

import javax.microedition.lcdui.*;

import java.util.*;
import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;

// TODO: highlight, help, find, clear selection, insert into selection, tabs

public class ExtendedTextField extends Canvas implements Runnable, CommandListener
{
	private Command exit, save, saveAs, open, eval, output, cls, newFile;
	
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
	
	class EditorState
	{
		public static final short Input = 1;
		public static final short Commands = 2;
	}
	
	int currentState = EditorState.Input;
	
	char currentCharCommand = ' ';
	
	private void setState(short state)
	{
		// TODO: save capslock status?
		currentState = state;
	}
	
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
	
	// TODO: manual keyboard?
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
	
	private boolean isViewScrollBar = false;

	long scrollBarDelay = 1000L;

	long lastScrollBarTimestamp = 0;

	void viewScrollBar()
	{
		isViewScrollBar = true;
		lastScrollBarTimestamp = System.currentTimeMillis();
	}
	
	boolean isViewStatus = false;

	long statusDelay = 5000L;

	long lastStatusTimestamp = 0;
	
	String statusString = "";
	
	void setAndViewStatus(String status)
	{
		statusString = status;
		isViewStatus = true;
		lastStatusTimestamp = System.currentTimeMillis();
	}

	public ExtendedTextField()
	{
		setFullScreenMode(true);
		
		setCommandListener(this);
		
		exit = new Command("Exit", Command.EXIT, 1);
		open = new Command("Open", Command.ITEM, 1);
		newFile = new Command("New", Command.ITEM, 2);
		save = new Command("Save", Command.ITEM, 3);
		saveAs = new Command("Save As", Command.ITEM, 4);
		eval = new Command("Eval", Command.ITEM, 5);
		output = new Command("Output", Command.ITEM, 6);
		cls = new Command("Clear", Command.ITEM, 7);
		
		addCommand(exit);
		addCommand(newFile);
		addCommand(open);
		addCommand(save);
		addCommand(saveAs);
		addCommand(eval);
		addCommand(output);
		addCommand(cls);
		
		new Thread(this).start();
		
		// TODO: change font size?
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
			ExitCommand();
		}
		else if(c == newFile)
		{
			NewFileCommand();
		}
		else if(c == save)
		{
			SaveCommand();
		}
		else if(c == saveAs)
		{
			SaveAsCommand();
		}
		else if(c == open)
		{
			OpenCommand();
		}
		else if(c == FileBrowser.select)
		{
			currentDirName = fileBrowser.getCurrentDir();
			currentFileName = fileBrowser.getCurrentFile();
			
			if(currentDirName != null && currentFileName != null)
			{
				setAndViewStatus(currentDirName + currentFileName);
			}
			
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
			if(currentDirName != null && currentFileName != null)
			{
				setAndViewStatus(currentDirName + currentFileName);
			}
			
			midlet.display.setCurrent(this);
		}
		else if(c == eval)
		{
			EvalCommand();
		}
		else if(c == output)
		{
			SwitchOutputCommand();
		}
		else if(c == cls)
		{
			ClsCommand();
		}
	}
	
	public void ExitCommand()
	{
		// Cleanup and notify that the MIDlet has exited
		midlet.destroyApp(false);
		midlet.notifyDestroyed();
	}

	public void NewFileCommand()
	{
		currentFileName = null;
		ClsCommand();
	}

	public void SaveCommand()
	{
		fileBrowser = new FileBrowser(this, midlet.display);
		
		if(currentDirName != null && currentFileName != null)
		{
			fileBrowser.setCurrentDir(currentDirName);
			
			fileBrowser.setCurrentFile(currentFileName);
			
			Save();
		}
		else
		{
			fileBrowser.setCurrentDir(currentDirName);
			
			fileBrowser.browse();
		}
	}
	
	public void SaveAsCommand()
	{
		fileBrowserStatus = FileBrowserStatus.Save;
		
		fileBrowser = new FileBrowser(this, midlet.display);
		
		fileBrowser.setCurrentDir(currentDirName);
		
		fileBrowser.browse();
	}

	public void OpenCommand()
	{
		fileBrowserStatus = FileBrowserStatus.Open;
		
		fileBrowser = new FileBrowser(this, midlet.display);
		
		fileBrowser.setCurrentDir(currentDirName);
		
		fileBrowser.browse();
	}

	public void EvalCommand()
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

	public void ClsCommand()
	{
		clearSelection();
		setCursorPosition(0, 0);
		updateCaretPosition();
		midlet.clearOutput();
		setText("");
	}

	public void SwitchOutputCommand()
	{
		clearSelection();
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
			
			setAndViewStatus("Output");
		}
		else
		{
			removeCommand(output);
			output = new Command("Output", Command.ITEM, 5);
			addCommand(output);
			
			setText(currentCode);
			setCursorPosition(currentX, currentY);
			updateCaretPosition();
			
			if(currentDirName != null && currentFileName != null)
			{
				setAndViewStatus(currentDirName + currentFileName);
			}
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

	public void deleteLineCommand()
	{
		clearSelection();
		
		int y = getCursorY();
		
		if(y == 0)
		{
			if(getLinesCount() == 1)
			{
				setText("");
			}
			else
			{
				removeLine(y);
			}
			
			setCursorPosition(0, 0);
		}
		else
		{
			removeLine(y);
			setCursorPosition(0, y - 1);
		}
		updateCaretPosition();
	}
	
	// selection
	boolean isSelection = false;
	
	public void StartSelectionCommand()
	{
		int x = getCursorX();
		int y = getCursorY();
		startSelection(x, y);
	}
	
	public void EndSelectionCommand()
	{
		int x = getCursorX();
		int y = getCursorY();
		endSelection(x, y);
	}

	int xStartSelection = 0;
	int yStartSelection = 0;
	
	int xEndSelection = 0;
	int yEndSelection = 0;

	boolean isStartSelection = false;

	void startSelection(int x, int y)
	{
		xEndSelection = x;
		yEndSelection = y;
		
		xStartSelection = x;
		yStartSelection = y;
		
		isSelection = false;
		
		isStartSelection = true;
	}
	
	void endSelection(int x, int y)
	{
		xEndSelection = x;
		yEndSelection = y;
		
		isSelection = true;
		
		isStartSelection = false;
	}

	public void SelectAllCommand()
	{
		startSelection(0, 0);
		int y = getLinesCount() - 1;
		int x = vectorLines.elementAt(y).toString().length();
		endSelection(x, y);
	}
	
	void clearSelection()
	{
		xEndSelection = 0;
		yEndSelection = 0;
		
		xStartSelection = 0;
		yStartSelection = 0;
		
		isSelection = false;
	}
	
	String clipboard = "";
	
	public void CopySelectionCommand()
	{
		clipboard = getText(xStartSelection, yStartSelection, xEndSelection, yEndSelection);
	}
	
	public void CutSelectionCommand()
	{
		clipboard = getText(xStartSelection, yStartSelection, xEndSelection, yEndSelection);
		removeText(xStartSelection, yStartSelection, xEndSelection, yEndSelection);
		clearSelection();
	}
	
	public void PasteSelectionCommand()
	{
		clearSelection(); // TODO: paste into selection
		int x = getCursorX();
		int y = getCursorY();
		setText(clipboard, x, y);
	}
	
	boolean isSelected(int x, int y)
	{
		if(y == yStartSelection && yStartSelection == yEndSelection)
		{
			if(xStartSelection < xEndSelection)
			{
				if(x >= xStartSelection && x < xEndSelection)
				{
					return true;
				}
			}
			else
			{
				// TODO: recursive inversion
				return false;
			}
		}
		else if(y >= yStartSelection && y <= yEndSelection && yStartSelection < yEndSelection)
		{
			if(
				(y == yStartSelection && x >= xStartSelection) ||
				(y > yStartSelection && y < yEndSelection) ||
				(y == yEndSelection && x < xEndSelection)
			  )
			{
				return true;
			}
		}
		else
		{
			// TODO: recursive inversion
			return false;
		}
		return false;
	}
	
	public void SelectLineCommand()
	{
		clearSelection(); // TODO: add to selection?
		int y = getCursorY();
		startSelection(0, y);
		int x = vectorLines.elementAt(y).toString().length();
		endSelection(x, y);
	}
	
	public void GotoBeginCommand()
	{
		setCursorPosition(0, 0);
		updateCaretPosition();
		// setState(EditorState.Input);
	}
	
	public void GotoEndCommand()
	{
		setCursorPosition(0, getLinesCount()-1);
		updateCaretPosition();
		// setState(EditorState.Input);
	}
	
	public void PageUpCommand()
	{
		int y = getCursorY();
		setCursorPosition(0, Math.min(y + (linesOnScreen - 2), getLinesCount()-1));
		updateCaretPosition();
	}
	
	public void PageDownCommand()
	{
		int y = getCursorY();
		setCursorPosition(0, Math.max(y - (linesOnScreen - 2), 0));
		updateCaretPosition();
	}
	
	// chars 

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
	
	boolean isUp = false;
	boolean isDown = false;
	boolean isLeft = false;
	boolean isRight = false;
	
	protected void keyPressed(int keyCode)
	{
		int gameAction = getGameAction(keyCode);
		
		if(currentState == EditorState.Commands)
		{
			if(keyCode == DELETE)
			{
				setState(EditorState.Input);
			}
			if((keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9))
			{
				writeKeyPressed(keyCode, false);
			}
			else if(gameAction == Canvas.UP)
			{
				GotoBeginCommand();
			}
			else if(gameAction == Canvas.DOWN)
			{
				GotoEndCommand();
			}
			else if(gameAction == Canvas.RIGHT)
			{
				PageUpCommand();
			}
			else if(gameAction == Canvas.LEFT)
			{
				PageDownCommand();
			}
			else if(gameAction == Canvas.FIRE)
			{
				setState(EditorState.Input);
				
				switch(String.valueOf(currentCharCommand).toLowerCase().charAt(0))
				{
					case '[':
					case ']':
							if(isStartSelection)
							{
								EndSelectionCommand();
							}
							else
							{
								StartSelectionCommand();
							}
							break;
					case 'a': SelectAllCommand(); break;
					case 'c': CopySelectionCommand(); break;
					case 'd': deleteLineCommand(); break;
					case 'e': EvalCommand(); break;
					case 'f': break; // TODO: find
					case 'h': break; // TODO: help
					case 'l': SelectLineCommand(); break;
					case 'n': NewFileCommand(); break;
					case 'o': OpenCommand(); break;
					case 's': SaveCommand(); break;
					case 't': SwitchOutputCommand(); break;
					case 'v': PasteSelectionCommand(); break;
					case 'w': ExitCommand(); break;
					case 'x': CutSelectionCommand(); break;
					case 'y': break; // TODO: redo
					case 'z': break; // TODO: undo
				}
			}
		}
		else if(currentState == EditorState.Input)
		{
			if(keyCode == DELETE)
			{
				if(isSelection)
				{
					removeText(xStartSelection, yStartSelection, xEndSelection, yEndSelection);
					setCursorPosition(xStartSelection, yStartSelection);
				}
				else
				{
					removeCharacter();
				}
				
				clearSelection();
				
				updateCaretPosition();
			}
			else if((keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) || (keyCode == KEY_POUND))
			{
				clearSelection();
				writeKeyPressed(keyCode, false);
				updateCaretPosition();
			}
			else if(keyCode == KEY_STAR)
			{
				isUppercase = !isUppercase;
			}
			else if(gameAction == Canvas.UP)
			{
				isUp = true;
				// viewScrollBar();
				moveCursor(CURSOR_UP);
				updateCaretPosition();
			}
			else if(gameAction == Canvas.DOWN)
			{
				isDown = true;
				// viewScrollBar();
				moveCursor(CURSOR_DOWN);
				updateCaretPosition();
			}
			else if(gameAction == Canvas.RIGHT)
			{
				isRight = true;
				moveCursor(CURSOR_RIGHT);
				updateCaretPosition();
			}
			else if(gameAction == Canvas.LEFT)
			{
				isLeft = true;
				moveCursor(CURSOR_LEFT);
				updateCaretPosition();
			}
			else if(gameAction == Canvas.FIRE)
			{
				// TODO: if selection remove?
				clearSelection();
				enterLine();
				updateCaretPosition();
			}
		}
	}
	
	protected void keyReleased(int keyCode)
	{
		isUp = false;
		isDown = false;
		isRight = false;
		isLeft = false;
	}

	protected void keyRepeated(int keyCode)
	{
		currentKeyStep = 0;
		int gameAction = getGameAction(keyCode);
		if(currentState == EditorState.Commands)
		{
			if((keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9))
			{
				writeKeyPressed(keyCode, true);
			}
			else if(gameAction == Canvas.RIGHT)
			{
				PageUpCommand();
			}
			else if(gameAction == Canvas.LEFT)
			{
				PageDownCommand();
			}
		}
		else if(currentState == EditorState.Input)
		{
			if(keyCode == DELETE)
			{
				// TODO: if selection remove?
				clearSelection();
				removeCharacter();
				updateCaretPosition();
			}
			else if((keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9))
			{
				writeKeyPressed(keyCode, true);
				updateCaretPosition();
			}
			else if(keyCode == KEY_STAR)
			{
				setState(EditorState.Commands);
				isUppercase = !isUppercase;
				
			}
			else if(keyCode == KEY_POUND)
			{
				writeKeyPressed(keyCode, true);
				updateCaretPosition();
				goToNextChar = true;
			}
			else if(gameAction == Canvas.UP)
			{
				viewScrollBar();
				
				moveCursor(CURSOR_UP);
				updateCaretPosition();
			}
			else if(gameAction == Canvas.DOWN)
			{
				viewScrollBar();
				
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
				// TODO: if selection remove?
				clearSelection();
				enterLine();
				updateCaretPosition();
			}
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
			
			if(currentState == EditorState.Input)
			{
				if(goToNextChar)
				{
					// TODO: if selection remove?
					clearSelection();
					addCharacter(ch);
				}
				else
				{
					updateCharacter(ch);
				}
			}
			else if(currentState == EditorState.Commands)
			{
				currentCharCommand = ch;
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

		if(scrollBarDelay + lastScrollBarTimestamp < currentTime)
		{
			isViewScrollBar = false;
		}
		
		if(statusDelay + lastStatusTimestamp < currentTime)
		{
			isViewStatus = false;
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
	
	int translationY = 0;
	int translationX = 0;
	
	// boolean isTranslatedUp = false;
	// boolean isTranslatedDown = false;
	
	// int yRelative = 0;
	
	// int yStart = 0;
	
	public void paint(Graphics g)
	{
		g.setFont(inputFont);
		g.setColor(0xffffff);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(0x000000);
		
		int x = getCursorX();
		int y = getCursorY();
		
		translationX = caretLeft - (getWidth() - 5);
		
		// if(isUp)
		// {
		translationY = y*inputHeight - (getHeight() - 40);
		// }
		// else if(isDown)
		// {
		// 	translationY = y*inputHeight - (getHeight() - 40);
		// }
		// etc
		
		if(translationX > 0)
		{
			g.translate(-translationX, 0);
		}
		
		if(translationY > 0)
		{
			g.translate(0, -translationY);
			
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
		
		if(currentState == EditorState.Commands)
		{
			if(!goToNextChar)
			{
				displayCharacterMap(g);
			}
			else
			{
				displayCurrentCommand(g);
			}
		}
		else if(currentState == EditorState.Input)
		{
			if(!goToNextChar)
			{
				displayCharacterMap(g);
			}
			else if(isViewStatus)
			{
				displayStatus(g);
			}
		}

		if(isViewScrollBar)
		{
			displayScrollBar(g);
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
		
		if(isSelection && (xStartSelection != xEndSelection || yStartSelection != yEndSelection))
		{
			for(int y = Math.max(0, yStart); y < Math.min(yStart + linesOnScreen + 1 + 2, vectorLines.size()); y++)
			{
				String currentLine = vectorLines.elementAt(y).toString();
				int width = 0;
				
				for(int x = 0; x < currentLine.length(); x++)
				{
					int previousWidth = (x == 0) ? 0 : inputFont.charWidth(currentLine.charAt(x - 1));
					width += previousWidth;
					
					if(isSelected(x, y))
					{
						g.setColor(0x000000);
						g.fillRect(width, y*inputHeight, inputFont.charWidth(currentLine.charAt(x)), inputHeight);
						
						g.setColor(0xffffff);
						g.drawChar(currentLine.charAt(x), width, y*inputHeight, Graphics.LEFT | Graphics.TOP);
					}
					else
					{
						g.setColor(0x000000);
						g.drawChar(currentLine.charAt(x), width, y*inputHeight, Graphics.LEFT | Graphics.TOP);
					}
				}
				
				if(currentLine.length() == 0)
				{
					if(isSelected(0, y))
					{
						g.setColor(0x000000);
						g.fillRect(0, y*inputHeight, 5, inputHeight);
					}
				}
			}
		}
		else
		{
			for(int y = Math.max(0, yStart); y < Math.min(yStart + linesOnScreen + 1 + 2, vectorLines.size()); y++)
			{
				g.drawString(vectorLines.elementAt(y).toString(), 0, y*inputHeight, Graphics.LEFT | Graphics.TOP);
			}
		}
	}
	
	void displayCursor(Graphics g)
	{
		int x = getCursorX();
		int y = getCursorY();
		if(x >= 0 && x <= vectorLines.elementAt(y).toString().length())
		{
			if(isSelected(x, y))
			{
				g.setColor(0xffffff);
			}
			else
			{
				g.setColor(0x000000);
			}
			g.drawLine(caretLeft, (y)*inputHeight, caretLeft, (y+1)*inputHeight);
		}
	}
	
	void displayCharacterMap(Graphics g)
	{
		if(currentChars != null)
		{
			g.setColor(0xffffff);
			g.fillRect(0, getHeight() - inputHeight - 2, getWidth(), inputHeight + 2);
			g.setColor(0x000000);
			g.drawLine(0, getHeight() - inputHeight - 2, getWidth(), getHeight() - inputHeight - 2);
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
	
	void displayCurrentCommand(Graphics g)
	{
		g.setColor(0xffffff);
		g.fillRect(0, getHeight() - inputHeight - 2, getWidth(), inputHeight + 2);
		g.setColor(0x000000);
		g.drawLine(0, getHeight() - inputHeight - 2, getWidth(), getHeight() - inputHeight - 2);
		g.drawString("Ctrl" + " + " + currentCharCommand, 0, getHeight() - inputHeight, Graphics.LEFT | Graphics.TOP);
	}
	
	void displayStatus(Graphics g)
	{
		if(statusString != null)
		{
			g.setColor(0xffffff);
			g.fillRect(0, getHeight() - inputHeight - 2, getWidth(), inputHeight + 2);
			g.setColor(0x000000);
			g.drawLine(0, getHeight() - inputHeight - 2, getWidth(), getHeight() - inputHeight - 2);
			g.drawString(statusString, 0, getHeight() - inputHeight - 2, Graphics.LEFT | Graphics.TOP);
		}
	}

	void displayScrollBar(Graphics g)
	{
		int x = getCursorX();
		int y = getCursorY();
		g.setColor(0xffffff);
		g.fillRect(getWidth()-5, 0, 5, getHeight());
		g.setColor(0x000000);
		g.drawLine(getWidth()-5, 0, getWidth()-5, getHeight());
		
		int hScrollMin = inputHeight;
		
		int yScroll = (((linesOnScreen-1)*y)*inputHeight)/getLinesCount();
		
		g.fillRect(getWidth()-5, yScroll, 5, hScrollMin);
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
				clearSelection();
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
	
	boolean removeFromLine(int xFrom, int xTo, int y)
	{
		try
		{
			// getLine
			StringBuffer currentLine = (StringBuffer)vectorLines.elementAt(y);
			
			currentLine.delete(xFrom, xTo);
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
	
	public String getText(int xFrom, int yFrom, int xTo, int yTo)
	{
		String text = "";
		
		yFrom = Math.max(0, yFrom);
		yTo = Math.min(yTo, vectorLines.size() - 1);
		
		for(int y = yFrom; y <= yTo; y++)
		{
			if(yFrom == yTo)
			{
				if(xFrom < xTo)
				{
					String currentLine = vectorLines.elementAt(y).toString();
					
					for(int x = 0; x < currentLine.length(); x++)
					{
						if(x >= xFrom && x < xTo)
						{
							text += vectorLines.elementAt(y).toString().charAt(x);
						}
					}
				}
				else
				{
					return text;
				}
			}
			else if(yFrom < yTo)
			{
				String currentLine = vectorLines.elementAt(y).toString();
				
				if(y == yFrom)
				{
					for(int x = 0; x < currentLine.length(); x++)
					{
						if(x >= xFrom)
						{
							text += vectorLines.elementAt(y).toString().charAt(x);
						}
					}
					text += '\n';
				}
				else if(y > yFrom && y < yTo)
				{
					text += vectorLines.elementAt(y).toString() + '\n';
				}
				else if(y == yTo)
				{
					for(int x = 0; x < currentLine.length(); x++)
					{
						if(x < xTo)
						{
							text += vectorLines.elementAt(y).toString().charAt(x);
						}
					}
				}
			}
			else
			{
				return text;
			}
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
			
			stringBuffer.append(chars[i]); // probably bug fixed?
			
			if(i == chars.length - 1)
			{
				vectorLines.addElement(stringBuffer);
			}
		}
		
		if(chars.length == 0)
		{
			vectorLines.addElement(stringBuffer);
		}
		
		setCursorPosition(0, 0);
		updateCaretPosition();
	}
	
	public void setText(String text, int x, int y)
	{
		if(text.indexOf('\n') == -1)
		{
			if(!addToLine(x, y, new StringBuffer(text)))
			{
				return;
			}
			
			setCursorPosition(x + text.length(), y);
			updateCaretPosition();
		}
		else
		{
			if(!divideLine(x, y))
			{
				return;
			}
			
			String firstLine = text.substring(0, text.indexOf('\n'));
			
			if(!addToLine(x, y, new StringBuffer(firstLine)))
			{
				return;
			}
			
			text = text.substring(text.indexOf('\n') + 1);
			
			char[] chars = text.toCharArray();
			
			StringBuffer stringBuffer = new StringBuffer("");
			
			for(int i = 0; i < chars.length; i++)
			{
				if(chars[i] == '\n' || chars[i] == '\r')
				{
					if(chars[i] == '\n')
					{
						y++;
						
						if(!divideLine(0, y))
						{
							return;
						}
						
						setLine(y, stringBuffer);
						
						stringBuffer = new StringBuffer();
					}
					continue;
				}
				
				stringBuffer.append(chars[i]);
				
				if(i == chars.length - 1)
				{
					y++;
					addToLine(0, y, stringBuffer);
					setCursorPosition(stringBuffer.toString().length(), y);
				}
			}
			
			updateCaretPosition();
		}
	}

	public void removeText(int xFrom, int yFrom, int xTo, int yTo)
	{
		yFrom = Math.max(0, yFrom);
		yTo = Math.min(yTo, vectorLines.size() - 1);
		
		// inverse order needed!
		for(int y = yTo; y >= yFrom; y--)
		{
			if(yFrom == yTo)
			{
				if(xFrom < xTo)
				{
					removeFromLine(xFrom, xTo, y);
				}
				else
				{
					return;
				}
			}
			else if(yFrom < yTo)
			{
				if(y == yFrom)
				{
					removeFromLine(xFrom, vectorLines.elementAt(y).toString().length(), y);
				}
				else if(y > yFrom && y < yTo)
				{
					removeLine(y);
				}
				else if(y == yTo)
				{
					removeFromLine(0, xTo, y);
				}
			}
			else
			{
				return;
			}
		}
		setCursorPosition(xFrom, yFrom);
		updateCaretPosition();
	}
}