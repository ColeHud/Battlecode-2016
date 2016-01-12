package wintermute1;
import battlecode.common.*;

import java.util.*;

public class Archon 
{
	public static Random rand;
	public static RobotController rc;
	public static ArrayList<MapLocation> locationsWithParts = new ArrayList<MapLocation>();
	public static MapLocation goal = null;
	public static boolean goalIsASafeLocation = false;
	public static RobotType typeToBuild = RobotType.SOLDIER;
	
	
	//locations
	public static MapLocation[] nearbyMapLocations;
	public static MapLocation[] nearbyPartsLocations;
	public static ArrayList<MapLocation> past10Locations = new ArrayList<MapLocation>(10);//slug trail
	
	//robots
	public static RobotInfo[] robots;
	public static ArrayList<RobotInfo> foes;
	public static ArrayList<RobotInfo> foesWithinAttackRange;

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
				past10Locations = new ArrayList<MapLocation>();//delete the slug trail after you reach your goal
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
				//add to locationsWithParts arraylist
				if(locationsWithParts.contains(loc) == false)
				{
					locationsWithParts.add(loc);
				}
			
				//find the location with the most parts
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
			robots = rc.senseNearbyRobots();
			foes = new ArrayList<RobotInfo>();
			foesWithinAttackRange = new ArrayList<RobotInfo>();
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
				if(maxParts > 0 && goal == null)//if there are parts nearby
				{
					//make that the goal
					goal = nearbyLocationWithMostParts;
				}
				else if(goal == null)//if there aren't and there is no goal
				{
					//build something or find new parts
					//75% build, 25% new parts
					if(locationsWithParts.size() > 0 && Math.random() > .75)
					{
						goal = locationsWithParts.get(0);
						locationsWithParts.remove(0);
						goalIsASafeLocation = false;
					}
					else
					{
						buildRobots();
					}

					//calculate the next goal - maybe a new parts location you got via signal
				}
				else if(goal != null)//if there is a goal, move there
				{
					moveToLocation(goal);
				}
			}
			else//there are foes nearby
			{
				if(nearbyFoesInAttackRange > 0)
				{
					rc.setIndicatorString(0, "THERE ARE FOES NEARBY");
					if(goal != null && goalIsASafeLocation == false)//if it's not a safe goal, find one
					{
						goal = findSaferLocation();
						goalIsASafeLocation = true;
						moveToLocation(goal);
					}
					else if(goalIsASafeLocation && goal != null)//if it's already a safe location
					{
						moveToLocation(goal);
					}
				}
			}
			
			Clock.yield();
		}
	}
	
	//find a safe location
	public static MapLocation findSaferLocation()//move to far spot in direction with fewest enemies
	{
		MapLocation currentLocation = rc.getLocation();
		ArrayList<Direction> directions = Utility.arrayListOfDirections();
		ArrayList<Integer> enemiesInEachDirection = new ArrayList<Integer>(10);
		
		//initialize the enemiesInEachDirection arraylist
		for(int i = 0; i < 10; i++)
		{
			enemiesInEachDirection.add(0);
		}
		
		for(RobotInfo foe : foes)
		{
			Direction dirToFoe = currentLocation.directionTo(foe.location);
			int index = directions.indexOf(dirToFoe);
			int numberOfFoesInDirection = enemiesInEachDirection.get(index);
			enemiesInEachDirection.set(index, numberOfFoesInDirection++);
		}
		
		int leastEnemies = 1000000;
		int directionWithLeastEnemies = 0;
		for(int i = 0; i<enemiesInEachDirection.size(); i++)
		{
			int numberOfEnemies = enemiesInEachDirection.get(i);
			if(numberOfEnemies < leastEnemies)
			{
				directionWithLeastEnemies = i;
				leastEnemies = numberOfEnemies;
			}
		}
		
		Direction direction = directions.get(directionWithLeastEnemies);//the direction with the fewest enemies
		
		//move in that direction as far as you can see
		MapLocation locationToGoTo = currentLocation.add(direction, (int)Math.sqrt(RobotType.ARCHON.sensorRadiusSquared) / 2);
		return locationToGoTo;
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
			//first, check if you should remove rubble. only if the surrounding squares are empty
			boolean shouldRemoveRubble = false;
			int directionsWithRubble = 0;
			for(Direction d : Direction.values())
			{
				MapLocation added = goal.add(d);
				boolean isFullOfRubble = rc.senseRubble(added) > 50;
				if(isFullOfRubble)
				{
					directionsWithRubble++;
				}
			}
			if(directionsWithRubble > 2)//if it's surrounded then dig
			{
				if(rc.isCoreReady())
				{
					rc.clearRubble(actualDirectionToMove);
				}
			}
			else //if not, path around it
			{
				Direction right = actualDirectionToMove.rotateRight();
				MapLocation rightLoc = currentLoc.add(right);
				
				while(right.equals(actualDirectionToMove) == false)
				{
					if(canMoveThere(right, rightLoc))
					{
						moveInDirection(right);
						right = actualDirectionToMove;
					}
					else
					{
						right = right.rotateRight();
						rightLoc = currentLoc.add(right);
					}
				}
			}
		}
	}
	
	//can move to a location
	public static boolean canMoveThere(Direction d, MapLocation m)
	{
		if(past10Locations.contains(m) == false && rc.senseRubble(m) < 50 && rc.canMove(d))//make sure it's not part of the slug trail and it's not blocked by rubble and you can move there
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	//move in a given direction
	public static void moveInDirection(Direction d) throws GameActionException
	{
		MapLocation m = rc.getLocation().add(d);
		
		if(rc.senseRubble(m) < 50)//if it's less than 20, just move there
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