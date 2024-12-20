import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;


class HTTPRequest {
	RequestType type;
	String resource;
	HTTPHeader headers[];
	
	String getHeaderValue(String key)
		{
		for (int i = 0; i < headers.length; i++)
			{
			if (headers[i].key.equals(key))
				return headers[i].value;
			}
		
		return null;
		}
	}
/*
 * For breaking down long URLS into smaller chunks 
 */
class HTTPSplitter {
	String[] sections;
	int index = 0;
	HTTPSplitter(HTTPRequest request){
		
		this.sections = request.resource.split("/");
	}

	boolean reset() 
	{
		this.index = 0;
		return true;
	}

	String current(){
		return this.sections[this.index];
	}

	
	String next()
	{
		
		this.index++;
		System.out.println("Index "+ this.index+ " | Len : " + this.sections.length );
		if (this.index >= this.sections.length)
		{
			System.out.println("resetting the index");
			reset();
			return "null";
		}
		System.out.println("HERE");
		return this.sections[this.index];
	}


}

public class Web implements IWeb {
	
	static int RESPONSE_OK = 200;
	static int RESPONSE_NOT_FOUND = 404;
	static int RESPONSE_SERVER_ERROR = 501;
	
	
	FormMultipart formParser = new FormMultipart();
	
	private void sendResponse(OutputStream output, int responseCode, String contentType, byte content[])
		{
		try {
			output.write(new String("HTTP/1.1 " + responseCode + "\r\n").getBytes());
			output.write("Server: Kitten Server\r\n".getBytes());
			if (content != null) output.write(new String("Content-length: " + content.length + "\r\n").getBytes());
			if (contentType != null) output.write(new String("Content-type: " + contentType + "\r\n").getBytes());
			output.write(new String("Connection: close\r\n").getBytes());
			output.write(new String("\r\n").getBytes());
			
			if (content != null) output.write(content);
			}
			catch (IOException e)
			{
			e.printStackTrace();
			}
		}
	
