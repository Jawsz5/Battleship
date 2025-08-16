import java.io.IOException;

import GameFiles.Game;


public class Main {
 public static void main(String[] args){
   //Game g1 = new Game(10, 100);
   //g1.playGameOnTerminal_Human();
   //try{g1.playGameComputer();}catch(Exception e){System.out.println(e);System.out.println(g1.getTurnsPlayed());};
   int totalTurns = 0;
   for(int i = 0; i < 1000; i++){
     try{
       Game g = new Game(10, 10);
       g.playGameComputer();
       totalTurns += g.getTurnsPlayed();
     }catch(IOException e){System.out.println(e);}
   }
   System.out.println(totalTurns / 1000);
  }
}
