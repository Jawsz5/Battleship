package Game;

public class Ship{
    private int size, startx, starty;
    private Boolean isVertical, isAHit;
    private int[] spotsX, spotsY;
    //initialize ship position
    private void setXY(int x, int y){startx = x; starty = y;}
    //2 methods for direction and size setting for simplicity later on
    private void setSize(int s){size = s;}
    private void setSize(String s){
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
    private void setDirection(String vert){
        vert = vert.toLowerCase().strip();
        String[] possible_vert_strings = new String[]{"vertical", "v", "vert"};
        for(String i: possible_vert_strings){
            if (i.equals(vert)){
                isVertical = true;
            }
        }
        isVertical = false;
    }
    private void setDirection(Boolean vert){
        if(vert == true){isVertical = true;}
        else{isVertical = false;}
    }
    //overload the createBoat to allow different kinds of input
    public void createBoat(int x, int y, Boolean vertical, int s){setXY(x,y); setDirection(vertical); setSize(s); 
        spotsX = new int[size];
        spotsY = new int[size];
        for(int i = 0; i < spotsX.length; i++){
            if(isVertical){
                spotsX[i] = x;
                spotsY[i] = y + i;
            } else{
                spotsX[i] = x + i;
                spotsY[i] = y;
            }
        }
    }
    public void createBoat(int x, int y, String vertical, int s){
        setDirection(vertical);setSize(size);createBoat(x, y, isVertical, size);
    }
    public void createBoat(int x, int y, Boolean vertical, String s){
        setDirection(vertical);setSize(size);createBoat(x, y, isVertical, size);
    }
    public void createBoat(int x, int y, String vertical, String s){
        setDirection(vertical);setSize(size);createBoat(x, y, isVertical, size);
    }

    //get methods
    public int[] getSpotsX(){return spotsX;}
    public int[] getSpotsY(){return spotsY;}
    public int getX(){return startx;}
    public int getY(){return starty;}
    //hit tracking
    public Boolean isHit(int x, int y){
        hit(x,y);
        return isAHit;
    }
    public void hit(int x, int y){
        // if the x and y coordinates fall on the boat
        Boolean hitX = false;
        Boolean hitY = false;
        // the value of the position on the boat that is hit. Ie: 1st, 2nd, 3rd
        int shotX = 0;
        int shotY = 0;
        for(int i = 0; i < spotsX.length; i++){
            if(x == spotsX[i]){hitX = true; shotX = i;}
        }
        for(int i = 0; i < spotsY.length; i++){
            if(y == spotsY[i]){hitY = true; shotY = i;}
        }
        if(hitX && hitY){
            spotsX[shotX] = -1;
            spotsY[shotY] = -1;
            isAHit = true;
        }
    }
}