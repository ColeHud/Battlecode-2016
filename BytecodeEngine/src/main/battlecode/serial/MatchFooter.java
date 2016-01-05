package battlecode.serial;

import battlecode.common.Team;

public class MatchFooter implements ServerEvent {

    private static final long serialVersionUID = 1179780102137505202L;
    private final Team winner;
    private final long[][] state;

    public MatchFooter(Team winner, long[][] state) {
        this.winner = winner;
        this.state = state;
    }

    public Team getWinner() {
        return winner;
    }

    public long[][] getState() {
        return state;
    }

    /**
     * For use by serializers.
     */
    @SuppressWarnings("unused")
    private MatchFooter() {
        this(null, null);
    }
}
