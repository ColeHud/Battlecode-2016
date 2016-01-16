package wintermute3learningFromSprint;
import java.util.ArrayList;
import battlecode.common.*;
import java.util.*;

public class Archon 
{
	public static RobotController rc;
	public static int strategyNumber = 0;
	//0 = spawn, 1 = turtle, 2 = big army, 3 = run away

	//slug trail for pathing
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>(20);

	//initial stuff
	public static MapLocation averageArchonLocation;
	public static boolean madeItToAverageArchonLocation = false;
	public static int[] zombieSpawnRounds;
	
	//build orders
	public static RobotType[] turtleBuildOrder = {RobotType.GUARD, RobotType.TURRET, RobotType.SCOUT, RobotType.TURRET, RobotType.SOLDIER};
	public static RobotType[] bigArmyBuildOrder = {RobotType.SOLDIER, RobotType.GUARD, RobotType.SOLDIER, RobotType.TURRET, RobotType.SCOUT};
	public static int currentBuildNumber = 0;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;

		zombieSpawnSchedule();
		findAverageArchonLocation();

		while(true)
		{
			RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.ARCHON.sensorRadiusSquared);

			if(foes.length > 0)//if there are foes nearby
			{
				moveToLocation(findSaferLocation());
			}
			else
			{
				meetUpWithOtherArchons();
				buildStrategicRobot();
			}

			Clock.yield();
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

	//meet up with the other archons at the start of the game if you're going to turtle
	public static void meetUpWithOtherArchons() throws GameActionException
	{
		if(strategyNumber == 1 && madeItToAverageArchonLocation == false && rc.getRoundNum() < 100)
		{
			moveToLocation(averageArchonLocation);
			if(rc.getLocation().distanceSquaredTo(averageArchonLocation) < 5)
			{
				madeItToAverageArchonLocation = true;
			}
		}
	}

	//build a robot based on the strategy
	public static void buildStrategicRobot() throws GameActionException
	{
		if(rc.getTeamParts() >= RobotType.TURRET.partCost)
		{
			if(strategyNumber == 1)//turtle
			{
				rc.setIndicatorString(0, ""+currentBuildNumber);
				buildRobot(turtleBuildOrder[currentBuildNumber]);
			}
			else if(strategyNumber == 2)//big army
			{
				rc.setIndicatorString(0, "Big Army");
				buildRobot(bigArmyBuildOrder[currentBuildNumber]);
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
					currentBuildNumber++;//iterate the bot to build
					if(currentBuildNumber >= turtleBuildOrder.length)
					{
						currentBuildNumber = 0;
					}
					return;
				}
			}
		}
	}

	//get zombie spawn schedule and determine initial strategy off of it
	public static void zombieSpawnSchedule()
	{
		//get the zombie spawn schedule
		ZombieSpawnSchedule zombieSchedule = rc.getZombieSpawnSchedule();
		zombieSpawnRounds = zombieSchedule.getRounds();

		//determine the initial strategy
		int initialSpawnRound = zombieSpawnRounds[0];
		int currentRound = rc.getRoundNum();
		if((initialSpawnRound - currentRound) < Utility.MINIMUM_TIME_TO_TURTLE)
		{
			strategyNumber = 2; //big army if you don't have much time
		}
		else
		{
			strategyNumber = 1; //turtle if you've got time
		}
	}

	//get the locations of friendly archons and go to the average location
	public static void findAverageArchonLocation()
	{
		MapLocation[] myTeamArchons = rc.getInitialArchonLocations(rc.getTeam());
		int averageX = 0;
		int averageY = 0;
		for(MapLocation archon : myTeamArchons)
		{
			averageX += archon.x;
			averageY += archon.y;
		}
		averageX /= myTeamArchons.length;
		averageY /= myTeamArchons.length;
		averageArchonLocation = new MapLocation(averageX, averageY);
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
						rc.setIndicatorString(0, "Trying to move");
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
							rc.setIndicatorString(0, "Trying to move");
							rc.move(candidateDirection);
							return;
						}

					}
				}
			}
		}
	}

}
