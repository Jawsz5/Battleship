package app.src.main.GameFiles;
import java.util.Scanner;
public class Main {
    public static void main(String[] args){
        /* 
        Scanner input = new Scanner(System.in);
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
        while(!s.isAllSunk()){
            System.out.println("Not all ships are sunk yet.");
            System.out.println("Enter x coordinate to fire at: ");
            int fireX = input.nextInt();
            System.out.println("Enter y coordinate to fire at: ");
            int fireY = input.nextInt();
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
            for(int i = 0; i < 10; i++){
                for(int j = 0; j < 10; j++){
                    System.out.print(g[i][j]);
                }
                System.out.println();
            }
            } 
            
            }catch(Exception e){System.out.println("skadoosh");}
            System.out.println("All ships sunk! You win!");
            input.close();
        
       */
      Game g1 = new Game(10, 100);
      g1.playGameOnTerminal_Human();


     }
    }
