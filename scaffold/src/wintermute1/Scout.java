package wintermute1;
import battlecode.common.*;

import java.util.*;

public class Scout 
{
	public static Random rand;
	public static RobotController rc;
	public static MapLocation spawn;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		spawn = rc.getLocation();

		while(true)
		{
			scoutMove();
			
			signalOnSight();
			
			Clock.yield();
		}
	}
	
	//dodge enemies - scouts shouldn't be taking damage
	public static void scoutMove() throws GameActionException//incorporates moveRandomly() and a dodge mechanism
	{
		//find if foes are within attack range
		MapLocation currentLocation = rc.getLocation();
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
		
		//randomly move if there aren't any enemies near you
		if(nearAttackRange.size() == 0)
		{
			moveRandomly();
			return;
		}
		
		//get the average direction to them
		ArrayList<Direction> directions = Utility.arrayListOfDirections();
		int averageDirection = 0;
		for(RobotInfo foe : nearAttackRange)
		{
			averageDirection += directions.indexOf(currentLocation.directionTo(foe.location));
		}
		averageDirection /= nearAttackRange.size();
		Direction directionToEnemies = directions.get(averageDirection);
		
		//move away from the enemies
		Direction directionAwayFromEnemies = directionToEnemies.opposite();
		if(rc.canMove(directionAwayFromEnemies) && rc.isCoreReady())
		{
			rc.move(directionAwayFromEnemies);
		}
	}
	
	//signal if you see a den, enemy archon, or parts
	public static void signalOnSight() throws GameActionException
	{
		//foes
		MapLocation currentLocation = rc.getLocation();
		RobotInfo[] foes = rc.senseHostileRobots(currentLocation, RobotType.SCOUT.sensorRadiusSquared);
		int broadcastRange = currentLocation.distanceSquaredTo(spawn) * 2;
		
		for(RobotInfo foe : foes)
		{
			if(foe.type == RobotType.ZOMBIEDEN)// Zombie den
			{
				rc.broadcastMessageSignal(Utility.ZOMBIE_DEN_CODE, Utility.ZOMBIE_DEN_CODE, broadcastRange);
			}
			else if(foe.type == RobotType.ARCHON)//Archon
			{
				rc.broadcastMessageSignal(Utility.ENEMY_ARCHON_CODE, Utility.ENEMY_ARCHON_CODE, broadcastRange);
			}
		}
		
		//parts
		if(rc.senseParts(currentLocation) > 10)
		{
			rc.broadcastMessageSignal(Utility.PARTS_CODE, Utility.PARTS_CODE, broadcastRange);
		}
	}
	
	//move randomly
	public static void moveRandomly() throws GameActionException
	{
		if(rc.isCoreReady())
		{
			boolean hasMoved = false;
			ArrayList<Direction> directions = Utility.arrayListOfDirections();
			
			while(hasMoved == false && directions.size() > 0)
			{
				Direction dirToTry = directions.get(rand.nextInt(directions.size()));
				if(rc.canMove(dirToTry))
				{
					rc.move(dirToTry);
					hasMoved = true;
				}
				else
				{
					directions.remove(dirToTry);
				}
			}
		}
	}
}
