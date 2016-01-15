package wintermute3learningFromSprint;
import java.util.ArrayList;

import battlecode.common.*;

public class Turret 
{
	public static RobotController rc;

	//slug trail for pathing
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>(20);
	
	//goal maplocation
	public static MapLocation goal = null;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		while(true)
		{
			//read scout signal
			
			//attack foes
			RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.TURRET.attackRadiusSquared);
			double lowestHealth = 100000;
			RobotInfo weakestFoe = null;
			
			for(RobotInfo foe : foes)
			{
				if(rc.getLocation().distanceSquaredTo(foe.location) > 5 && foe.health < lowestHealth)
				{
					lowestHealth = foe.health;
					weakestFoe = foe;
				}
			}
			
			if(weakestFoe != null)
			{
				if(rc.getType() == RobotType.TURRET && rc.isCoreReady() && rc.isWeaponReady())
				{
					rc.attackLocation(weakestFoe.location);
					continue;
				}
			}
			
			if(goal != null)
			{
				if(rc.getLocation().equals(goal))
				{
					if(rc.getType() == RobotType.TTM)
					{
						rc.unpack();
					}
					goal = null;
				}
				else
				{
					moveToLocation(goal);
				}
			}
			
			//organize
			//if you're blocking another unit, get out of the way
			RobotInfo[] friends = rc.senseNearbyRobots(4, rc.getTeam());
			if(friends.length > 0)
			{
				for(RobotInfo friend : friends)
				{
					if(rc.getLocation().distanceSquaredTo(friend.location) <= 1)
					{
						//try to move away
						Direction directionAway = rc.getLocation().directionTo(friend.location).opposite();
						goal = rc.getLocation().add(directionAway);
					}
				}
			}
			
			//move

			Clock.yield();
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
