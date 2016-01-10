package wintermute1;
import battlecode.common.*;

import java.util.*;

public class Turret 
{
	public static Random rand;
	public static RobotController rc;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());

		while(true)
		{

			Clock.yield();
		}
	}
}
