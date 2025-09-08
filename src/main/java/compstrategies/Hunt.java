package compstrategies;

//general hunt algorithm
public class Hunt {
    private boolean hunt, targeted;
    private int[] hitMap;
    private int previousShot, hitPos; //hitPos is the initial hit sqaure
    private int dir = 0; //direction can be -2,-1,1,2 for right, down, up, and left
    public Hunt(int[] shotsFired){
        hitMap = shotsFired;
        targeted = false;
    }
    private int shootAround(int pos){
        //position above the square is pos - 10
        //pos + 10 is below
        //pos to the right is +1
        //left is -1
        //returns a position around the intputted one that hasn't been shot yet
        try{
            if(hitMap[pos+10]==0) return hitMap[pos+10]; 
            if(hitMap[pos-10]==0) return hitMap[pos-10];
            if(hitMap[pos+1]==0) return hitMap[pos+1];
            return hitMap[pos-1];
        }catch( ArrayIndexOutOfBoundsException e){};
        return -1;
    }
    private boolean vertical(int pos){
        //vertical method is based on the first shootAround call, finding the direction that call was in
        if(hitMap[pos+10]==1 || hitMap[pos-10]==1){
            return true;
        }
        return false;
    }
    public int huntShip(){
        int shootPos = -1;
        boolean sunk = false;
        //while there isn't a hit, call shootAround
        if(!targeted && previousShot == 0){
            shootPos = shootAround(hitPos);
            previousShot = shootPos;
        }
        //after shoot around, check to see if the square in the opposite direction is viable
        // 
        

        
        //continue in the direction of the boat until sunk


        hunt = sunk;
        return shootPos;
    }
    private int movePos(int pos){
        //will hunt the ship in a certain direction
        //to improve add a condition that checks if the current position or the position it is moving to is an 'O'
        //if it is, go back to hitPos and move the other way.
        if(hitMap[previousShot] == 1)
        if(vertical(pos)){
            if(hitMap[pos+10] == 1){
                return hitMap[pos-10];
            }
            return hitMap[pos+10];
        }
        if(hitMap[pos+1] == 1){
            return hitMap[pos-1];
        }
        return hitMap[pos+1];
    }
    public boolean getHunt(){
        return hunt;
    }
    public void setHunt(boolean state){
        hunt = state;
    }
    public void setPrevShot(int pos){
        previousShot = pos;
    }
    public void markShot(int pos, int state){
        hitMap[pos] = state;
    }
}

//2's represent sunk ships. Add that feature for all of the other strategies
