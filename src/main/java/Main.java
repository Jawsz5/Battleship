import GameFiles.Game;


public class Main {
  public static void main(String[] args){
  //Game g1 = new Game(10, 100);
  //g1.playGameOnTerminal_Human();
  //try{g1.playGameComputer();}catch(Exception e){System.out.println(e);System.out.println(g1.getTurnsPlayed());};
  
  int minturns = 100;
  int avgturns = 0;
  for(int i = 0; i < 100000; i++){
    try{
      int totalTurns = 0;
      Game g = new Game(10, 100);
      g.playGameComputer();
      totalTurns += g.getTurnsPlayed();
      if(totalTurns < minturns){minturns = totalTurns;}
      avgturns += totalTurns;
    }catch(Exception e){e.printStackTrace();}
  }
  System.out.println(avgturns/100000);
  System.out.println(minturns);
    
    /* 
    try{
      int totalTurns = 0;
      Game g = new Game(10, 100);
      g.playGameComputer();
      totalTurns += g.getTurnsPlayed();
      System.out.println(totalTurns);
    }catch(Exception e){e.printStackTrace();}
    */

  }
}
