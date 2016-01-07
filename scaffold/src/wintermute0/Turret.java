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
	
	public static void run()
	{
		random = new Random(rc.getID());
		
		//set mode on spawn
		setMode();
		
		while(true)
		{
			Clock.yield();
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
