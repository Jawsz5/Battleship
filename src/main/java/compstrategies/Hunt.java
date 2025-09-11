package compstrategies;

import java.io.IOException;

//general hunt algorithm
public class Hunt {
    private boolean targeted, switchDir;
    private int origHit, move, dim; //hitPos is the initial hit sqaure
    private int shootPos = -1;
    public Hunt(int hitOne){
        origHit = hitOne;
        targeted = false;
    }
    private int shootAround(int pos, int[] hitMap) throws IOException{
        //position above the square is pos - 10
        //pos + 10 is below
        //pos to the right is +1
        //left is -1
        //returns a position around the intputted one that hasn't been shot yet
        dim = (int) Math.sqrt(hitMap.length);
        //try shooting at each position around the originial hit and catch any errors
        try{if(hitMap[pos+dim]==0) return pos+dim; } catch(Exception e){}
        try{if(hitMap[pos-dim]==0) return pos-dim;} catch(Exception e){}
        try{
            if(hitMap[pos+1]==0 && (pos+1)/dim == pos/dim) return pos+1; //ensures that the next position is in the same row
        }catch(Exception e){}
        try{if((pos - 1)/dim == pos/dim)return pos-1;}catch(Exception e){}
        
        throw new IOException("Could not find a shot");
    }
    
    public int huntShip(int[] hitMap){
        //while there isn't a hit, call shootAround
        if(!targeted){
            try{shootPos = shootAround(origHit, hitMap);}
            catch(Exception e){}
            move = shootPos - origHit;
            switchDir = false;
            return shootPos;
        }
        //after shoot around, check to see if the square in the opposite direction is viable
        int nextShot = shootPos + move;
        try{
            if(move == 1 && nextShot/dim != shootPos/dim) throw new Exception("Next horizontal shot is in the wrong row");
            if(move == -1 && nextShot/dim != shootPos/dim) throw new Exception("Next horizontal shot is in the wrong row");
            if(hitMap[nextShot] == 0) shootPos = nextShot; 
            return nextShot; 
        } catch (Exception e){switchDir = true;} //if the shot is out of bounds, reverse the direction as seen below
        move *= -1; 
        if(switchDir) {nextShot = origHit + move; switchDir = false;}
        else nextShot = shootPos + move;
        shootPos = nextShot;
        return nextShot;
    }
    public void setTargetted(boolean state){
        targeted = state;
    }
}

