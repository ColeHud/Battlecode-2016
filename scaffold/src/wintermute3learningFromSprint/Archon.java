package wintermute3learningFromSprint;
import java.util.ArrayList;
import battlecode.common.*;

public class Archon 
{
	public static RobotController rc;
	public static int strategyNumber = 0;
	//0 = spawn, 1 = turtle, 2 = big army, 3 = run away

	//slug trail for pathing
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>(20);

	//initial stuff
	public static MapLocation averageArchonLocation;
	public static int[] zombieSpawnRounds;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;

		zombieSpawnSchedule();
		findAverageArchonLocation();

		while(true)
		{
			
			
			Clock.yield();
		}
	}
	
	//build a robot based on the strategy
	public static void buildStrategicRobot()
	{
		if(strategyNumber == 1)//turtle
		{
			
		}
		else if(strategyNumber == 2)//big army
		{
		
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
