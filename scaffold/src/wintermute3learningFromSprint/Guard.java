package wintermute3learningFromSprint;
import battlecode.common.*;

public class Guard 
{
	public static RobotController rc;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		while(true)
		{
			
			Clock.yield();
		}
	}
}
