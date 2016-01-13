package wintermute2Defense;
import battlecode.common.*;

public class RobotPlayer 
{
	public static void run(RobotController rc) throws GameActionException
	{
		RobotType type = rc.getType();
		
		if(type == RobotType.ARCHON)
		{
			while(true)
			{
				//build the units
				if(rc.isCoreReady() && rc.hasBuildRequirements(RobotType.TURRET))//surround yourself with turrets
				{
					for(Direction d : Direction.values())
					{
						if(rc.canBuild(d, RobotType.TURRET))
						{
							rc.build(d, RobotType.TURRET);
							break;
						}
					}
				}
				
				Clock.yield();
			}
		}
		else if(type == RobotType.TURRET)
		{
			//attack enemies in this order:
			//1. Enemy Archons
			//2. Big Zombies
			//3. Enemy Bots
			//4. Zombies
			
			while(true)
			{
				if(rc.isCoreReady() && rc.isWeaponReady())
				{
					RobotInfo[] foes = rc.senseHostileRobots(rc.getLocation(), RobotType.TURRET.attackRadiusSquared);
					
					RobotInfo botToAttack = null;
					double lowestHealth = 1000000;
					for(RobotInfo foe : foes)
					{
						if(foe.type == RobotType.ARCHON)
						{
							botToAttack = foe;
							break;
						}
						else if(foe.type == RobotType.BIGZOMBIE)
						{
							botToAttack = foe;
							break;
						}
						else
						{
							if(foe.health < lowestHealth)
							{
								botToAttack = foe;
								lowestHealth = foe.health;
							}
						}
					}
					
					if(botToAttack != null && rc.canAttackLocation(botToAttack.location))
					{
						rc.attackLocation(botToAttack.location);
					}
				}
				
				Clock.yield();
			}
		}
	}
}
