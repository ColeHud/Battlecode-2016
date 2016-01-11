package wintermute1;
import battlecode.common.*;

import java.util.*;

/* Rough overview of what the turrets will do: 
 * 
 */

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
			// check messages for enemy within sight range but not attack range
			// pack up if enemies too close and move away (still stay a little away from archons)
			// unpack if there aren't any enemies too close
			// clump with other turrets?
			Clock.yield();
		}
	}
}
