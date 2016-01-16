package wintermute4squad;
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
//			//go to the nearest turret and act as vision
//			RobotInfo[] nearbyFriends = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, rc.getTeam());
//			ArrayList<RobotInfo> nearbyTurrets = new ArrayList<RobotInfo>();
//			for(RobotInfo friend : nearbyFriends)
//			{
//				if(friend.type == RobotType.TURRET || friend.type == RobotType.TTM)
//				{
//					nearbyTurrets.add(friend);
//				}
//			}
//			//if your current turret is still near you, then stay there, otherwise, find a new one
//			if(nearbyTurrets.contains(turretToGiveVision))
//			{
//				//move to that turret if you need to
//				if(rc.getLocation().distanceSquaredTo(turretToGiveVision.location) > 1)
//				{
//					moveToLocation(turretToGiveVision.location);
//				}
//			}
//			else//pick a new turret
//			{
//				//choose a random turret to help
//				if(nearbyTurrets.size() > 0)
//				{
//					turretToGiveVision = nearbyTurrets.get(rand.nextInt(nearbyTurrets.size()));
//				}
//			}
			
//			broadcastEnemyLocationForNearbyTurret();

			Clock.yield();
		}
	}

	//broadcast an enemy location
	public static void broadcastEnemyLocationForNearbyTurret() throws GameActionException
	{
		//broadcast enemy locations to nearby turrets
		RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		if(foes.length > 0)
		{
			for(RobotInfo foe : foes)
			{
				int distance = foe.location.distanceSquaredTo(turretToGiveVision.location);
				if(distance > 24 && distance <= 48)
				{
					int xToBroadcast = Integer.parseInt((Utility.SCOUT_TURRET_VISION_PREFACE_CODE + foe.location.x));
					int yToBroadcast = Integer.parseInt((Utility.SCOUT_TURRET_VISION_PREFACE_CODE + foe.location.y));
					rc.broadcastMessageSignal(xToBroadcast, yToBroadcast, 2);
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
