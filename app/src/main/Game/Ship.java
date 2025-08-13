package app.src.main.Game;

import java.io.IOException;

public class Ship{
    private int size, startx, starty;
    private Boolean isVertical, isAHit;
    private int[] spotsX, spotsY;
    //initialize ship position
     //overload the constructor to allow different kinds of input
     public Ship(int x, int y, Boolean vertical, int s){
        try{this.setXY(x,y);}catch(IllegalArgumentException e){System.out.println("coordinates of the boat must be integers");}
        try{setDirection(vertical);}catch(IllegalArgumentException e){
            System.out.println("direction of the boat must be either a boolean or string");
        }
        try{setSize(s);}catch(IllegalArgumentException e){
            System.out.println("size of the boat must be an integer or valid string");
        }
        populateSpots(size, x, y);
    }
    public Ship(int x, int y, String vertical, int s){
        try{setDirection(vertical);}catch(IllegalArgumentException e){System.out.println("direction of the boat must be either a boolean or string");}
        try{setSize(s);}catch(IllegalArgumentException e){System.out.println("size of the boat must be an integer or valid string");}
        populateSpots(size, x, y);
    }
    public Ship(int x, int y, Boolean vertical, String s){
        setDirection(vertical); 
        try{setSize(s);}catch(IllegalArgumentException | IOException e){
            System.out.println("size of the boat must be an integer or valid string. If the input isn't a valid string, check the IO exception");
        }
        new Ship(x, y, isVertical, size);
    }
    public Ship(int x, int y, String vertical, String s){
        setDirection(vertical);
        try{setSize(s);}catch(IllegalArgumentException | IOException e){
            System.out.println("size of the boat must be an integer or valid string. If the input isn't a valid string, check the IO exception");
        }
        new Ship(x, y, isVertical, size);
    }

    private void populateSpots(int size, int x, int y){
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

    private void setXY(int x, int y)throws IllegalArgumentException{
        if(x != (int)x){
            throw new IllegalArgumentException("coordinates of the boat must be integers");
        }
        startx = x; starty = y;
    }
    //2 methods for direction and size setting for simplicity later on
    private void setSize(int s) throws IllegalArgumentException{
        if (s != (int)s){
            throw new IllegalArgumentException("size of the boat must be an integer or valid string");
        }
        size = s;
    }
    private void setSize(String s) throws IllegalArgumentException, IOException{
        if (!(s instanceof String)){
            throw new IllegalArgumentException("size of the boat must be either an integer or valid string");
        }
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
        if(size == 0){throw new IOException("size input is not contained in the string list not is it a valid integer");}
        //map the position in the sizes array to actual ship size
        int[] size_map = new int[]{5, 4, 3, 3, 2};
        size = size_map[size];
    }
    private void setDirection(String vert) throws IllegalArgumentException{
        if (!(vert instanceof String)){
            throw new IllegalArgumentException("direction of the boat must be either a boolean or string");
        }
        vert = vert.toLowerCase().strip();
        String[] possible_vert_strings = new String[]{"vertical", "v", "vert"};
        for(String i: possible_vert_strings){
            if (i.equals(vert)){
                isVertical = true;
            }
        }
        isVertical = false;
    }
    private void setDirection(Boolean vert) throws IllegalArgumentException{
        if (!(vert instanceof Boolean)){
            throw new IllegalArgumentException("direction of the boat must be either a boolean or string");
        }
        if(vert == true){isVertical = true;}
        else{isVertical = false;}
    }

    //get methods
    public int[] getSpotsX(){return spotsX;}
    public int[] getSpotsY(){return spotsY;}
    public int getX(){return startx;}
    public int getY(){return starty;}
    //hit tracking
    public Boolean isHit(int x, int y) throws IllegalArgumentException{
        if(x != (int)x || y != (int)y){
            throw new IllegalArgumentException("Input coordinates must be integers");
        }
        hit(x,y);
        return isAHit;
    }
    public void hit(int x, int y) throws IllegalArgumentException{
        if(x != (int)x || y != (int)y){
            throw new IllegalArgumentException("Input coordinates must be integers");
        }
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