	/*
	 * built in regular html and css then just merged the two files
	 */
	void page_index(OutputStream output)
	{
	

		String response = ""
		+"<head>"+ "  <meta charset=\"UTF-8\">"
		+"<style>"
		+"body{"
		+"font-family: Arial, sans-serif;"
		+"background-color: #f4f4f9;"
		+"margin: 0;"
		+"padding: 0;"
		+"display: flex;"
		+"justify-content: center;"
		+"align-items: center;"
		+"height: 100vh;"
		+"text-align: center;"
		+"}"
		+".container{"
		+"background-color: #fff;"
		+"padding: 30px;"
		+"border-radius: 8px;"
		+"width: 80%;"
		+"max-width: 400px;"
		+"}"
		+"h1 {"
		+"color: #333;"
		+"margin-bottom: 20px;"
		+"font-size: 20px;"
		+"}"
		+"p{"
		+"color: #555;"
		+"font-size: 20px;"
		+"margin-bottom: 30px;"
		+"}"
		+".link-button {"
		+"display: inline-block;"
		+"background-color: #007BFF;"
		+"color: white;"
		+"text-decoration: none;"
		+"padding: 10px 30px;"
		+"border-radius: 10px;"
		+"font-size: 20px;"
		+"margin: 10px;"
		+"transition: background-color 0.3s;"
		+"}"
		+"</style>"
		+"</head>"
		+"<body>"
		+"<div class=\"container\">"
		+"<a href=\"/upload" +"\" class=\"link-button\">Upload</a>" //use the href to link the user to the 
		+"<a href=\"/getTask"  +"\" class=\"link-button\">Results</a>"
		+"</div>"
		+"</body>"
		+"</html>";
		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());
	}
	
	//example of a form to fill in, which triggers a POST request when the user clicks submit on the form
	public void page_upload(OutputStream output) throws RemoteException,NotBoundException
	{

		Registry registry = LocateRegistry.getRegistry("localhost"); 
		INodeHandler nodeHandler = (INodeHandler)registry.lookup("NodeHandler");
		String filePath = "../testData";
		File folder = new File(filePath);

		String response = "";
		response = response + "<html>";
		response = response + "<body>";
		response = response + "<form action=\"/upload_do\" method=\"POST\" enctype=\"multipart/form-data\">";
		//response = response + "<input type=\"text\" name=\"name\" placeholder=\"File name\" required/>";
		response = response + "<input type=\"file\" name=\"content\" required/>";
		response = response + "<input type=\"submit\" name=\"submit\"/>";
		response = response + "</form>";

		response = response + "</body></html>";
		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());



		//response = response + "</body>";
		//response = response + "</html>";
		
		//sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());
	}
	
	//this function maps GET requests onto functions / code which return HTML pages
	public void get(HTTPRequest request, OutputStream output) throws RemoteException, NotBoundException
	{
			//System.out.println("start of get");
			
			HTTPSplitter splitHTTP = new HTTPSplitter(request);

			//System.out.println("splitHTTP_ARRAY->"+Arrays.toString(splitHTTP.sections));			

		//System.out.println("1");
		if (request.resource.equals("/")){
			
			page_index(output);

		}

		//System.out.println("2");

		else if (request.resource.equals("/upload")){
			//System.out.println("2");
			page_upload(output);
		}
		else if (request.resource.equals("/taskProgress")){
			//System.out.println("2");
			DisplayDoneTasks(output);
		}
		
	

		else if(splitHTTP.reset())
		{
			if(splitHTTP.next().equals("getTask"))
			{
				getTask(output);
			}
		}

	
		
		else{
			//System.out.println("invalid request found");
			sendResponse(output, RESPONSE_NOT_FOUND, null, null);
		}
			
		
	}


	/*
	 * Called when a user goes to '/getTask' URL, will request to retrieve all known done tasks
	 */
	public void getTask(OutputStream output) throws RemoteException, NotBoundException
	{

		Registry registry = LocateRegistry.getRegistry("localhost"); 
		INodeHandler nodeHandler = (INodeHandler)registry.lookup("NodeHandler");
		String filePath = "../testData";
		File folder = new File(filePath);
		
		String response = "";
		response = response + "<html>";
		response = response + "<body>";
		response = response + "<form action=\"/get_task\" method=\"POST\" enctype=\"multipart/form-data\">";
		response = response + "<input type=\"text\" name=\"name\" placeholder=\"File name\" required/>";
		//response = response + "<input type=\"file\" name=\"content\" required/>";
		response = response + "<input type=\"submit\" name=\"submit\"/>";
		response = response + "</form>";
		//	byte[] file = nodeHandler.fetchTasks(fileName);

		
	
		
		ArrayList<Store> retrievedStoreArray = nodeHandler.RetrieveTasks();
		ArrayList<String> names = new ArrayList<String>();
		//HashSet<Store> retrievedStoreArrayFinal = new HashSet<>(retrievedStoreArray);

	
		
		if(retrievedStoreArray!=null && !retrievedStoreArray.isEmpty())
		{
			for(int i=0;i<retrievedStoreArray.size();i++)
			{
				Store gotStore = retrievedStoreArray.get(i);
				if(!names.contains(gotStore.fileName))
				{
					response = response + "<p>"+gotStore.fileName+" HAS completed their task"+"</p>";
					names.add(gotStore.fileName);
				}
				
			}
		
		}
		else
		{
			response = response + "<p> No tasks have been completed yet</p>";
			
		}
		response = response + "</body></html>";
		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());

	}


	@Override
	public void listFiles(OutputStream output) throws RemoteException, NotBoundException {
		// TODO Auto-generated method stub
		
	}


	public void DisplayDoneTasks(OutputStream output) throws RemoteException, NotBoundException{
		Registry registry = LocateRegistry.getRegistry("localhost"); 
		INodeHandler nodeHandler = (INodeHandler)registry.lookup("NodeHandler");

		//System.out.println("starting displayDoneTasks..");
		String filePath = "../testData";
		File folder = new File(filePath);

		String response = "";
		response = response + "<html>";
		response = response + "<body>";


		//byte[] file = nodeHandler.fetchTasks(fileName);
		ArrayList<Store> retrievedStoreArray = nodeHandler.RetrieveTasks();
		if(retrievedStoreArray !=null)
		{
			for(int i=0;i<retrievedStoreArray.size();i++)
			{
				Store gotStore = retrievedStoreArray.get(i);
				response = response + "<p>"+gotStore.fileName+"has completed their task"+"</p>";
			}
			response = response + "</body></html>";
		}
		else
		{
			response = response + "<p> No tasks have been completed yet</p>";
		}
		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());


	}

	/*
	 * Only display files that are in the network already
	 * 
	 * Allows a user to download an XML file
	 */
	public void processTask(OutputStream output,String fileName) throws RemoteException, NotBoundException { //if zip redirect to zip download
		
		Registry registry = LocateRegistry.getRegistry("localhost"); 
		INodeHandler nodeHandler = (INodeHandler)registry.lookup("NodeHandler");

		
		String response = "";
		response = response + "<html>";
		response = response + "<body>";

		//ArrayList<byte[]> files = new ArrayList<byte[]>();
		byte[] file = nodeHandler.fetchTasks(fileName);
		String finalFileName = new String(fileName);
		//System.out.println("File inside process task"+finalFileName);
		
		if(file!=null) //add html code that displays no info was found otherwise
		{
			try{


				//Handle the user inputting the wrong file name here or if its not already been processed, otherwise page will just crash
				String newFileName = fileName.substring(0,fileName.lastIndexOf("."));

				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				byteArrayOutputStream.write(file);
				
				File fileOutput = new File("../"+newFileName+".xml");
				try(FileOutputStream fos = new FileOutputStream(fileOutput))
				{
					fos.write(file);

				}
				catch(IOException e)
				{
					e.printStackTrace();
				}


				

				sendResponse(output, RESPONSE_OK, "application/xml", byteArrayOutputStream.toByteArray());
				}
				catch(IOException e) {
					e.printStackTrace();
				}
		}
		else 
		{
			sendResponse(output, RESPONSE_OK, "text/html", "<html>File doesnt exist!</html>".getBytes());

		}
		

		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes());
	}
		
	//this function maps POST requests onto functions / code which return HTML pages
	public void post(HTTPRequest request, byte payload[], OutputStream output) throws RemoteException, NotBoundException
	{
			if (request.resource.equals("/upload_do")) 
			
				{
				//FormMultipart
				if (request.getHeaderValue("content-type") != null && request.getHeaderValue("content-type").startsWith("multipart/form-data"))
					{
					FormData data = formParser.getFormData(request.getHeaderValue("content-type"), payload);
					System.out.println(data);
					for (int i = 0; i < data.fields.length; i++)
						{
						System.out.println("field: " + data.fields[i].name);
						
						if (data.fields[i].name.equals("content"))
						{
							System.out.println(" -- filename: " + ((FileFormField) data.fields[i]).filename);
							
							String encoding = "Cp1250";
							String filePath = ((FileFormField) data.fields[i]).filename;
							System.out.println("File Path is: "+filePath);
							File file = new File(filePath);

							
							// Once retrieved a file, assign a chord node to handle this by sending a request

							try {
								Registry registry = LocateRegistry.getRegistry("localhost"); //export this one as well then connect to the nodeHandler
								INodeHandler nodeHandler = (INodeHandler)registry.lookup("NodeHandler");
								//Once we have the text file, we need to send this to NodeHandler to assign this to a node

								nodeHandler.AssignChordNodeATask(data.fields[i].content,filePath); //send our data from upload to NodeHandler


							} catch (NotBoundException e) {
								e.printStackTrace();
								// TODO: handle exception
							}
							
	
	
	
						}
						
						}
					
					sendResponse(output, RESPONSE_OK, "text/html", "<html>File sent, thanks!</html>".getBytes());
					}
					else
					{
					sendResponse(output, RESPONSE_SERVER_ERROR, null, null);
					}
				}

			/*
			 * Handles when the user is trying to retrieve a task
			 */
			if(request.resource.equals("/get_task"))
			{
				if (request.getHeaderValue("content-type") != null && request.getHeaderValue("content-type").startsWith("multipart/form-data"))
					{
					FormData data = formParser.getFormData(request.getHeaderValue("content-type"), payload);
					System.out.println(data);
					for (int i = 0; i < data.fields.length; i++)
						{
						System.out.println("field: " + data.fields[i].name);
						
						if (data.fields[i].name.equals("name"))
						{
							
							String encoding = "Cp1250";
							
							byte[] x = data.fields[i].content;
							String x2 = new String(x);
							//System.out.println(x2);
							
							// Once user clicked on submit to retrieve a task, handle this by getting the data

							processTask(output,x2);
						}
						
						}
					
					//sendResponse(output, RESPONSE_OK, "text/html", "<html>File sent, thanks!</html>".getBytes());
						
					}
					else
					{
					sendResponse(output, RESPONSE_SERVER_ERROR, null, null);
					}
			}
			
	
			
	}
	
}