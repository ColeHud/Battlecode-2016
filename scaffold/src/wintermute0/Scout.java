package wintermute0;
import java.util.ArrayList;

import battlecode.common.*;

public class Scout 
{
	//variables
	public static RobotController rc = Robot.rc;
	public static MapLocation spawnpoint;
	
	public static RobotInfo[] surroundingRobots;
	public static RobotInfo[] surroundingEnemies;
	public static RobotInfo[] surroundingZombies;
	
	public static void run() throws GameActionException
	{
		spawnpoint = rc.getLocation();
		while(true)
		{
			//sense all of the robots around you
			senseSurroundingBots();
			
			//signals (zombie dens, enemy archons, parts, etc.)
			signals();
			
			//move randomly
			Robot.randomMove();
			
			Clock.yield();
		}
	}
	
	//sense surrounding bots
	public 	static void senseSurroundingBots()
	{
		surroundingRobots = rc.senseNearbyRobots();
		
		surroundingEnemies = new RobotInfo[surroundingRobots.length];
		surroundingZombies = new RobotInfo[surroundingRobots.length];
		int enemies = 0;
		int zombies = 0;
		
		for(int i = 0; i < surroundingRobots.length; i++)
		{
			RobotInfo info = surroundingRobots[i];
			
			if(info.team == Robot.enemy)
			{
				surroundingEnemies[enemies] = info;
				enemies++;
			}
			else if(info.team == Team.ZOMBIE)
			{
				surroundingZombies[zombies] = info;
				zombies++;
			}
		}
	}
	
	//send signals
	public static void signals() throws GameActionException
	{
		foundZombieDenSignal();
		foundEnemyArchonSignal();
		foundPartsSignal();
	}
	
	//found zombie den signal
	public static void foundZombieDenSignal() throws GameActionException
	{
		int distance = rc.getLocation().distanceSquaredTo(spawnpoint) * 2;
		for(RobotInfo enemy : surroundingEnemies)
		{
			if(enemy.type == RobotType.ZOMBIEDEN)
			{
				rc.broadcastMessageSignal(666, 666, distance);
			}
		}
	}
	
	//found enemy archon signal
	public static void foundEnemyArchonSignal() throws GameActionException
	{
		int distance = rc.getLocation().distanceSquaredTo(spawnpoint) * 2;
		for(RobotInfo enemy : surroundingEnemies)
		{
			if(enemy.type == RobotType.ARCHON)
			{
				rc.broadcastMessageSignal(42, 42, distance);
			}
		}
	}
	
	//send a message --117bc
	public static void foundPartsSignal() throws GameActionException
	{
		double partsAtLocation = rc.senseParts(rc.getLocation());
		
		if(partsAtLocation > 5)
		{
			int distance = rc.getLocation().distanceSquaredTo(spawnpoint) * 2;
			rc.broadcastMessageSignal(94572, (int)partsAtLocation, distance);
		}
	}
	
	//send a signal --116bc
	public static void sendSignal() throws GameActionException
	{
		int distance = rc.getLocation().distanceSquaredTo(spawnpoint) * 2;
		
		if(rc.isCoreReady())
		{
			rc.broadcastSignal(distance);
		}
	}
}
