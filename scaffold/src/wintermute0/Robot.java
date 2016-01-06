package wintermute0;
import battlecode.common.*;

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
}
