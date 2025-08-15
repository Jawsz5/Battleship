package app.src.main.GameFiles;

public class Ship{
    private int startx, starty;
    private char boatType;
    private int boatSize;
    private Boolean isVertical, isAHit;
    private int[] spotsX, spotsY;
    //initialize ship position
     //overload the constructor to allow different kinds of input
     public Ship(int x, int y, Boolean vertical, String s){
        this.setXY(x,y);
        try{setDirection(vertical);}catch(IllegalArgumentException e){
            System.out.println(e);
        }
        try{setSize(s);}catch(IllegalArgumentException e){
            System.out.println(e);
        }
        populateSpots(boatSize, x, y);
    }
    public Ship(int x, int y, String vertical, String s){
        this.setXY(x,y);
        setDirection(vertical);
        try{setSize(s);}catch(IllegalArgumentException e){
            System.out.println(e);
        }
        populateSpots(boatSize, x, y);
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
        startx = x; starty = y;
    }
    private void setSize(String s) throws IllegalArgumentException{
        if (s == null){
            throw new IllegalArgumentException("size of the boat can't be null");
        }
        s = s.toLowerCase().strip();
        String[][] sizes = new String[][]{
            {"aircraft", "aircraftcarrier", "a", "air"}, //due to strip function, aircraft carrier is one word
            {"battleship", "battle", "b"}, //battleships
            {"destroyer", "d"}, //destroyer
            {"cruiser", "c"}, //cruiser
            {"submarine", "sub", "s"}, //submarines
        };
        int selectBoat = 10;
        for(int i = 0; i < sizes.length; i++){
            for(String m: sizes[i]){
                if(m.equals(s)){selectBoat = i;} 
            }
        }
        if(selectBoat == 10){throw new IllegalArgumentException("size input is not contained in the string list not is it a valid String");}
        //map the position in the sizes array to actual ship size
        char[] type_map = new char[]{'a', 'b', 'd', 'c', 's'};
        int[] size_map = new int[]{5, 4, 3, 3, 2};
        boatType = type_map[selectBoat];
        boatSize = size_map[selectBoat];
    }
    //2 methods for direction setting for simplicity later on
    private void setDirection(String vert) throws IllegalArgumentException{
        if (vert == null){
            throw new IllegalArgumentException("direction of the boat can't be null");
        }
        vert = vert.toLowerCase().strip();
        String[] possible_vert_strings = new String[]{"vertical", "v", "vert"};
        isVertical = false;
        for(String i: possible_vert_strings){
            if (i.equals(vert)){
                isVertical = true;
            }
        }
    }
    private void setDirection(Boolean vert) throws IllegalArgumentException{
        if (!(vert instanceof Boolean)){
            throw new IllegalArgumentException("direction of the boat must be either a Boolean or string");
        }
        if(vert == true){isVertical = true;}
        else{isVertical = false;}
    }

    //get methods
    public int[] getSpotsX(){return spotsX;}
    public int[] getSpotsY(){return spotsY;}
    public int getX(){return startx;}
    public int getY(){return starty;}
    public char getBoatType(){return boatType;}
    public String getBoatName(){
        String[] boatTypeToName = new String[]{"aircraft carrier", "battleship", "destroyer", "cruiser", "submarine"};
        for(String i: boatTypeToName){
            if(getBoatType() == i.charAt(0)){
                return i;
            }
        }
        return "no boat name found"; //base case that is never reached 
    }
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
        else{isAHit = false;}
    }
    public boolean isSunk(){
        for(int i: spotsX){
            if(i != -1){
                return false;
            }
        }
        for(int i: spotsY){ //redundant but safe approach
            if(i != -1){
                return false;
            }
        }
        return true;
    }
}