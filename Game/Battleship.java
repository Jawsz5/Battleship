package Game;

public class Battleship{
    protected int size, startx, starty;
    private Boolean isVertical, hit;
    protected int[] spots;
    //initialize ship position
    public void setX(int x){startx = x;}
    public void setY(int y){starty = y;}
    public void setXY(int x, int y){startx = x; starty = y;}
    //2 methods for direction and size setting for simplicity later on
    public void setSize(int s){size = s;}
    public void setSize(String s){
        s = s.toLowerCase().strip();
        String[][] sizes = new String[][]{
            {"aircraft", "aircraftcarrier", "a", "air"}, //due to strip function, aircraft carrier is one word
            {"battleship", "battle", "b"}, //battleships
            {"destroyer", "d"}, //destroyer
            {"cruiser", "c"}, //cruiser
            {"submarine", "sub", "s"}, //submarines
        };
        for(int i = 0; i < sizes.length; i++){
            for(String m: sizes[i]){
                if(m.equals(s)){size = i;}
            }
        }
        //map the position in the sizes array to actual ship size
        int[] size_map = new int[]{5, 4, 3, 3, 2};
        size = size_map[size];
    }
    public void setDirection(String vert){
        vert = vert.toLowerCase().strip();
        String[] possible_vert_strings = new String[]{"vertical", "v", "vert"};
        for(String i: possible_vert_strings){
            if (i.equals(vert)){
                isVertical = true;
            }
        }
        isVertical = false;
    }
    public void setDirection(Boolean vert){
        if(vert == true){isVertical = true;}
        else{isVertical = false;}
    }
    public void createBoat(int x, int y, Boolean vertical, int size){}
    public void createBoat(int x, int y, String vertical, int size){}
    public void createBoat(int x, int y, Boolean Vertical, String size){}
    public void createBoat(int x, int y, String vertical, String size){}


    //hit tracking

}