package wintermute0;
import battlecode.common.*;

public class Scout 
{
	//variables
	public static RobotController rc = Robot.rc;
	public static MapLocation spawnpoint;
	
	public static void run() throws GameActionException
	{
		spawnpoint = rc.getLocation();
		while(true)
		{
			Robot.randomMove();
			foundPartsSignal();
			
			Clock.yield();
		}
	}
	
	//found enemy signal
	public static void foundEnemySignal()
	{
		
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
