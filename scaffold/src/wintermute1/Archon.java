package wintermute1;
import battlecode.common.*;

import java.util.*;

public class Archon 
{
	public static Random rand;
	public static boolean hasBuiltScout = false;
	public static RobotController rc;
	public static ArrayList<MapLocation> locationsWithParts = new ArrayList<MapLocation>();
	public static ArrayList<MapLocation> nearbyLocations = new ArrayList<MapLocation>();

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());

		while(true)
		{	
			/*
			 * INPUT
			 */
			//sense locations around you
			MapLocation[] nearbyMapLocationsArray = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), rc.getType().sensorRadiusSquared);
			nearbyLocations = Utility.arrayToArrayList(nearbyMapLocationsArray);
			
			//read signals
			Signal[] signals = rc.emptySignalQueue();
			for(Signal signal : signals)
			{
				//check if the signal has parts at the location
				int[] message = signal.getMessage();
				if(message != null && message[0] == 94572)
				{
					//add that location to the locationsWithParts arraylist
					locationsWithParts.add(signal.getLocation());
				}
			}
			
			//sense robots
			RobotInfo[] robots = rc.senseNearbyRobots();
			ArrayList<RobotInfo> foes = new ArrayList<RobotInfo>();
			for(RobotInfo robot : robots)
			{
				if(robot.team == Team.ZOMBIE || robot.team == rc.getTeam().opponent())//if the robot is a foe
				{
					foes.add(robot);
				}
			}
			
			//check stats
			double health = rc.getHealth();
			int infectedTurns = rc.getInfectedTurns();
			
			/*
			 * OUPUT
			 */
			
			//move
				//move away from enemies
				//move to parts
			if(foes.size() > 0) // if there are enemies
			{
				//move to the point that gets you farthest away from them out of the 8 directions you can move
				Direction[] possibleDirections = Direction.values();
				
			}
			
			
			//build
			
			//signal
			
			Clock.yield();
		}
	}
}
