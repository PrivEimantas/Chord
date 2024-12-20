import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Set;

public interface IChordNode extends Remote {
    void put(String key, byte[] value) throws RemoteException,IOException;
    byte[] get(String key) throws RemoteException;
    int getKey() throws RemoteException;
    IChordNode getPredecessor() throws RemoteException;
    void join(IChordNode atNode) throws RemoteException;
    IChordNode findSuccessor(int key) throws RemoteException;
    IChordNode closestPrecedingNode(int key) throws RemoteException;
    int hash(String s) throws RemoteException;
    public void notify(IChordNode potentialPredecessor) throws RemoteException;
    void stabilise() throws RemoteException;
    void fixFingers() throws RemoteException;
    void checkPredecessor() throws RemoteException;
    public void checkDataMoveDown() throws RemoteException;
    public boolean isAlive() throws RemoteException;
    public int getSuccessorKey() throws RemoteException;
    public Integer getPredecessorKey() throws RemoteException;
    public boolean isBusy() throws RemoteException;
    public float averageWordLength(byte[] value) throws RemoteException;
    public String mostFrequentlyOccuringWord(byte[] value) throws RemoteException;
    public byte[] doTask(IChordNode node,byte[] value,String key) throws RemoteException;
    public int countTotalWords(byte[] value) throws RemoteException;
    public void moveTheData(Integer key,Store store) throws RemoteException;

    public Integer countCSVRows(byte[] value) throws IOException, RemoteException;
    public Integer countCSVColumns(byte[] value) throws IOException, RemoteException;
    public Integer RealWordsCSV(byte[] value) throws IOException, RemoteException;

    public void saveObjectFile(HashMap<Integer,Store> dataStoreToSave) throws RemoteException;
    


}
