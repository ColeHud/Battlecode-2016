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
			/* // check messages for enemy within sight range but not attack range
			Signal[] signals = rc.emptySignalQueue();
			for(Signal signal : signals)
			{
				//have code to tell turrets to fire somewhere if they can?
				
				//do something
			}
			*/
			//might have to Clock.yield() earlier
			//otherwise get foes in your attackRange
			//pack up if enemies too close and move away (still stay a little away from archons)
				//go into a TTM while loop
					//unpack if there aren't any enemies too close?, break out of loop
			//attack if you can
			//clump with other turrets?
			Clock.yield();
		}
	}
}
