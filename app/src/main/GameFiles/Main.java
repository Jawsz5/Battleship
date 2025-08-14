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
        } catch(Exception e){System.out.println("skadoosh");}

    }
}
