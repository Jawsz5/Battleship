package gamefiles;

public class Ship{
   private int startPos;
   private char boatType;
   private int boatSize;
   private Boolean isVertical, isAHit;
   private int[] spots, saveSpots;
   //initialize ship position
    //overload the constructor to allow different kinds of input
    public Ship(int pos, Boolean vertical, String s){
       this.setPos(pos);
       try{setDirection(vertical);}catch(IllegalArgumentException e){
           System.out.println(e);
       }
       try{setSize(s);}catch(IllegalArgumentException e){
           System.out.println(e);
       }
       populateSpots(boatSize, pos);
   }
   public Ship(int pos, String vertical, String s){
       this.setPos(pos);
       setDirection(vertical);
       try{setSize(s);}catch(IllegalArgumentException e){
           System.out.println(e);
       }
       populateSpots(boatSize, pos);
   }


   private void populateSpots(int size, int pos){
       spots = new int[size];
       saveSpots = new int[size];
       int step = isVertical ? 10: 1;
       for(int i = 0; i < size; i++){
            int id = pos + i * step;
            spots[i] = id;
            saveSpots[i] = id;
       }
   }


   private void setPos(int pos)throws IllegalArgumentException{
       startPos = pos;
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
   public int[] getSpots(){return spots;}
   public int[] getSaveSpots(){return saveSpots;}
   public int getPos(){return startPos;}
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
    public int getBoatLength(){
        return switch (boatType){
            case 'a' -> 5;
            case 'b' -> 4;
            case 'd', 'c' -> 3;
            case 's' -> 2;
            default -> throw new IllegalStateException("Unknown boat type: " + boatType);
        };
    }

   //hit tracking
   public Boolean isHit(int pos) throws IllegalArgumentException{
       if(pos != (int)pos){
           throw new IllegalArgumentException("Input coordinates must be integers");
       }
       hit(pos);
       return isAHit;
   }
    public void hit(int pos) throws IllegalArgumentException{
        if(pos != (int)pos){
            throw new IllegalArgumentException("Input coordinates must be integers");
        }
    // the value of the position on the boat that is hit. Ie: 1st, 2nd, 3rd
        boolean shot = false;
        for(int i = 0; i < spots.length; i++){
            if (pos == spots[i]) { spots[i] = -1; shot = true; }
        }
        isAHit = shot;
    }
   public boolean isSunk(){
       for(int i: spots){
           if(i != -1){
               return false;
           }
       }
       return true;
   }
}
