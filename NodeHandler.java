import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AccessException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class NodeHandler implements INodeHandler,Runnable {
    
    INodeHandler mainNodeHandler = null; //maybe get rid of this in the end
    static final int KEY_BITS = 8;

    ArrayList<IChordNode> chordNodesLog = new ArrayList<IChordNode>();
    Map<IChordNode,String> instanceAndName = new HashMap<>();

    NodeHandler() throws RemoteException
    {
        super();
        new Thread(this).start();
        
    }

    public int hash(String s)
        {
        int hash = 0;
        
        for (int i = 0; i < s.length(); i++)
            hash = hash * 31 + (int) s.charAt(i);
        
        if (hash < 0) hash = hash * -1;
        
        return hash % ((int) Math.pow(2, KEY_BITS));
    }

   
    
    /*
     * Called by accessing'/getTask', it will retrieve all tasks that are on the network
     */
    public ArrayList<Store> RetrieveTasks() throws RemoteException
    {
        
        ArrayList<Store> StoreArray= new ArrayList<Store>();
        
        
        for(int i=0;i<chordNodesLog.size();i++)
        {
            //System.out.println("reaching inside the for loop in node handler");
            IChordNode chordNode = chordNodesLog.get(i);
            
            HashMap<Integer,Store> retrievedDataStore = chordNode.getDataStore();
            System.out.println("got data store of our node"+Integer.toString(chordNode.getKey()));
            
            if(retrievedDataStore != null)
            {
                
                    System.out.println("data store is not null..");
                    for(Map.Entry<Integer,Store> entry : retrievedDataStore.entrySet())
                    {
                        Store retrievedStore = entry.getValue(); //fetches the store values
                        System.out.println("Found key value of "+ Integer.toString(entry.getKey()));
                        StoreArray.add(retrievedStore); //add to list of total items retrieved from here
                    }
                    //filesAsBytesArray.add(output);
                    
                
                
            }

            
            
            
        }
        return StoreArray; //once attatched all tasks as an array, just send to Web

    }

    /*
     * Called by '/getTask' when trying to access a specific file and download its contents as an XML
     */
    public byte[] fetchTasks(String fileName) throws RemoteException
    {
        System.out.println("attempting to fetch tasks");
        System.out.println("Key is"+fileName);
        //ArrayList<byte[]> filesAsBytesArray = new ArrayList<byte[]>();
        
        for(int i=0;i<chordNodesLog.size();i++)
        {
            //System.out.println("reaching inside the for loop in node handler");
            IChordNode chordNode = chordNodesLog.get(i);
            //byte[] output = chordNode.get(instanceAndName.get(chordNode));
            byte[] output = chordNode.get(fileName);
            
            if(output!=null)
            {
                return output;
                //filesAsBytesArray.add(output);
            }
            
        }
        return null;
        
        
    }

    
    /*
     * Adds a chord to a list of known chord nodes
     */
    public void addChordNode(Registry registry, IChordNode ChordNode, String ChordNodeName) throws RemoteException, AccessException
    {
        System.out.println("Inside - NodeHandler | Adding a new node");

        String[] ListOfChordNodes = registry.list();
        for(String currentChordNode : ListOfChordNodes) 
        {
            System.out.println(currentChordNode);
            //if(ChordNodeName.hash) //check for collisions as hash functions may have them
            
            
        }
        chordNodesLog.add(ChordNode);
        
        instanceAndName.put(ChordNode,"N-"+ChordNodeName);
    }

    /*
     * Is this server alive?
     */
    public boolean isAlive() throws RemoteException
    {
        return true;
    }

    /*
     * Randomly assigns a node to do a task
     */
    public void AssignChordNodeATask(byte[] fileInBytes,String filePath) 
    {
        //Random approach, pick a node at random

        
        Random random = new Random();
        System.out.println("File being assigned is:"+filePath);
        
        if(!chordNodesLog.isEmpty())
        {
            int randomNodeLocation = random.nextInt(chordNodesLog.size());
            IChordNode node = chordNodesLog.get(randomNodeLocation);
            try {
                //node.put(instanceAndName.get(node), fileInBytes);
                node.put(filePath,fileInBytes);
            } catch (Exception e) {
                e.printStackTrace();
                // TODO: handle exception
            }
            
        }

       


    } 
        

    public ArrayList<IChordNode> getChordNodesLog() throws RemoteException
    {
        return chordNodesLog;
    }

    /*
     * Called by the Thread periodically, checks if any nodes are dead and so removes from list of known nodes
     */
    public void checkForAliveNodes() throws RemoteException
    {

       // System.out.println(chordNodesLog.size());
        for(int i=0;i<chordNodesLog.size();i++)
        {
            //System.out.println("running loop inside checkforalivenodes");
            try {
                IChordNode node = chordNodesLog.get(i);
                if(node.isAlive()){

                    System.out.println("The node: "+Integer.toString(node.getKey())+" is alive" );
                    System.out.println("Successor is" + node.getSuccessorKey());

                    System.out.println(LocalTime.now());
                    System.out.println("Predecessor is" + node.getPredecessorKey()); //from terminal, looks out of sync,
                    
                    System.out.println("-------");

                }
               // else{
               //     System.out.println("Node dead");
               //     chordNodesLog.remove(i);
               // }
            } catch (Exception e) {
                System.out.println("Node dead");
                chordNodesLog.remove(i);

               
                break;
                

                
            }
        }
    }
    

    public void run()
    {
        
        while (true) 
        { 
            
            try
            {
                Thread.sleep(4000);
            }    
            
            catch(InterruptedException e)
            {
                System.out.println("Interrupt detected - !!");
            }


            try{
                //System.out.println("running from thread inside node handler");
                checkForAliveNodes();
            }
            catch(Exception e)
            {
                System.err.println("EXCEPTION when trying to call checkForAliveNodes  -- !! ");
                e.printStackTrace();
            }
        }
    }




    public static void main(String[] args) throws RemoteException{
        

        try{
            NodeHandler server = new NodeHandler();
            System.out.println("Booting up server..");
            String name = "NodeHandler";
            Registry registry = LocateRegistry.getRegistry("localhost");
            INodeHandler stub = (INodeHandler) UnicastRemoteObject.exportObject(server, 0) ;
            registry.rebind(name, stub);

        }

        catch(Exception e)
        {
            System.err.println("EXCEPTION");
            e.printStackTrace();
        }




    }


}
