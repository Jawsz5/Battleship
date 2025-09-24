import java.util.Arrays;

import gamefiles.Game;

  public class Main {
  public static void main(String[] args){
    //Game g1 = new Game(10, 100);
    //g1.playGameOnTerminal_Human();
    //try{g1.playGameComputer();}catch(Exception e){System.out.println(e);System.out.println(g1.getTurnsPlayed());};
    int median;
    int minturns = 100;
    int avgturns = 0;
    int maxturns = 0;
    int hundreds = 0;
    byte[] games = new byte[100000];
    for(int i = 0; i < 100000; i++){
      try{
        int totalTurns = 0;
        Game g = new Game(10, 100);
        g.playGameComputer();
        totalTurns += g.getTurnsPlayed();
        if(totalTurns < minturns){minturns = totalTurns;}
        if(totalTurns > maxturns){maxturns = totalTurns;}
        games[i] = (byte) g.getTurnsPlayed();
        avgturns += totalTurns;
        if(totalTurns == 100){hundreds++;}
      }catch(Exception e){e.printStackTrace();}
    }
     
    Arrays.sort(games);
    if(games.length % 2 == 0){
      median = games[games.length / 2 + 1] + games[games.length / 2 - 1];
      median /= 2;
    } else median = games[games.length/2];
    System.out.println("Median: " + median);
    System.out.println("Mean: " + avgturns/100000);
    System.out.println("Minimum: " + minturns);
    System.out.println("Maximum: " + maxturns);
    System.out.println("# of 100 turn games: " + hundreds);


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
