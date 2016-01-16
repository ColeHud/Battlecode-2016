package wintermute4squad;
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
			broadcastsThisTurn = 0;
			//signal
			RobotInfo[] nearbyFoes = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
			if(nearbyFoes.length > 5)
			{
				broadcast(Utility.CLUMP_OF_FOES_CODE, Utility.CLUMP_OF_FOES_CODE);
			}
			int numberOfEnemyTurrets = 0;
			for(RobotInfo foe : nearbyFoes)
			{
				if(foe.type == RobotType.TURRET || foe.type == RobotType.TTM)
				{
					numberOfEnemyTurrets++;
				}
				if(foe.type == RobotType.ZOMBIEDEN)
				{
					broadcast(Utility.ZOMBIE_DEN, Utility.ZOMBIE_DEN);
				}
			}
			if(numberOfEnemyTurrets > 3)
			{
				broadcast(Utility.ENEMY_TURTLE, Utility.ENEMY_TURTLE);
			}
			
			//initial case
			if(directionToMove == null)
			{
				//choose a random direction to move in
				Direction[] possibleDirections = Direction.values();
				directionToMove = possibleDirections[rand.nextInt(possibleDirections.length)];
			}
			
			//every other case
			if(rc.canMove(directionToMove))
			{
				//if there are foes nearby, choose another direction
				/*
				RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
				if(foes.length > 0)
				{
					//set the direction to move to the enemy so you move away from it
					directionToMove = rc.getLocation().directionTo(foes[0].location);
					getNewDirection();
					if(rc.canMove(directionToMove) && rc.isCoreReady())
					{
						rc.move(directionToMove);
					}
				}
				*/
				/*else */if(rc.isCoreReady())//if there aren't enemies nearby, move int the current direction
				{
					rc.move(directionToMove);
				}
			}
			else
			{
				//if you can't move in the current direction, find a new one
				getNewDirection();
			}

			Clock.yield();
		}
	}	
	
	//broadcast
	public static void broadcast(int message1, int message2) throws GameActionException
	{
		if(broadcastsThisTurn < 20)
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
