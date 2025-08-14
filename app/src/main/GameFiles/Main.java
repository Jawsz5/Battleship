package app.src.main.GameFiles;
public class Main {
    public static void main(String[] args){
        
        try{
        Ocean s = new Ocean(10);
        s.placeRandomBoats();
        System.out.println(s.getBoats());
        char[][] g = s.getGrid();
        for(int i = 0; i < 10; i++){
            for(int j = 0; j < 10; j++){
                System.out.print(g[i][j]);
            }
            System.out.println();
        }
        int fireX = (int) (Math.random() * 10);
        int fireY = (int) (Math.random() * 10);
        if(s.isHit(fireX, fireY)){
            System.out.println("Hit at " + fireX + ", " + fireY);
        }else{
            System.out.println("Miss at " + fireX + ", " + fireY);
        }
        for(Ship ship : s.getBoats()){
            if(ship.isHit(fireX, fireY)){
                System.out.println("Target was " + ship.getBoatType());
            }
            if(ship.isSunk()){
                System.out.println("Ship " + ship.getBoatType() + " is sunk!");
            }else{
                System.out.println("Ship " + ship.getBoatType() + " is not sunk.");
            }
        }
        } catch(Exception e){System.out.println("skadoosh");}
       


    }
}
