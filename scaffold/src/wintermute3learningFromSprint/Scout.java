package wintermute3learningFromSprint;
import battlecode.common.*;

public class Scout 
{
	public static RobotController rc;

	public static void run()
	{
		rc = RobotPlayer.rc;
		while(true)
		{
			Clock.yield();
		}
	}
}
