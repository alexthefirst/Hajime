import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import java.io.IOException;
import java.util.Vector;

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

public class SimpleTextEditor extends MIDlet //  implements CommandListener
{
	protected Interp interp = null;
	protected HeclTask evaltask = null;
	protected String[] args = {};
	protected boolean started = false;
	
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
		if(started)
		{
			if(interp.commandExists("midlet.onresume"))
			{
				interp.evalAsync(new Thing("midlet.onresume"));
			}
			return;
		}
		started = true;
		
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
		
		try
		{
			// Alert a = new Alert("Loading Hecl", "Loading Hecl...", null, null); // AlertType.INFO
			// display.setCurrent(a);
			// a.setTimeout(Alert.FOREVER);
			// a.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
			
			interp = new Interp();
			Vector v = new Vector();
			for(int i = 0; i < args.length; ++i)
			{
				v.addElement(new Thing(args[i]));
			}
			interp.setVar("argv", ListThing.create(v));

			// load extensions into interpreter...
			RMSCmd.load(interp);
			HttpCmd.load(interp);
			Base64Cmd.load(interp);
			//if locationapi == 1
//			try 
//			{
//				Class.forName("javax.microedition.location.Location");
//				org.hecl.location.LocationCmd.load(interp);
//			}
//			catch(Exception e)
//			{
//				
//			}
			//endif

			//if kxml == 1
			org.hecl.kxml.KXMLCmd.load(interp);
			//endif

			//if files == 1
			org.hecl.files.FileCmds.load(interp);
			org.hecl.files.HeclStreamCmds.load(interp);
			org.hecl.files.FileFinderCmds.load(interp);
			//endif

			MidletCmd.load(interp,this);

			//if mwt == 1
			// org.hecl.mwtgui.MwtCmds.load(interp, this);
			//endif
			
			String scriptcontent =
			HeclUtils.getResourceAsString(this.getClass(),"/script.hcl","UTF-8");

			// interp.setVar("splash", ObjectThing.create(a));
			evaltask = interp.evalIdle(new Thing(scriptcontent));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			destroyApp(true);
		}
		
	}
	
	protected void pauseApp()
	{
		current = display.getCurrent();
		/*
		if(interp.commandExists("midlet.onpause"))
		{
			interp.evalAsync(new Thing("midlet.onpause"));
		}
		*/
	}
	
	protected void destroyApp(boolean unconditional)
	{
		
	}
	
	/**
	* The <code>runScript</code> method exists so that external
	* applications (emulators, primarily) can call into Hecl and run
	* scripts.
	*
	* @param s a <code>String</code> value
	*/
	
	public void runScript(String s)
	{
		try
		{
			// First wait for the idleEval call to complete...
			while(evaltask == null)
			{
				Thread.currentThread().yield();
			}
			
			while(!evaltask.isDone())
			{
				try
				{
					synchronized(evaltask)
					{
						evaltask.wait();
					}
				}
				catch(Exception e)
				{
					// ignore
					e.printStackTrace();
				}
			}
			
			interp.eval(new Thing(s));
		}
		catch(Exception e)
		{
			// At least let the user know there was an error.
			Alert a = new Alert("Hecl error", e.toString(), null, null);
			Display display = Display.getDisplay(this);
			display.setCurrent(a);
			// e.printStackTrace();
			System.err.println("Error in runScript: " + e);
		}
	}
	
}