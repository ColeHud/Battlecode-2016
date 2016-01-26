package wintermute7scouts;
import battlecode.common.*;

import java.util.*;

public class Archon 
{
	public static RobotController rc;
	public static int strategyNumber = 0;
	//0 = spawn, 1 = build army, 2 = run away

	//slug trail for pathing
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>(20);

	//build order
	//public static RobotType[] buildOrder = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.GUARD, RobotType.SOLDIER, RobotType.TURRET, RobotType.SOLDIER, 
	//RobotType.SOLDIER, RobotType.TURRET, RobotType.GUARD, RobotType.VIPER};
	public static RobotType[] buildOrder = {RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SCOUT, RobotType.TURRET, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER};
	public static int currentBuildNumber = 0;
	public static int numberOfInitialArchons;
	public static Random rand;

	//goals
	public static MapLocation goal = null;
	public static boolean goalIsNeutralBot = false;
	public static int roundsGoingAfterGoal = 0;
	public static ArrayList<MapLocation> goalsToAvoid = new ArrayList<MapLocation>();


	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		numberOfInitialArchons = rc.getInitialArchonLocations(rc.getTeam()).length;
		rand = new Random(rc.getID());

		while(true)
		{
			//GOAL COMPLETION
			if(goal != null)
			{
				if(rc.getLocation().distanceSquaredTo(goal) == 1 && goalIsNeutralBot)
				{
					if(rc.isCoreReady())
					{
						rc.activate(goal);
					}
					goal = null;
				}

				if(rc.getLocation().equals(goal))
				{
					goal = null;
				}
				else if(roundsGoingAfterGoal > 20)
				{
					goalsToAvoid.add(goal);
					goal = null;
				}
			}

			//EVASION CODE
			RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.ARCHON.sensorRadiusSquared);
			RobotInfo[] friends = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, rc.getTeam());
			
			if(foes.length > 0 && rc.getRoundNum() % 4 == 0)
			{
				rc.broadcastSignal(25);
			}

			int numberOfFoesNearAttackRadius = 0;
			if(foes.length > 0)
			{
				for(RobotInfo foe : foes)
				{
					MapLocation currentLocation = rc.getLocation();
					MapLocation foeLocation = foe.location;
					if(currentLocation.distanceSquaredTo(foeLocation) > foe.type.attackRadiusSquared - 1)
					{
						//moveToLocation(findSaferLocation(foes));//don't want to do anything else if you're evading
						moveToLocation(findSaferLocation());
						break;
					}
				}
			}
			else//if there are no enemies nearby, then try to move to the goal or build
			{
				//BUILDING CODE
				double chancesOfBuilding = 1.0/(double)numberOfInitialArchons;
				double random = rand.nextFloat();
				if(random <= chancesOfBuilding && canBuildSomething())//you get to build! :)
				{
					buildStrategicRobot();
					continue;
				}
				else//if you can't build anything, go look for some parts/move to the current goal
				{
					if(goal != null)
					{
						moveToLocation(goal);
					}
					else
					{
						//GO ACTIVATE NEUTRAL BOTS
						RobotInfo[] neutralBots = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.NEUTRAL);
						if(neutralBots.length > 0)
						{
							goal = neutralBots[0].location;
							goalIsNeutralBot = true;
						}
						else
						{
							findNearbyPartsAndSetGoal();
						}
					}
				}
			}




			Clock.yield();
		}
	}

	//evade nearby foes
	public static void evadeNearbyFoes(RobotInfo[] foes) throws GameActionException
	{
		MapLocation currentLocation = rc.getLocation();
		ArrayList<Direction> directions = Utility.arrayListOfDirections();

		//get the average direction to them
		int averageDirection = 0;
		for(RobotInfo foe : foes)
		{
			averageDirection += directions.indexOf(currentLocation.directionTo(foe.location));
		}
		if(foes.length > 0)
		{
			rc.broadcastSignal(25);
			averageDirection /= foes.length;
			Direction directionToEnemies = directions.get(averageDirection);

			Direction averageDirectionAwayFromFoes = directionToEnemies.opposite();
			
			if(rc.isCoreReady())
			{
				if(rc.canMove(averageDirectionAwayFromFoes))
				{
					rc.move(averageDirectionAwayFromFoes);
				}
				else//your core is ready, but it seems like that direction is blocked
				{
					//try the other directions starting with that direction
					Direction rotatedRight = averageDirectionAwayFromFoes.rotateRight();
					Direction rotatedLeft = averageDirectionAwayFromFoes.rotateLeft();
					int tries = 0;

					//only go one loop around
					while(tries < 4)
					{
						if(rc.canMove(rotatedRight))
						{
							rc.move(rotatedRight);
							return;
						}
						else if(rc.canMove(rotatedLeft))
						{
							rc.move(rotatedLeft);
							return;
						}
						else
						{
							tries++;
						}
					}
				}
			}
		}
	}

	//find nearby parts and make that the new goal
	public static void findNearbyPartsAndSetGoal()
	{
		//FIND PARTS
		MapLocation[] nearbyParts = rc.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
		if(nearbyParts.length > 0)
		{
			//find the location with the most parts
			double mostParts = 0;
			MapLocation locationWithMostParts = null;
			for(MapLocation partsLocation : nearbyParts)
			{
				double partsAtLocation = rc.senseParts(partsLocation);
				if(partsAtLocation > mostParts && goalsToAvoid.contains(partsLocation) == false)//there are a lot of parts and this isn't a goal to avoid
				{
					mostParts = partsAtLocation;
					locationWithMostParts = partsLocation;
				}
			}
			if(locationWithMostParts != null)
			{
				goal = locationWithMostParts;
				goalIsNeutralBot = false;
			}
		}
		else
		{
			RobotInfo[] nearbyFriends = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, rc.getTeam());
			
			if(nearbyFriends.length > 0)
			{
				//find the nearest friend and make that the goal
				int shortestDistance = 100000;
				RobotInfo closestFriend = null;
				
				MapLocation currentLocation = rc.getLocation();
				for(RobotInfo friend : nearbyFriends)
				{
					int distance = currentLocation.distanceSquaredTo(friend.location);
					if(distance < shortestDistance)
					{
						shortestDistance = distance;
						closestFriend = friend;
					}
				}
				
				if(closestFriend != null)
				{
					goal = closestFriend.location;
					goalIsNeutralBot = false;
				}
			}
		}
	}

	//find a safe location
	public static MapLocation findSaferLocation()//move to far spot in direction with fewest enemies
	{
		MapLocation currentLocation = rc.getLocation();
		ArrayList<Direction> directions = Utility.arrayListOfDirections();

		//find if foes are within attack range
		RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		ArrayList<RobotInfo> nearAttackRange = new ArrayList<RobotInfo>();

		for(RobotInfo foe : foes)
		{
			RobotType type = foe.type;
			if(type != RobotType.ARCHON && type != RobotType.ZOMBIEDEN && type != RobotType.SCOUT)//only want enemies who can attack
			{
				//if you're close to the attack range
				if(currentLocation.distanceSquaredTo(foe.location) < foe.type.attackRadiusSquared + 4)
				{
					nearAttackRange.add(foe);
				}
			}
		}

		//get the average direction to them
		//ArrayList<Direction> listOfDirections = Utility.arrayListOfDirections();
		int averageDirection = 0;
		for(RobotInfo foe : nearAttackRange)
		{
			averageDirection += directions.indexOf(currentLocation.directionTo(foe.location));
		}
		if(nearAttackRange.size() > 0)
		{
			averageDirection /= nearAttackRange.size();
			Direction directionToEnemies = directions.get(averageDirection);

			//move in that direction as far as you can see
			MapLocation locationToGoTo = currentLocation.add(directionToEnemies.opposite(), (int)Math.sqrt(RobotType.ARCHON.sensorRadiusSquared));
			return locationToGoTo;
		}
		else
		{
			return rc.getLocation();
		}
	}

	//build a robot based on the strategy
	public static void buildStrategicRobot() throws GameActionException
	{
		if(rc.getTeamParts() >= buildOrder[currentBuildNumber].partCost)
		{
			buildRobot(buildOrder[currentBuildNumber]);
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
					currentBuildNumber++;//iterate the bot to build
					if(currentBuildNumber >= buildOrder.length)
					{
						currentBuildNumber = 0;
					}
					return;
				}
			}
		}
	}

	//can build something
	public static boolean canBuildSomething()
	{
		if(rc.isCoreReady() && rc.getTeamParts() > buildOrder[currentBuildNumber].partCost)
		{
			Direction[] directions = Direction.values();
			for(Direction direction : directions)
			{
				if(rc.canBuild(direction, buildOrder[currentBuildNumber]))
				{
					return true;
				}
			}
			return false;
		}
		else
		{
			return false;
		}
	}

	//move to a location
	public static void moveToLocation(MapLocation location) throws GameActionException
	{
		if(rc.isCoreReady())
		{
			MapLocation currentLocation = rc.getLocation();

			//trim the slug trail to size 20
			slugTrail.add(currentLocation);
			if(slugTrail.size() > 20)
			{
				slugTrail.remove(0);
			}

			Direction candidateDirection = currentLocation.directionTo(location);
			MapLocation locationInDirection = currentLocation.add(candidateDirection);
			double rubbleAtLocation = rc.senseRubble(locationInDirection);

			if(slugTrail.contains(locationInDirection))//if you've just been there
			{
				for(int i = 0; i < 8; i++)
				{
					candidateDirection = candidateDirection.rotateRight();
					locationInDirection = currentLocation.add(candidateDirection);
					rubbleAtLocation = rc.senseRubble(locationInDirection);

					if(rc.canMove(candidateDirection) && slugTrail.contains(locationInDirection) == false && rubbleAtLocation < GameConstants.RUBBLE_OBSTRUCTION_THRESH)//move there then return
					{
						rc.move(candidateDirection);
						return;
					}

				}
			}
			else
			{
				if(rc.canMove(candidateDirection) && rubbleAtLocation < GameConstants.RUBBLE_OBSTRUCTION_THRESH)
				{
					rc.move(candidateDirection);
				}
				else
				{
					for(int i = 0; i < 8; i++)
					{
						candidateDirection = candidateDirection.rotateRight();
						locationInDirection = currentLocation.add(candidateDirection);
						rubbleAtLocation = rc.senseRubble(locationInDirection);

						if(rc.canMove(candidateDirection) && slugTrail.contains(locationInDirection) == false && rubbleAtLocation < GameConstants.RUBBLE_OBSTRUCTION_THRESH)//move there then return
						{
							rc.move(candidateDirection);
							return;
						}

					}
				}
			}
		}
	}

}
