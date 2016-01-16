package wintermute4squad;
import battlecode.common.*;

import java.util.*;

public class Scout 
{
	public static RobotController rc;
	public static Random rand;
	
	//for pathing
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>();
	
	public static MapLocation[] enemyArchonsInitialPosition;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		
		enemyArchonsInitialPosition = rc.getInitialArchonLocations(rc.getTeam().opponent());
		
		while(true)
		{
			

			Clock.yield();
		}
	}
		
}
