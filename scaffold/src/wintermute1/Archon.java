package wintermute1;
import battlecode.common.*;

import java.util.*;

public class Archon 
{
	public static Random rand;
	public static RobotController rc;
	public static ArrayList<MapLocation> locationsWithParts = new ArrayList<MapLocation>();
	public static MapLocation goal = null;
	public static RobotType typeToBuild = RobotType.SOLDIER;
	
	
	//locations
	public static MapLocation[] nearbyMapLocations;
	public static MapLocation[] nearbyPartsLocations;
	public static ArrayList<MapLocation> past10Locations = new ArrayList<MapLocation>(10);//slug trail
	
	//signals

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		
		//build scouts right away
		buildRobot(RobotType.SCOUT);

		while(true)
		{	
			/*
			 * INPUT
			 */
			if(rc.getLocation().equals(goal))
			{
				goal = null;//you made it to the goal
			}
			
			//sense locations around you
			nearbyMapLocations = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), rc.getType().sensorRadiusSquared);
			
			//parts locations 
			nearbyPartsLocations = rc.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
			
			//find the nearest mapLocation with the most parts
			double maxParts = 0;
			MapLocation nearbyLocationWithMostParts = null;
			for(MapLocation loc : nearbyPartsLocations)
			{
				double partsAtLoc = rc.senseParts(loc);
				if(partsAtLoc > maxParts)
				{
					maxParts = partsAtLoc;
					nearbyLocationWithMostParts = loc;
				}
			}
			
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
			MapLocation myLoc = rc.getLocation();
			RobotInfo[] robots = rc.senseNearbyRobots();
			ArrayList<RobotInfo> foes = new ArrayList<RobotInfo>();
			ArrayList<RobotInfo> foesWithinAttackRange = new ArrayList<RobotInfo>();
			for(RobotInfo robot : robots)
			{
				if(robot.team == Team.ZOMBIE || robot.team == rc.getTeam().opponent())//if the robot is a foe
				{
					foes.add(robot);
					
					if(myLoc.distanceSquaredTo(robot.location) < robot.type.attackRadiusSquared)
					{
						foesWithinAttackRange.add(robot);
					}
				}
			}
			int nearbyFoes = foes.size();
			int nearbyFoesInAttackRange = foesWithinAttackRange.size();
			
			//check stats
			double health = rc.getHealth();
			int infectedTurns = rc.getInfectedTurns();
			int robotsAlive = rc.getRobotCount();
			
			/*
			 * OUPUT
			 */
			
			//what to do
			if(nearbyFoes == 0)//if there are no foes in sight
			{
				if(nearbyFoesInAttackRange == 0)//if there are no foes in attack range
				{
					if(maxParts > 0 && goal == null)//if there are parts nearby
					{
						//make that the goal
						goal = nearbyLocationWithMostParts;
					}
					else if(goal == null)//if there aren't and there is no goal
					{
						//build something
						buildRobots();
						
						//calculate the next goal - maybe a new parts location you got via signal
					}
					else if(goal != null)//if there is a goal, move there
					{
						moveToLocation(goal);
					}
				}
				else
				{
					//there are nearby bots that will attack. Run Away!!!
				}
			}
			
			Clock.yield();
		}
	}
	
	//move to a maplocation
	public static void moveToLocation(MapLocation m) throws GameActionException
	{
		MapLocation currentLoc = rc.getLocation();
		Direction directionToM = currentLoc.directionTo(m);
		Direction actualDirectionToMove = directionToM;
		
		//deal with the slug trail - trim it to size
		if(past10Locations.size() > 10)
		{
			while(past10Locations.size() > 10)
			{
				past10Locations.remove(past10Locations.size() - 1);
			}
		}
		past10Locations.add(currentLoc);
		
		MapLocation locationToMoveTo = currentLoc.add(directionToM);
		
		if(canMoveThere(actualDirectionToMove, locationToMoveTo))//make sure it's not part of the slug trail and it's not blocked by rubble and you can move there
		{
			moveInDirection(actualDirectionToMove);
		}
		else
		{
			Direction right = actualDirectionToMove.rotateRight();
			Direction left = actualDirectionToMove.rotateLeft();
			MapLocation rightLoc = currentLoc.add(right);
			MapLocation leftLoc = currentLoc.add(left);
			
			while(right != left)
			{
				if(canMoveThere(right, rightLoc))
				{
					moveInDirection(right);
				}
				else if(canMoveThere(left, leftLoc))
				{
					moveInDirection(left);
				}
				else
				{
					right = right.rotateRight();
					left = left.rotateLeft();
					rightLoc = currentLoc.add(right);
					leftLoc = currentLoc.add(left);
				}
			}
		}
	}
	
	//can move to a location
	public static boolean canMoveThere(Direction d, MapLocation m)
	{
		if(past10Locations.contains(m) == false && rc.senseRubble(m) < 20 && rc.canMove(d))//make sure it's not part of the slug trail and it's not blocked by rubble and you can move there
		{
			return true;
		}
		else
		{
			return true;
		}
	}
	
	//move in a given direction
	public static void moveInDirection(Direction d) throws GameActionException
	{
		MapLocation m = rc.getLocation().add(d);
		
		if(rc.senseRubble(m) < 20)//if it's less than 20, just move there
		{
			if(rc.isCoreReady() && rc.canMove(d))
			{
				rc.move(d);
			}
		}
		else//clear it
		{
			if(rc.isCoreReady() && rc.canAttackLocation(m))
			{
				rc.clearRubble(d);	
			}
		}
	}
	
	//build robots
	public static void buildRobots() throws GameActionException
	{
		//check if you should build
		if(rc.getTeamParts() > RobotType.TURRET.partCost)
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