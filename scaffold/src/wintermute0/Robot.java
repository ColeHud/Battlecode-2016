package wintermute0;
import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

//used for methods every bot needs
public class Robot 
{
	//variables
	public static Team myTeam;
	public static Team enemy;
	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    public static RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
	public static RobotController rc;
	public static RobotType type;
	public static Random random;
	
	/*
	 * METHODS
	 */
	
	//attack --20bc
	public static void attack(MapLocation loc) throws GameActionException
	{
		if(rc.canAttackLocation(loc) && rc.isCoreReady() && rc.isWeaponReady())
		{
			rc.attackLocation(loc);
		}
	}
	
	//random move
	public static void randomMove() throws GameActionException
	{
		int tries = 0;
		while(tries < 8)
		{
			Direction dir = directions[random.nextInt(directions.length)];
			if(rc.isCoreReady() && rc.canMove(dir))
			{
				rc.move(dir);
				break;
			}
			else
			{
				tries++;
			}
		}
	}
	
	//check surroundings, attack foes
	public static void attackFoes() throws GameActionException
	{
		RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), Robot.type.attackRadiusSquared);
		
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
}
