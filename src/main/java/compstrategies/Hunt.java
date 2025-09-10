package compstrategies;

import java.io.IOException;

//general hunt algorithm
public class Hunt {
    private boolean hunt, targeted;
    private int[] hitMap;
    private int previousShot, hitPos; //hitPos is the initial hit sqaure
    private boolean dir = true; //direction can be + or -
    public Hunt(int[] shotsFired){
        hitMap = shotsFired;
        targeted = false;
    }
    private int shootAround(int pos) throws IOException{
        //position above the square is pos - 10
        //pos + 10 is below
        //pos to the right is +1
        //left is -1
        //returns a position around the intputted one that hasn't been shot yet
        int spotPicker = 0;
        while(true){ //if an invalid position is picked, it will move on to the next
            spotPicker ++;
            try{
                if(hitMap[pos+10]==0) return hitMap[pos+10]; 
                if(hitMap[pos-10]==0) return hitMap[pos-10];
                if(hitMap[pos+1]==0) return hitMap[pos+1];
                return hitMap[pos-1];
            }catch( ArrayIndexOutOfBoundsException e){};
            if(spotPicker > 4){throw new IOException("No shots found around target");}
        }
    }
    private boolean vertical(int pos){
        //vertical method is based on the first shootAround call, finding the direction that call was in
        if(pos+10 == previousShot || pos-10==previousShot){
            return true;
        }
        return false;
    }
    public int huntShip(){
        int shootPos = -1;
        boolean sunk = false;
        //while there isn't a hit, call shootAround
        if(!targeted){
            try{shootPos = shootAround(hitPos);}
            catch(IOException e){}
            previousShot = shootPos;
        }
        //after shoot around, check to see if the square in the opposite direction is viable
        if(vertical(hitPos)){
            
        }
        

        
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
