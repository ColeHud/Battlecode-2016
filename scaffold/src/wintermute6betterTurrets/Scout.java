package wintermute6betterTurrets;
import battlecode.common.*;

import java.util.*;

public class Scout 
{
	public static RobotController rc;
	public static Random rand;
	
	//for pathing
	public static ArrayList<MapLocation> slugTrail = new ArrayList<MapLocation>();
	
	public static MapLocation[] enemyArchonsInitialPosition;
	
	//scouting
	public static MapLocation pivotLocation = null;
	public static Direction directionToMove = null;
	
	//broadcasting
	public static int broadcastsThisTurn = 0;
	public static MapLocation spawnLocation;

	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		rand = new Random(rc.getID());
		
		enemyArchonsInitialPosition = rc.getInitialArchonLocations(rc.getTeam().opponent());
		
		pivotLocation = rc.getLocation();
		spawnLocation = pivotLocation;
		while(true)
		{
			//sense nearby friends
			RobotInfo[] nearbyFriends = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, rc.getTeam());
			RobotInfo closestTurret = null;
			int shortestDistanceToTurret = 10000;
			for(RobotInfo friend : nearbyFriends)
			{
				if(friend.type == RobotType.TURRET || friend.type == RobotType.TTM)
				{
					
				}
			}
			

			Clock.yield();
		}
	}	
	
	//broadcast
	public static void broadcast(int message1, int message2) throws GameActionException
	{
		if(broadcastsThisTurn < 10)
		{
			//calculate the radius to broadcast
			int radius = rc.getLocation().distanceSquaredTo(spawnLocation);
			
			rc.broadcastMessageSignal(message1, message2, radius);
		}
	}
	
	//get a new direction to move
	public static void getNewDirection()
	{
		pivotLocation = rc.getLocation();
		
		//get a new direction to go
		ArrayList<Direction> possibleDirections = Utility.arrayListOfDirections();
		possibleDirections.remove(directionToMove);
		possibleDirections.remove(directionToMove.rotateRight());
		possibleDirections.remove(directionToMove.rotateLeft());
		possibleDirections.remove(directionToMove.opposite());
		
		directionToMove = possibleDirections.get(rand.nextInt(possibleDirections.size()));
	}
}
