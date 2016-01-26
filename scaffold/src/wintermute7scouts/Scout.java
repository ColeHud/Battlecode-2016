package wintermute7scouts;
import battlecode.common.*;

import java.util.*;

public class Scout 
{
	public static RobotController rc;
	public static Random rand;
	public static RobotInfo turretToGiveVision = null;
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>();

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		while(true)
		{
			turretToGiveVision = null;
			
			//find all the nearest turrets
			RobotInfo[] nearbyFriends = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, rc.getTeam());
			ArrayList<RobotInfo> nearbyTurrets = new ArrayList<RobotInfo>();
			for(RobotInfo friend : nearbyFriends)
			{
				if(friend.type == RobotType.TURRET || friend.type == RobotType.TTM)
				{
					nearbyTurrets.add(friend);
				}
			}
			
			//move to the nearest one if it exists
			if(nearbyTurrets.size() > 0)
			{
				MapLocation currentLocation = rc.getLocation();
				RobotInfo closestTurret = null;
				int shortestDistance = 10000;
				for(RobotInfo turret : nearbyTurrets)
				{
					if(currentLocation.distanceSquaredTo(turret.location) < shortestDistance)
					{
						closestTurret = turret;
					}
				}
				
				
				if(closestTurret != null)//if there is a close turret
				{
					if(shortestDistance <= 4)
					{
						//broadcast
						turretToGiveVision = closestTurret;
						broadcastEnemyLocationForNearbyTurret();
					}
					else
					{
						moveToLocation(closestTurret.location);
					}
				}
			}
			else//if there are no turrets nearby, go to the nearest friend
			{
				if(nearbyFriends.length > 0)
				{
					MapLocation currentLocation = rc.getLocation();
					int distanceToClosestFriend = 100000;
					RobotInfo closestFriend = null;
					
					for(RobotInfo friend : nearbyFriends)
					{
						int distance = currentLocation.distanceSquaredTo(friend.location);
						if(distance < distanceToClosestFriend)
						{
							distanceToClosestFriend = distance;
							closestFriend = friend;
						}
					}
					
					if(closestFriend != null)
					{
						moveToLocation(closestFriend.location);
					}
				}
			}


			Clock.yield();
		}
	}

	//broadcast an enemy location
	public static void broadcastEnemyLocationForNearbyTurret() throws GameActionException
	{
		//broadcast enemy locations to nearby turrets
		RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		if(foes.length > 0 && turretToGiveVision != null)
		{
			for(RobotInfo foe : foes)
			{
				int distance = foe.location.distanceSquaredTo(turretToGiveVision.location);
				if(distance > 24 && distance <= 48)
				{
					int xToBroadcast = Integer.parseInt((Utility.SCOUT_TURRET_VISION_PREFACE_CODE + foe.location.x));
					int yToBroadcast = Integer.parseInt((Utility.SCOUT_TURRET_VISION_PREFACE_CODE + foe.location.y));
					rc.broadcastMessageSignal(xToBroadcast, yToBroadcast, 4);
					return;
				}
			}
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
