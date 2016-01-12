package wintermute1;
import battlecode.common.*;

import java.util.*;

/* Rough overview of what the turrets will do: 
 * 
 */

//don't want turrets to just keep running away

public class Turret 
{
	public static Random rand;
	public static RobotController rc;
	
	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		Team enemyTeam = rc.getTeam().opponent();
		
		while(true)
		{	
			//attack any foes sending messages if you can
			//should save locs of all foes? May get outdated fast
			if(rc.isWeaponReady())
			{
				Signal[] signals = rc.emptySignalQueue();
				for(Signal signal : signals)
				{
					Team signalTeam = signal.getTeam();
					if(signalTeam == enemyTeam || signalTeam == Team.ZOMBIE)
					{
						MapLocation foeLoc = signal.getLocation();
						if(rc.canAttackLocation(foeLoc))
						{
							rc.attackLocation(foeLoc);
							break;
						}
					}
				}
			}
			//might have to Clock.yield() earlier

			//otherwise get foes in your attackRange
			RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotPlayer.myType.attackRadiusSquared);
			if(foes.length > 0)
			{
				if(rc.isWeaponReady())
				{
					rc.attackLocation(foes[0].location);
				}
				//go through foes, move away if necessary and go into TTM loop?
			}
			//pack up if enemies too close and move away (still stay a little away from archons)
				//go into a TTM while loop
					//unpack if there aren't any enemies too close?, break out of loop
			//attack if you can
			//clump with other turrets?
			Clock.yield();
		}
	}
}
