package wintermute3learningFromSprint;
import java.util.ArrayList;

import battlecode.common.*;

public class Soldier 
{
	public static RobotController rc;

	//slug trail for pathing
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>(20);

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		while(true)
		{
			//find the closest foe and attack it
			RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.GUARD.sensorRadiusSquared);
			int shortestDistance = 1000000;
			RobotInfo closestFoe = null;
			if(foes.length > 0)
			{
				MapLocation currentLocation = rc.getLocation();
				for(RobotInfo foe : foes)
				{
					int distanceToFoe = currentLocation.distanceSquaredTo(foe.location);
					if(distanceToFoe < shortestDistance)
					{
						shortestDistance = distanceToFoe;
						closestFoe = foe;
					}
				}
			}

			if(closestFoe != null)
			{
				if(shortestDistance <= RobotType.GUARD.attackRadiusSquared)
				{
					if(rc.isCoreReady() && rc.isWeaponReady() && rc.canAttackLocation(closestFoe.location))
					{
						rc.attackLocation(closestFoe.location);
					}
				}
				else
				{
					moveToLocation(closestFoe.location);
				}
			}

			Clock.yield();
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
