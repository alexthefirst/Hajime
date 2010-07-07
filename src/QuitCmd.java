import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import org.hecl.Command;
import org.hecl.HeclException;
import org.hecl.Interp;
import org.hecl.Thing;

class QuitCmd implements Command
{
	private Display display = null;
	private Displayable current = null;
	
	public QuitCmd(Display display, Displayable current)
	{
		this.display = display;
		this.current = current;
	}
	
	public Thing cmdCode(Interp interp, Thing[] argv) throws HeclException
	{ 
		display.setCurrent(current);
		
		return null;
	}
}
