import java.util.*;
import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.lcdui.*;
// TODO: keep current dir
class FileBrowser implements CommandListener
{
	private static final String UP_DIRECTORY = "..";
	private static final String ROOT = "/";
	private static final String SEP_STR = "/";
	private static final char SEP = '/';
	private String currDirName;
	
	private String currFileName;
	
	public final static Command select = new Command("Select", Command.ITEM, 1);
	public final static Command cancel = new Command("Cancel", Command.BACK, 2);
	private static Command create = new Command("Create", Command.ITEM, 3);
	
	private static Command createConfirm = new Command("OK", Command.OK, 1);
	private static Command createCancel = new Command("Back", Command.BACK, 2);
	
	private TextField nameTextField;
	private Form newFileForm;
	
	private CommandListener externalListener;
	
	private Display display;
	private Displayable current;
	
	private FileConnection fileConnection;
	
	public FileBrowser(CommandListener externalListener, Display display, Displayable current)
	{
		this.display = display;
		this.current = current;
		this.externalListener = externalListener;
		
		currDirName = ROOT;
	}
	
	public void browse()
	{
		showCurrDir();
	}
	
	public void setCurrentDir(String currentDirName)
	{
		if(currentDirName != null)
		{
			this.currDirName = currentDirName;
		}
	}
	
	public String getCurrentDir()
	{
		return currDirName;
	}
	
	private Displayable displayable;
	
	public void commandAction(Command c, Displayable d)
	{
		if(c == select)
		{
			displayable = d;
			List curr = (List)d;
			final String currFile = curr.getString(curr.getSelectedIndex());
			new Thread(
				new Runnable()
				{
					public void run()
					{
						if(currFile.endsWith(SEP_STR) || currFile.equals(UP_DIRECTORY))
						{
							traverseDirectory(currFile);
						}
						else
						{
							currFileName = currFile;
							
							if(externalListener != null) externalListener.commandAction(select, displayable);
						}
					}
				}
			).start();
		}
		else if(c == cancel)
		{
			if(externalListener != null) externalListener.commandAction(cancel, d);
		}
		else if(c == create)
		{
			createFileForm();
		}
		else if(c == createConfirm)
		{
			String newName = nameTextField.getString();

			if((newName == null) || newName.equals(""))
			{
				Alert alert = new Alert("Error!", "File Name is empty.", null, AlertType.ERROR);
				alert.setTimeout(Alert.FOREVER);
				display.setCurrent(alert);
			}
			else
			{
				// Create file in a separate thread and disable all commands
				// except for "Cancel"
				executeCreateFile(newName, false);
				
				newFileForm.removeCommand(createConfirm);
				newFileForm.removeCommand(createCancel);
			}
		}
		else if(c == createCancel)
		{
			showCurrDir();
		}
	}
	
	void showCurrDir()
	{
		Enumeration e;
		FileConnection currDir = null;
		List browser;

		try
		{
			if(ROOT.equals(currDirName))
			{
				e = FileSystemRegistry.listRoots();
				browser = new List(currDirName, List.IMPLICIT);
			}
			else
			{
				currDir = (FileConnection)Connector.open("file://localhost/" + currDirName);
				e = currDir.list();
				browser = new List(currDirName, List.IMPLICIT);
				// not root - draw UP_DIRECTORY
				browser.append(UP_DIRECTORY, null);
			}

			while(e.hasMoreElements())
			{
				String fileName = (String)e.nextElement();

				if(fileName.charAt(fileName.length() - 1) == SEP)
				{
					// This is directory
					browser.append(fileName, null);
				}
				else
				{
					// this is regular file
					browser.append(fileName, null);
				}
			}

			browser.setSelectCommand(select);

			//Do not allow creating files/directories beside root
			if(!ROOT.equals(currDirName))
			{
				browser.addCommand(create);
			}

			browser.addCommand(cancel);

			browser.setCommandListener(this);

			if(currDir != null)
			{
				currDir.close();
			}

			display.setCurrent(browser);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

	void traverseDirectory(String fileName)
	{
		// In case of directory just change the current directory
		// and show it
		if(currDirName.equals(ROOT))
		{
			if(fileName.equals(UP_DIRECTORY))
			{
				// can not go up from ROOT
				return;
			}
			currDirName = fileName;
		}
		else if(fileName.equals(UP_DIRECTORY))
		{
			// Go up one directory
			int i = currDirName.lastIndexOf(SEP, currDirName.length() - 2);

			if(i != -1)
			{
				currDirName = currDirName.substring(0, i + 1);
			}
			else
			{
				currDirName = ROOT;
			}
		}
		else
		{
			currDirName = currDirName + fileName;
		}
		
		showCurrDir();
	}


	// Starts creatFile with another Thread
	private void executeCreateFile(final String newName, final boolean isDirectory)
	{
		new Thread(
			new Runnable()
			{
				public void run()
				{
					createFile(newName, isDirectory);
				}
			}
		).start();
	}
    
	void createFileForm()
	{
		newFileForm = new Form("New File");
		nameTextField = new TextField("Enter Name", null, 256, TextField.ANY);
		newFileForm.append(nameTextField);
		newFileForm.addCommand(createConfirm);
		newFileForm.addCommand(createCancel);
		newFileForm.setCommandListener(this);
		display.setCurrent(newFileForm);
	}

	void createFile(String newName, boolean isDirectory)
	{
		try
		{
			FileConnection fileConnection = (FileConnection)Connector.open("file:///" + currDirName + newName);

			if(isDirectory)
			{
				fileConnection.mkdir();
			}
			else
			{
				fileConnection.create();
			}

			showCurrDir();
		}
		catch(Exception e)
		{
			String s = "Can not create file '" + newName + "'";

			if((e.getMessage() != null) && (e.getMessage().length() > 0))
			{
				s += ("\n" + e);
			}

			Alert alert = new Alert("Error!", s, null, AlertType.ERROR);
			alert.setTimeout(Alert.FOREVER);
			display.setCurrent(alert);
			// Restore the commands that were removed in commandAction()
			newFileForm.addCommand(createConfirm);
			newFileForm.addCommand(createCancel);
		}
	}


	public void writeFile(String text) throws IOException
	{
		FileConnection fileConnection = (FileConnection)Connector.open("file://localhost/" + currDirName + currFileName, Connector.READ_WRITE);
		if(fileConnection.exists())
		{
			fileConnection.truncate(0);

			OutputStream outputStream = fileConnection.openOutputStream(0);

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");

			outputStreamWriter.write(text);
			
			outputStreamWriter.close();
			
			outputStream.close();
			
			fileConnection.close();
		}
	}
	
	public String readFile() throws IOException
	{
        FileConnection fileConnection = (FileConnection)Connector.open("file://localhost/" + currDirName + currFileName, Connector.READ);
		String text = "";
		if(fileConnection.exists())
		{
			InputStream inputStream = fileConnection.openInputStream();

			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");

			StringBuffer buffer = new StringBuffer();
			int total = 0;
			int read = 0;
			while((read = inputStreamReader.read()) >= 0)
			{
				total++;
				buffer.append((char)read);
			}

			inputStreamReader.close();
			
			inputStream.close();
			
			text = buffer.toString();
			
			fileConnection.close();
		}
		
		return text;
	}


}