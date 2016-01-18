package team062;
import java.util.*;

import battlecode.common.*;

public class Turret 
{
	public static RobotController rc;

	//slug trail for pathing
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>(20);
	
	//goal maplocation
	public static MapLocation goal = null;
	
	public static Random rand;
	public static Team myTeam;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		myTeam = rc.getTeam();
		while(true)
		{	
			//read signals
			Signal[] signals = rc.emptySignalQueue();
			for(Signal signal : signals)
			{
				int[] message = signal.getMessage();
				if(message != null)
				{
					if(message[0] == Utility.ENEMY_TURTLE)
					{
						goal = signal.getLocation();
					}
				}
			}
			
			//sense the robots around you
			RobotInfo[] nearbyBots = rc.senseNearbyRobots();
			RobotInfo[] nearbyFoes = rc.senseHostileRobots(rc.getLocation(), RobotType.TURRET.sensorRadiusSquared);
			ArrayList<RobotInfo> nearbyFriendlySoldiers = new ArrayList<RobotInfo>();
			for(RobotInfo robot : nearbyBots)
			{
				if(robot.type == RobotType.SOLDIER && robot.team == myTeam)
				{
					nearbyFriendlySoldiers.add(robot);
				}
			}
			
			//ATTACK
			if(nearbyFoes.length > 0)
			{
				//1. Big zombies
				//2. the foe with lowest health
				RobotInfo foeToAttack = null;
				double lowestHealth = 100000;
				for(RobotInfo foe : nearbyFoes)
				{
					if(foe.type == RobotType.BIGZOMBIE)
					{
						foeToAttack = foe;
						break;
					}
					else if(foe.health < lowestHealth)
					{
						lowestHealth = foe.health;
						foeToAttack = foe;
					}
				}
				
				if(foeToAttack != null)
				{
					if(rc.isCoreReady() && rc.isWeaponReady() && rc.canAttackLocation(foeToAttack.location))
					{
						rc.attackLocation(foeToAttack.location);
					}
				}
			}
			
			//get the average nearby soldier location
			int numberOfNearbyFriendlySoldiers = nearbyFriendlySoldiers.size();
			if(numberOfNearbyFriendlySoldiers > 0)
			{
				int averagex = 0;
				int averagey = 0;
				for(RobotInfo soldier : nearbyFriendlySoldiers)
				{
					averagex += soldier.location.x;
					averagey += soldier.location.y;
				}
				averagex /= numberOfNearbyFriendlySoldiers;
				averagey /= numberOfNearbyFriendlySoldiers;
				
				goal = new MapLocation(averagex, averagey);
			}
			
			if(nearbyFoes.length == 0 && goal != null)
			{
				moveToLocation(goal);
			}
			else
			{
				if(rc.getType() == RobotType.TTM)
				{
					rc.unpack();
				}
			}
			
			Clock.yield();
		}
	}
	
	//move to open adjacent location
	public static void moveToOpenAdjacentLocation() throws GameActionException
	{
		ArrayList<Direction> directions = Utility.arrayListOfDirections();
		while(directions.size() > 0)
		{
			int index = rand.nextInt(directions.size());
			if(rc.canMove(directions.get(index)) && rc.isCoreReady())
			{
				rc.move(directions.get(index));
			}
			else
			{
				directions.remove(index);
			}
		}
	}

	//move to a location
	public static void moveToLocation(MapLocation location) throws GameActionException
	{	
		if(rc.isCoreReady())
		{
			if(rc.getType() == RobotType.TURRET)
			{
				rc.pack();
			}

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

					if(rc.isCoreReady() && rc.canMove(candidateDirection) && slugTrail.contains(locationInDirection) == false && rubbleAtLocation < GameConstants.RUBBLE_OBSTRUCTION_THRESH)//move there then return
					{
						rc.setIndicatorString(0, "Trying to move");
						rc.move(candidateDirection);
						return;
					}

				}
			}
			else
			{
				if(rc.isCoreReady() && rc.canMove(candidateDirection) && rubbleAtLocation < GameConstants.RUBBLE_OBSTRUCTION_THRESH)
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

						if(rc.isCoreReady() && rc.canMove(candidateDirection) && slugTrail.contains(locationInDirection) == false && rubbleAtLocation < GameConstants.RUBBLE_OBSTRUCTION_THRESH)//move there then return
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
