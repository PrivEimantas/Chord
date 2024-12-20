import java.io.IOException;
import java.io.OutputStream;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IWeb {
    public void post(HTTPRequest request, byte payload[], OutputStream output) throws RemoteException, NotBoundException;
    public void listFiles(OutputStream output) throws RemoteException, NotBoundException;
    public void get(HTTPRequest request, OutputStream output) throws RemoteException, NotBoundException;
    public void page_upload(OutputStream output) throws RemoteException,NotBoundException;
    
}
