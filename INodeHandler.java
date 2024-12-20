
import java.io.File;
import java.rmi.AccessException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;



public interface  INodeHandler extends Remote{
    
    public ArrayList<IChordNode> getChordNodesLog() throws RemoteException;
    public void addChordNode(Registry registry, IChordNode ChordNode, String ChordNodeName) throws RemoteException, AccessException;
    public void AssignChordNodeATask(byte[] fileInBytes,String filePath) throws RemoteException;
    public byte[] fetchTasks(String fileName) throws RemoteException;
    
    public boolean isAlive() throws RemoteException;
    public ArrayList<Store> RetrieveTasks() throws RemoteException;
    
}
