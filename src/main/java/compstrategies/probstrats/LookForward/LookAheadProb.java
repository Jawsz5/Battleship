package compstrategies.probstrats.LookForward;

public class LookAheadProb extends HitMissProb{
    LookAheadProb(int dimension){
        super(dimension);
    }
    @Override
    public int selectShot(){
        return -1;
    }
}
