package wintermute1;
import battlecode.common.*;

import java.util.*;

public class Archon 
{
	public static Random rand;
	public static boolean hasBuiltScout = false;
	public static RobotController rc;
	public static ArrayList<MapLocation> locationsWithParts = new ArrayList<MapLocation>();
	public static MapLocation goal = null;
	public static RobotType typeToBuild = RobotType.SOLDIER;

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
			MapLocation[] nearbyMapLocations = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), rc.getType().sensorRadiusSquared);
			
			//read signals
			Signal[] signals = rc.emptySignalQueue();
			for(Signal signal : signals)
			{
				//check if the signal has parts at the location
				int[] message = signal.getMessage();
				if(message != null && message[0] == Utility.PARTS_CODE)
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
			int robotsAlive = rc.getRobotCount();
			
			/*
			 * OUPUT
			 */
			
			//movement
				//calculate the goal
				//then move there
		
					//move away from enemies
					//move to parts
					//move to defensive locations
			
			//if there are no foes, move randomly
			if(foes.size() == 0)
			{
				//move randomly
			}
			else // if there are foes, find the quickest path to get away
			{
				
			}
			
			
			//build
			if(hasBuiltScout == false)//build a scout if you haven't built one yet
			{
				buildRobot(RobotType.SCOUT);
				hasBuiltScout = true;
			}
			
			//check if you should build
			if(foes.size() <= Utility.MAX_FOES_TO_BUILD && goal == null && rc.getTeamParts() > RobotType.TURRET.partCost)
			{
				//if the current type to build != null, build one of that type
				if(typeToBuild != null && rc.hasBuildRequirements(typeToBuild))
				{
					buildRobot(typeToBuild);
					typeToBuild = null;
				}
				else
				{
					double percent = Math.random();
					rc.setIndicatorString(0, percent + "");
					if(percent <= Utility.PERCENTAGE_TURRETS) //build turret
					{
						typeToBuild = RobotType.TURRET;
					}
					else //build a soldier
					{
						typeToBuild = RobotType.SOLDIER;
					}
				}
			}
			
			//signal
			
			Clock.yield();
		}
	}
	
	//build a robot of a given type
	public static void buildRobot(RobotType type) throws GameActionException
	{
		if(rc.isCoreReady() && rc.hasBuildRequirements(type))
		{
			Direction[] values = Direction.values();
			for(Direction dir : values)
			{
				if(rc.canBuild(dir, type))
				{
					rc.build(dir, type);
					return;
				}
			}
		}
	}
}