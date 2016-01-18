package wintermute6betterTurrets;
import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class Guard 
{
	public static Random rand;
	public static RobotController rc;

	//continues might be a bad idea
	public static void run() throws GameActionException
	{
		rc = RobotPlayer.rc;
		Team myTeam = rc.getTeam();
		rand = new Random(rc.getID()); //ever used?

		while(true)
		{
			//have a preference for attacking zombies
			
			//move around with the soldiers
			
			//make sure you're closer than soldiers to attack
			
			//guards work good against turrets, so have a second preference for turrets
			
			Clock.yield(); //end after if statement
		}
	}
}
