package TestPlayer;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer 
{
	public static Direction[] directions = Direction.values();
	public static Random randall;

	public static void run(RobotController rc) throws GameActionException
	{
		randall = new Random(rc.getID());

		if(rc.getType() == RobotType.ARCHON)
		{
			while(true)
			{
				Direction directionToMove = directions[randall.nextInt(directions.length)];

				if(rc.canBuild(directionToMove, RobotType.SOLDIER) && rc.isCoreReady())
				{
					rc.build(directionToMove, RobotType.SOLDIER);
				}

				if(rc.canMove(directionToMove) && rc.isCoreReady())
				{
					rc.move(directionToMove);
				}

				Clock.yield();
			}
		}
		else if(rc.getType() == RobotType.SOLDIER)
		{
			while(true)
			{
				/*
				RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SOLDIER.attackRadiusSquared, Team.ZOMBIE);
				if(enemies.length > 0)
				{
					RobotInfo enemyToAttack = enemies[0];
					MapLocation loc = enemyToAttack.location;

					if(rc.canAttackLocation(loc) && rc.isCoreReady() && rc.isWeaponReady())
					{
						rc.attackLocation(loc);
					}
				}
				*/
				
				if(rc.getHealth() < RobotType.SOLDIER.maxHealth)
				{
					rc.disintegrate();
				}
				
				
				Direction directionToMove = directions[randall.nextInt(directions.length)];
				if(rc.canMove(directionToMove) && rc.isCoreReady())
				{
					rc.move(directionToMove);
				}

				Clock.yield();
			}
		}
		else if(rc.getType() == RobotType.SCOUT)
		{
			while(true)
			{
				Clock.yield();
			}
		}
		else if(rc.getType() == RobotType.TURRET)
		{
			while(true)
			{
				Clock.yield();
			}
		}
		else if(rc.getType() == RobotType.VIPER)
		{
			while(true)
			{
				Clock.yield();
			}
		}
	}
}
