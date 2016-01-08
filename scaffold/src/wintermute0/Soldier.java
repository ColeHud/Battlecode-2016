package wintermute0;
import battlecode.common.*;

public class Soldier
{
	//variables
	public static RobotController rc = Robot.rc;
	
	public static void run() throws GameActionException
	{
		while(true)
		{
			Robot.attackFoes();
			Clock.yield();
		}
	}
}
