package wintermute0;
import battlecode.common.*;
import java.util.Random;

/*
 * wintermute0
 * By Cole Hudson and Joseph Gnehm
 * 
 * Goal: Implement basic strategies
 */

/*
 * broadcast message codes
 * 
 * 94572 -> parts at location
 */

public class RobotPlayer 
{
	public static void run(RobotController rc) throws Exception
	{
		//set up variables
		Robot.myTeam = rc.getTeam();
        Robot.enemy = Robot.myTeam.opponent();
        Robot.rc = rc;
        Robot.type = rc.getType();
        Robot.random = new Random(rc.getID());
        
        //run methods
        if(Robot.type == RobotType.ARCHON)
        {
        	Archon.run();
        }
        else if(Robot.type == RobotType.GUARD)
        {
        	Guard.run();
        }
        else if(Robot.type == RobotType.SCOUT)
        {
        	Scout.run();
        }
        else if(Robot.type == RobotType.SOLDIER)
        {
        	Soldier.run();
        }
        else if(Robot.type == RobotType.TTM)
        {
        	TTM.run();
        }
        else if(Robot.type == RobotType.TURRET)
        {
        	Turret.run();
        }
        else if(Robot.type == RobotType.VIPER)
        {
        	Viper.run();
        }
        else
        {
        	throw new Exception("That's not a robot type I'm familiar with.");
        }
	}
}
