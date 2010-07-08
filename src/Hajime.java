import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import java.util.Vector;

import org.hecl.Interp;
import org.hecl.HeclException;
import org.hecl.HeclTask;
import org.hecl.ListThing;
import org.hecl.ObjectThing;
import org.hecl.StringThing;
import org.hecl.Thing;
import org.hecl.midp20.MidletCmd;
import org.hecl.misc.HeclUtils;
import org.hecl.net.HttpCmd;
import org.hecl.net.Base64Cmd;
import org.hecl.rms.RMSCmd;

import java.util.*;
import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;

public class Hajime extends MIDlet
{
	protected Interp interp = null;
	protected HeclTask evaltask = null;
	protected String[] args = {};
	protected boolean started = false;
	
	public Display display;
	
	public Displayable current = null;
	
	private ExtendedTextField textField;
	
	public Hajime()
	{
		display = Display.getDisplay(this);
		
		textField = new ExtendedTextField();
		
		textField.midlet = this;
		
		current = textField;
	}
	
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
		
		if(current == null)
		{
			display.setCurrent(textField);
			current = textField;
		}
		else
		{
			display.setCurrent(current);
		}
		
		try
		{
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

			// if kxml == 1
			org.hecl.kxml.KXMLCmd.load(interp);
			// endif

			// if files == 1
			org.hecl.files.FileCmds.load(interp);
			org.hecl.files.HeclStreamCmds.load(interp);
			org.hecl.files.FileFinderCmds.load(interp);
			// endif

			MidletCmd.load(interp, this);
			
			String scriptcontent =
			HeclUtils.getResourceAsString(this.getClass(), "/script.hcl", "UTF-8");
			
			interp.setVar(":out", new Thing(""));
			
			interp.addCommand("quit", new QuitCmd(display, current)); 
			
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
		
		if(interp.commandExists("midlet.onpause"))
		{
			interp.evalAsync(new Thing("midlet.onpause"));
		}
	}
	
	protected void destroyApp(boolean unconditional)
	{
		
	}
	
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

	public String getOutput()
	{
		try
		{
			return StringThing.get(interp.getVar(":out"));
		}
		catch(Exception e)
		{
			// At least let the user know there was an error.
			Alert a = new Alert("Hecl error", e.toString(), null, null);
			Display display = Display.getDisplay(this);
			display.setCurrent(a);
			
			System.err.println("Error in getOutput: " + e);
		}
		return "";
	}

	public void clearOutput()
	{
		interp.setVar(":out", new Thing("")); 
	}
}