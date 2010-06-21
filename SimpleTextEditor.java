import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

public class SimpleTextEditor extends MIDlet //  implements CommandListener
{
	// private Command exitCommand;
	
	private Display display;
	
	private Displayable current = null;
	
	private ExtendedTextField textField;
	
	public SimpleTextEditor()
	{
		display = Display.getDisplay(this);
		
		textField = new ExtendedTextField();
		
		textField.midlet = this;
		
		// exitCommand = new Command("Exit", Command.EXIT, 1);
	}
	
	/*
	public void commandAction(Command c, Displayable s)
	{
		if(c == exitCommand)
		{
			// Cleanup and notify that the MIDlet has exited
			destroyApp(false);
			notifyDestroyed();
		}
	}
	*/
	
	protected void startApp() throws MIDletStateChangeException
	{
		// Display.getDisplay(this).setCurrent(new ExtendedTextField());
		
		if(current == null)
		{
			// temp:
			display.setCurrent(textField);
		}
		else
		{
			display.setCurrent(current);
		}
	}
	
	protected void pauseApp()
	{
		current = display.getCurrent();
	}
	
	protected void destroyApp(boolean unconditional)
	{
		
	}
}