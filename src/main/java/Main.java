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
    int failed = 0;
    byte[] games = new byte[1000];
    int totalTurns = 0;
    for(int i = 0; i < 1000; i++){
      try{
        Game g = new Game(10, 100);
        g.playGameComputer();
        totalTurns += g.getTurnsPlayed();
        if(g.getTurnsPlayed() < minturns){minturns = g.getTurnsPlayed();}
        if(g.getTurnsPlayed() > maxturns){maxturns = g.getTurnsPlayed();}
        games[i] = (byte) g.getTurnsPlayed();
        if(g.getTurnsPlayed() == 100){hundreds++;}
      }catch(Exception e){e.printStackTrace(); failed ++;}
    }
     
    Arrays.sort(games);
    if(games.length % 2 == 0){
      median = games[games.length / 2 + 1] + games[games.length / 2 - 1];
      median /= 2;
    } else median = games[games.length/2];
    System.out.println("Median: " + median);
    System.out.println("Mean: " + totalTurns/1000);
    System.out.println("Minimum: " + minturns);
    System.out.println("Maximum: " + maxturns);
    System.out.println("# of 100 turn games: " + hundreds);
    System.out.println("# failed games: " + failed);
  }
}
