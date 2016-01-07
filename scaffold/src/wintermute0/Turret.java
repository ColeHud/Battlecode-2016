package wintermute0;
import battlecode.common.*;

import java.util.Random;

public class Turret
{
	//variables
	public static RobotController rc = Robot.rc;
	public static Random random;
	
	//different modes
	public static boolean protectArchon;
	public static boolean attackEnemy;
	public static boolean attackZombies;
	
	//current target
	public static MapLocation currentTarget;
	
	public static void run() throws GameActionException
	{
		random = new Random(rc.getID());
		
		//set mode on spawn
		setMode();
		
		while(true)
		{
			receiveSignals();
			
			attackFoes();
			
			Clock.yield();
		}
	}
	
	//check surroundings, attack foes
	public static void attackFoes() throws GameActionException
	{
		RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.TURRET.attackRadiusSquared);
		
		if(foes.length > 0)
		{
			int randIndex = random.nextInt(foes.length);
			RobotInfo foe = foes[randIndex];
			MapLocation loc = foe.location;
			
			if(rc.canAttackLocation(loc) && rc.isCoreReady() && rc.isWeaponReady())
			{
				rc.attackLocation(loc);
			}
		}
	}
	
	//receive signals
	public static void receiveSignals()
	{
		Signal signal = rc.readSignal();
		if(signal != null)
		{
			int[] message = signal.getMessage();
			if(currentTarget == null && message != null)
			{
				if(message[0] == 666 && attackZombies)//zombie den
				{
					currentTarget = signal.getLocation();
				}
				else if(message[0] == 42 && attackEnemy)//enemy archon
				{
					currentTarget = signal.getLocation();
				}
			}
		}
	}
	
	//set mode
	public static void setMode()
	{
		//~30% chance of each
		double mode = random.nextDouble();
		rc.setIndicatorString(1, "" + mode);
		if(mode < .66)
		{
			rc.setIndicatorString(0, "Protect");
			protectArchon = true;
			attackEnemy = false;
			attackZombies = false;
		}
		else if(mode >= .66 && mode < .75)
		{
			rc.setIndicatorString(0, "Attack Enemy");
			attackEnemy = true;
			attackZombies = false;
			protectArchon = false;
		}
		else
		{
			rc.setIndicatorString(0, "Attack Zombies");
			attackZombies = true;
			attackEnemy = false;
			protectArchon = false;
		}
	}
}
