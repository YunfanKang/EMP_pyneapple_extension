package edu.ucr.cs.pyneapple.regionalization;

import java.io.*;

public class SerializationTest {
    public static void main(String args[]) throws IOException {
        String[] name = {"a", "b", "c"};
        ObjectOutputStream oos = null;
        try{
            oos = new ObjectOutputStream(new FileOutputStream("./data/prevResults.txt"));
            oos.writeObject(name);
            oos.writeObject(name);
        }catch(Exception e){
            e.printStackTrace();
        }
        ObjectInputStream ois = null;
        try{
            ois = new ObjectInputStream(new FileInputStream("./data/prevResults.txt"));
            String[] newName = (String[]) ois.readObject();
            String[] newName2 = (String[]) ois.readObject();

            for(String n : newName){
                System.out.print(n + " ");
            }
        }catch(Exception e){
            e.printStackTrace();
        }


    }
    //ObjectInputStream

}
