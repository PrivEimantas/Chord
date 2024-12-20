//Libraries for ChordNode
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
//Libraries for making the xml
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;


import java.util.Comparator;
import java.net.ConnectException;
import java.io.*;


class Finger{
	public int key;
	public IChordNode node;
	}

class Store implements Serializable{
	String key;
	byte[] value;
	String fileName; //store the file name for retrieval when fetching all done tasks
	
}

class CreateXML{ //Uses libraries which help create XML documents

	public byte[] createXMLFileText(String totalNumWords,String freqOccurWord, String avgWordLen)
	{
		try {
			//System.out.println("reaching 2");

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			Document doc = dBuilder.newDocument();
			Element rooElement= doc.createElement("displayingTask");
			doc.appendChild(rooElement);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			Text totalNumberOfWords = doc.createTextNode("Total Word Count: "+totalNumWords);
			Text mostFrequentlyOccuringWord = doc.createTextNode("Most Frequent Word: "+freqOccurWord);
			Text averageWordLength = doc.createTextNode("Average Word Length: "+avgWordLen);

			Element totNumWord = doc.createElement("totalNumberOfWords");
			totNumWord.appendChild(totalNumberOfWords);
			rooElement.appendChild(totNumWord);

			Element freqWord = doc.createElement("frequentlyOccuringWords");
			freqWord.appendChild(mostFrequentlyOccuringWord);
			rooElement.appendChild(freqWord);
			
			Element avgLen = doc.createElement("averageWordLength");
			avgLen.appendChild(averageWordLength);
			rooElement.appendChild(avgLen);

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(doc), new StreamResult(outputStream));

			//System.out.println("reaching 3");
			return outputStream.toByteArray();
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
			// TODO: handle exception
		}
	}
	/*
	 * THis one is specifically for the csv file format
	 */
	public byte[] createXMLFileCSV(String rowCount,String colCount,String totalWordCount)
	{
		try {
			//System.out.println("reaching 2");

			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

			Document doc = dBuilder.newDocument();
			Element rooElement= doc.createElement("displayingTask");
			doc.appendChild(rooElement);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			Text csvRowCount = doc.createTextNode("Row Count: "+rowCount);
			Text csvColumnCount= doc.createTextNode("Column Word: "+colCount);
			Text csvtotalWordCount= doc.createTextNode("Total Word Count: "+totalWordCount);

			Element rowCountE = doc.createElement("rowCount");
			rowCountE.appendChild(csvRowCount);
			rooElement.appendChild(rowCountE);

			Element columnCountE = doc.createElement("columnCount");
			columnCountE.appendChild(csvColumnCount);
			rooElement.appendChild(columnCountE);

			Element totalWordCountE = doc.createElement("TotalWordCount");
			totalWordCountE.appendChild(csvtotalWordCount);
			rooElement.appendChild(totalWordCountE);
			
		
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(doc), new StreamResult(outputStream));

			//System.out.println("reaching 3");
			return outputStream.toByteArray();
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
			// TODO: handle exception
		}
	}

}

// MAke it so when a node joins/reboots it checks the file
public class ChordNode implements IChordNode,Runnable{
	
	static final int KEY_BITS = 8;
	
	//for each peer link that we have, we store a reference to the peer node plus a "cached" copy of that node's key; this means that whenever we change e.g. our successor reference we also set successorKey by doing successorKey = successor.getKey()
	IChordNode successor;
	int successorKey;
	
	IChordNode predecessor;
	int predecessorKey;
	
	//my finger table; note that all "node" entries will initially be "null"; your code should handle this
	int fingerTableLength;
	Finger finger[];
	int nextFingerFix;
	int hashNumber;

	boolean isWorking;
	
	HashMap<Integer,Store> dataStore = new HashMap<Integer,Store>();

	boolean connected;
	String rmiNodeName; 
	String nodeName;
	
	//note: you should always use getKey() to get a node's key; this will make the transition to RMI easier
	private int myKey;
	
	ChordNode(String myKeyString)
		{
		myKey = hash(myKeyString);

		nextFingerFix=-1;

		successor = this;
		successorKey = myKey;
		isWorking=false;
		

		//initialise finger table (note all "node" links will be null!)
		finger = new Finger[KEY_BITS];
		for (int i = 0; i < KEY_BITS; i++)
			finger[i] = new Finger();

		
		fingerTableLength = KEY_BITS;

	
		//start up the periodic maintenance thread
		
		new Thread(this).start();
		}
	
	// -- API functions --
	
	public void put(String key, byte[] value) throws RemoteException
	{
			//this.isWorking = true; //current node is busy with a task


			System.out.println("Key is: "+key);
			System.out.println("Adding file to node n "+ hash(key));
			
			int tempKey = hash(key);
		

			IChordNode tempNode = findSuccessor(tempKey);
			System.out.println("Assigned task to node: "+tempNode.getKey());

			

			new Thread( () -> { //Creates new store and does task asynchronously, so that one node can do multiple tasks at once
				Store tempStore = new Store(); //set up a placeholder value to store values
				try {
					tempStore.key = Integer.toString(tempNode.getKey());
					tempStore.value = tempNode.doTask(tempNode,value,key); //make this async by calling a thread

					tempStore.fileName = key; //store the name of the file inside store
				} catch (RemoteException e) {
					// TODO Auto-generated catch block

					e.printStackTrace();
				}

				if(tempStore.value!=null){
					try {
						tempNode.updateDataStore(key,tempStore); //updates the node responsible with the data
					} catch (Exception e) {
						e.printStackTrace();
						// TODO: handle exception
					}
					
					//dataStore.put(hash(key),tempStore); //order of datastore entries does not matter because of the hash
					
		
					//Save as soon as its done with a task, for robustness
					try {
						
						
						tempNode.savingFile();
					} catch (Exception e) {
						e.printStackTrace();
						// TODO: handle exception
					}
				}
				
			}).start();

			

			
		
	}

	/*
	 * Key is the filename, so will be hashed and located in the hashmap of datastore
	 * if found then will return the value otherwise returns null
	 */
	public byte[] get(String key) throws RemoteException
	{
		//find the node that should hold this key, request the corresponding value from that node's local store, and return it
		System.out.println("Key is: "+key);
		System.out.println("retrieving file from node n "+ hash(key));
		byte[] tempValue;
		int tempKey = hash(key);
		IChordNode tempNode = findSuccessor(tempKey);

		//tempNode.savingFile(); //backup data.. not doing anything

		System.out.println("Assigned task to node: "+tempNode.getKey());
		
		Store tempValueX = tempNode.getDataStore().get(hash(key));

	
		if(tempValueX!=null)
		{
			System.out.println("successfully retrieved file from node: "+Integer.toString(tempNode.getKey()));

			

			return tempValueX.value;
		}


		

		return null;
	}


	//Helper functions

	public HashMap<Integer,Store> getDataStore() throws RemoteException
	{
		return dataStore;
	}

	public void updateDataStore(String keyToStore, Store storeToStore ) throws RemoteException
	{

		dataStore.put(hash(keyToStore),storeToStore);
	}

	

	public void savingFile() throws RemoteException //intermediete function for in-between
	{
		String currentKey = Integer.toString(getKey());
		

		successor.saveObjectFile(currentKey,dataStore);
	}

	/*
	 * Replication works by saving to your successor, your data + theirs, then into folder 'storage'
	 * So any time data needs to be collected, its done from 'storage'
	 */
	public void saveObjectFile(String currentKey,HashMap<Integer,Store> receivedDataStore ) throws RemoteException
	{
		//e.g. 120's data is passed
		String filePath = "./storage/"+Integer.toString(getKey()); // + currentKey

		//System.out.println("Node:"+Integer.toString(getKey())+"is saving daat");
		HashMap<Integer,Store> tempDataStore = new HashMap<>(dataStore);
		tempDataStore.putAll(receivedDataStore);
		//dataStore.putAll(receivedDataStore); //adds on our predecessor's data to our own and we save our data
		

		try {
			File fileOne = new File(filePath);
			FileOutputStream fos = new FileOutputStream(fileOne);
			ObjectOutputStream oos = new ObjectOutputStream(fos);

			oos.writeObject(tempDataStore);
			oos.flush();
			oos.close();
			fos.close();

		} catch (Exception e) {
			//e.printStackTrace();
			// TODO: handle exception
		}
		/* TESTING
		try(FileOutputStream fileOut = new FileOutputStream(filePath); 
			ObjectOutputStream out = new ObjectOutputStream(fileOut)) {

				out.writeObject(dataStoreToSave);
				System.out.println("Object has been serialized and saved to:"+filePath);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		*/
	}

	/*
	 * Loading the files saved in storage, was checked extensively and there are no issues 
	 * but compiler did not like this
	 */
	@SuppressWarnings("unchecked")
	public HashMap<Integer,Store> loadObjectFile(String filePath) throws RemoteException
	{
		
		try {
			File fileRead = new File(filePath);

			if(fileRead.exists())
			{
				FileInputStream fis = new FileInputStream(fileRead);
				ObjectInputStream ois = new ObjectInputStream(fis);

				HashMap<Integer,Store> loadedDataStore = (HashMap<Integer,Store>)ois.readObject();

				ois.close();
				fis.close();

				return(loadedDataStore);
			}
			return null;

			/* TESTING PURPOSES - CAN SAVE AND LOAD DATASTORE
			for(Map.Entry<Integer,Store> m: loadedDataStore.entrySet())
			{
				
				System.out.println(m.getKey()+" : "+m.getValue());
				String str = new String(m.getValue().value);
				System.out.println(str);
			
			}
			*/
			

		} catch (Exception e) {
			//e.printStackTrace();
			return null;
			// TODO: handle exception
		}
		
	}


	public int countTotalWords(byte[] value) throws RemoteException
	{
		String myFile = new String(value);
		int countTotalWords = myFile.split(" ").length;
		return countTotalWords;
	}


	/*
	 * Will check of the file type and if its valid
	 * Returns a byte array of the completed task in XML using the builder
	 */
	public byte[] doTask(IChordNode node,byte[] value,String key) throws RemoteException
	{

		
	
		try {	 

			if(key.contains(".txt")) //Base tasks - TEXT FILES
			{
				
			String totalNumberOfWords = Integer.toString(node.countTotalWords(value));
			String mostFrequentlyOccuringWord = (node.mostFrequentlyOccuringWord(value));
			String averageWordLength = Float.toString(node.averageWordLength(value));
		
			//need as string so once the tasks are done, the file can be converted to XML
			CreateXML xmlConverter = new CreateXML();
			byte[] converted = xmlConverter.createXMLFileText(totalNumberOfWords,mostFrequentlyOccuringWord,averageWordLength);
			return converted;
			}

			if(key.contains(".csv"))
			{
				System.out.println("processing csv file");
				String countRows = Integer.toString(node.countCSVRows(value));
				String countColumns =  Integer.toString(node.countCSVColumns(value));
				String totalNumberOfWords =  Integer.toString(node.RealWordsCSV(value));


				

				CreateXML xmlConverter = new CreateXML();
				byte[] converted = xmlConverter.createXMLFileCSV(countRows,countColumns,totalNumberOfWords);
				return converted;
				

			}
			

			System.out.println("file type is not valid");
			return null; //File type is not valid
			


		} catch (Exception e) {
			//e.printStackTrace();
			return null;
			// TODO: handle exception
		}


	}

	public String mostFrequentlyOccuringWord(byte[] value) throws RemoteException
	{
		String myFile = new String(value);
		String[] splitted = myFile.split(" ");
		System.out.println(Arrays.toString(splitted));
		
		int freq=0;
		String output="";

		for(int i=0;i<splitted.length;i++)
		{
			int count=0;
			for(int j=i+1;j<splitted.length;j++)
			{
				
				if(splitted[j].equals(splitted[i]))
				{
					count++;
				}
			}
			if(count>=freq)
			{
				output=splitted[i];
				freq=count;
			}
		}

		return output;

		

	}


	public float averageWordLength(byte[] value) throws RemoteException
	{
		String myFile = new String(value);
		String[] splitted = myFile.split(" ");

		int sum=0;
		int count=0;
		for(String word: splitted)
		{
			int wordLength = word.length();
			sum+= wordLength;
			count++;
		}


		float average=0;
		if(count>0)
		{
			average=sum/count;
		}
		return average;


	}

	public String longestWord(byte[] value) throws RemoteException
	{
		String myFile = new String(value);
		String[] splitted = myFile.split(" ");

		String longest = Arrays.stream(splitted).max(Comparator.comparingInt(String::length)).orElse(null);
		return longest;

	}

	public String shortestWord(byte[] value) throws RemoteException
	{
		String myFile = new String(value);
		String[] splitted = myFile.split(" ");

		String shortest = Arrays.stream(splitted).min(Comparator.comparingInt(String::length)).orElse(null);
		return shortest;
	}


	public Integer countCSVRows(byte[] value) throws IOException, RemoteException
	{
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value);

		InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);
		BufferedReader reader = new BufferedReader(inputStreamReader);

		String line;
		int rowCount=0;

		while((line = reader.readLine())!=null)
		{
			rowCount++;
		}

		reader.close();

		return rowCount;

	}

	public Integer countCSVColumns(byte[] value) throws IOException, RemoteException
	{
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value);

		InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);
		BufferedReader reader = new BufferedReader(inputStreamReader);

		String firstLine = reader.readLine();
		if(firstLine == null)
		{
			System.out.println("csv file is empty");
		}

		String[] columns = firstLine.split(";"); //this one uses semi-colons to seperate, could be commas instead
		int totalCount=0;
		for(String valueInCOl : columns)
		{
			totalCount++;
		}
		//System.out.println("totla column count"+totalCount);
		return totalCount;

	}

	/*
	 * Unlike the text analysis, which will just count every string, this checks if the value
	 * in the CSV file is ACTUAL text (words, not numbers)
	 */
	public Integer RealWordsCSV(byte[] value) throws IOException, RemoteException
	{

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value);

		InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);
		BufferedReader reader = new BufferedReader(inputStreamReader);


		List<String> AllWords = new ArrayList<>();
		String line;

		while((line=reader.readLine())!=null)
		{
			String[] cells = line.split(";");
			for(String cell: cells)
			{
				AllWords.add(cell.trim());
			}
		}

		String[] output = AllWords.toArray(new String[0]);
		int totalCount=0;


		for(String item : output )
		{
			if(!isNumber(item)){
				totalCount++;
				//System.out.println(item);
				//System.out.println(totalCount);
			}
		}
		totalCount--; //because of final character that is blank
		if(totalCount>0)
		{
			return totalCount;
		}
		else{
			return 0;
		}

		
	}
	
	/*
	 * For checking csv files, helper function
	 */
	public boolean isNumber(String input)
	{
		return input.matches("\\d+");
	}
	
	
	
	
	// -- state utilities --
	
	public int getKey() throws RemoteException
		{
		return myKey;
		}

	public boolean isBusy() throws RemoteException
	{
		return isWorking;
	}
	
	public IChordNode getPredecessor() throws RemoteException
	{
	return predecessor;
	}

	


		//TESTING PURPOSES
	public int getSuccessorKey() throws RemoteException
	{
		return successor.getKey();
	}

	public Integer getPredecessorKey() throws RemoteException
	{
		if(predecessor==null)
		{
			
			return null;
		} 
		else return predecessor.getKey();
	}

	
	// -- topology management functions --
	public void join(IChordNode atNode) throws  RemoteException
		{
		
		this.predecessor = null;
		
		this.successor = atNode.findSuccessor(this.getKey());
		this.successorKey = this.successor.getKey();
		
		
		}
	
	// -- utility functions --


	/*
	 * Check if a node is in the range, or if the node trying to connect is completely new, just add straight away
	 * Find its closests preceding node otherwise
	 */
	public IChordNode findSuccessor(int key) throws RemoteException
	{	
		//System.out.println("Finding successor");

		//if(this.successor==this){
		//	return this;
		//}
		
		if( successor == this || isInHalfOpenRangeR(key, this.getKey(), successor.getKey())  )
		//if(key > this.getKey() && key <= this.successor.getKey() )
		{
			
			return this.successor;

		}
		else{
			//System.out.println("Looking again for a successor");
			IChordNode n0 = closestPrecedingNode(key);
			if(n0 != this)
			{
				return n0.findSuccessor(key);
			}
			else
			{
				return this;
			}
		}

		
		
	}

	
	
	/*
	 * Queries the finger table for the closest node to prevent lookup times from being O(n) and instead O(log(n))
	 */
	public IChordNode closestPrecedingNode(int key) throws RemoteException
		{
			//System.out.println("Looking at closests preceding node..");
			for(int i=this.fingerTableLength-1;i>=0;i--){
				
				if(finger[i].node != null && isInOpenRange(this.finger[i].key, this.getKey(), key))
				//if(this.finger[i].node != null && inInterval(this.finger[i].key, this.getKey(), key))
				{
					return this.finger[i].node;
				}
			}
		return this;
		}



	/*
	 * Is the node alive? It doesnt need anything inside because if its dead, the rmi call wont be received
	 */
	public boolean isAlive() throws RemoteException  
	{          
		return true;
	}
	
	
	
	// -- range check functions; they deal with the added complexity of range wraps --
	// x is in [a,b] ?
	boolean isInClosedRange(int key, int a, int b)
		{
			if (b > a) return key >= a && key <= b;
			else return key >= a || key <= b;
		}
	
	// x is in (a,b) ?
	boolean isInOpenRange(int key, int a, int b) //it includes a wrap-around thats why its got the else statement
		{
			if (b > a) return key > a && key < b;
			else return key > a || key < b;
			//return key > a && key < b;
		}
	
	// x is in [a,b) ?
	boolean isInHalfOpenRangeL(int key, int a, int b)
		{
		if (b > a) return key >= a && key < b;
		else return key >= a || key < b;
		}
	
	// x is in (a,b] ?
	boolean isInHalfOpenRangeR(int key, int a, int b)
		{
				//return key > a && key <= b;
			if (b > a) return key > a && key <= b;
			else return key > a || key <= b;
		}
	
	// -- hash functions --
	//this function converts a string "s" to a key that can be used with the DHT's API functions
	public int hash(String s)
		{
		int hash = 0;
		
		for (int i = 0; i < s.length(); i++)
			hash = hash * 31 + (int) s.charAt(i);
		
		if (hash < 0) hash = hash * -1;
		
		return hash % ((int) Math.pow(2, KEY_BITS));
		}
	
	// -- maintenance --
	public void notify(IChordNode potentialPredecessor) throws RemoteException
		{
			
				if( (this.predecessor == null) || (isInOpenRange(potentialPredecessor.getKey(), this.predecessor.getKey(), this.getKey())) ){
					this.predecessor = potentialPredecessor;
					this.predecessorKey = this.predecessor.getKey();
				} 
			
			
		}
	/*
	 * Called periodically, it will call notify to update the predecessor.
	 * Detects any successor dying as well
	 */
	public void stabilise() throws RemoteException
		{
			try {
				
				IChordNode x = this.successor.getPredecessor(); //changed from predecessor to getPredecessor()
				


				if(x != null &&  isInOpenRange(x.getKey(), this.getKey(), this.successor.getKey())){
					this.successor = x;
					this.successorKey = x.getKey();
				}
				successor.notify(this);
			} catch (RemoteException e) {
				System.out.println(LocalTime.now());
				//e.printStackTrace();
				
				//if successor has died, then just set successor back to itself

				this.successor = this;
				this.successorKey = this.getKey();

				//Initially done to correct nodes dying/rejoining..
				//this.predecessorKey = this.getKey();
				//this.predecessor=this;
				
				successor.notify(this);
				
			}
				
			
			
		}
	/*
	 * Periodically updates the finger table
	 */
	public void fixFingers() throws RemoteException

		{


			

			
			//System.out.println("Fixing fingers..");
			this.nextFingerFix = this.nextFingerFix +1;
			if (this.nextFingerFix > this.fingerTableLength-1){
				this.nextFingerFix=0;

			}
			int mod_value = (int) (this.getKey()+Math.pow(2,this.nextFingerFix) );
			int fingerValue = (int) (mod_value % Math.pow(2,this.fingerTableLength));

			//System.out.println(mod_value);

			this.finger[this.nextFingerFix].node = this.findSuccessor(fingerValue);
			

			this.finger[this.nextFingerFix].key =  this.finger[this.nextFingerFix].node.getKey(); //fingerValue; //this.finger[this.nextFingerFix].node.getKey();
			
			
		}

	/*
	* If a node has died, it will retrieve it's predecessor values and load them in so the user
	can keep using the program without being aware of a node dying
	*/
	public void FindBackup() throws RemoteException
	{

		//Cannot fetch data from predecessor anymore, we we should fetch the data that it was storing
		File folder = new File("./storage/"); //look in storage folder where we store node datastores
		if(folder.exists() && folder.isDirectory())
		{
			File[] files = folder.listFiles();
			if(files!=null){ //if no files, then there was nothing to retrieve

				
				for(File file: files)
				{
					if(file.isFile())
					{
						String fileName= file.getName();
						
						//if(fileName.contains("-"+Integer.toString(predecessorKey)))
						if(fileName.contains(Integer.toString(getKey())))
						{
							//String[] twoNodes = fileName.split("-");
							//if(Integer.toString(getKey()).equals(twoNodes[0])){
								//Retrieve the data..
								//System.out.println("attempting to load the data back in");
								//System.out.println(fileName);
								dataStore=loadObjectFile("./storage/"+fileName);
							//}
						}
					}
				}
			}
		}

	}
	
	/*
	 * Has the predecessor died? If so, this node will need to load in the data so it has the
	 * predecessor's data
	 */
	public void checkPredecessor() throws RemoteException
		{
			try {
				if(!predecessor.isAlive()){
					//this.predecessor=null;
					System.out.println(LocalTime.now());
					this.predecessorKey = this.getKey();
					this.predecessor=this;


				}
			} catch (Exception e) {
				System.out.println("pred is dead");
				System.out.println(LocalTime.now());

				//Initially done when nodes died/rejoining
				//this.predecessorKey = this.getKey(); //reset keys
				//this.predecessor=this;
				predecessor=null;
				FindBackup(); // otherwise just insert block of code again
				

			
			}
			
			
			
		}
	

	/*
	 * When loading in new info, if a node comes back online and it should be holding the values, then this handles that
	 * Just moves values down in the network so the node that should be responsible for it, is the one holding the data
	 */
	public void checkDataMoveDown() throws RemoteException
	{
	//if I'm storing data that my current predecessor should be holding, move it
		if(dataStore!=null)
		{
			for(Map.Entry<Integer,Store> entry: dataStore.entrySet())
			{
			Integer currKey = entry.getKey();
			Store currStore = entry.getValue();

			String thisKey = currStore.key;
			

			if(predecessor!=null)
			{
				//IChordNode findSucc = findSuccessor(currKey);
				//if(predecessor.getKey()==findSucc.getKey() && predecessor.getKey() != getKey()) //if my predecessor is the successor of my current file
				// i.o.w if predecessor should be holding it now and not me then remove current and tell pred to add it to its data store
				//this is wrong bc its asking if the pred is our successor (e.g. 120 and 144, if add text then wont be moved to 47)
				if( (predecessor.getKey() >= currKey || currKey >= getKey() ) &&  predecessor.getKey() <= getKey())
				//if( ( predecessor.getKey() <= getKey() )||   (currKey >= predecessor.getKey() || currKey <= getKey() ))
				{
					dataStore.remove(currKey);
					predecessor.moveTheData(currKey,currStore); //move the data then remove the one we have, otherwise infinite loop
					
				}
				
			}

			}
		}
		
	}
	/*
	 * Intermediete function call for the predecessor
	 */
	public void moveTheData(Integer key,Store store) throws RemoteException
	{
		
		//System.out.println("Putting key"+Integer.toString(key)+" inside:"+Integer.toString(getKey()));
		//System.out.println("this key is "+Integer.toString(getKey()));
		dataStore.put(key, store);
	}

	/*
	 * Join the main server
	 */
	public void establishConnection() throws RemoteException
	{
		try {
			Registry registry = LocateRegistry.getRegistry("localhost");
			INodeHandler nodeHandler = (INodeHandler)registry.lookup("NodeHandler");
				
			System.out.println("Trying to connect node with key "+ rmiNodeName );

			//get list of nodes in network and try to connect

			
			if(!nodeHandler.getChordNodesLog().isEmpty()) //what if its not the first node in network?
			{	
				System.out.println("ChordNodesLog is not empty..");
				int endOfLog = nodeHandler.getChordNodesLog().size() -1;
				//System.out.print("got size of chordlog");
				
				nodeHandler.getChordNodesLog().get(endOfLog).join(this); //just join it to the end, the network will fix itself anyways
				//the items must be of the interface because it says argument mismatch otherwise
			}

			nodeHandler.addChordNode(registry,this,nodeName);
			

			connected=true; //prevents from infinitely allowing many to join at once
			


		} catch (Exception e) {
			System.out.println("Issue with connecting node to main server");
			//e.printStackTrace();
			
			System.out.println("retrying to connect");

		}
	}

	/*
	 * Will check if a server is alive and try to connect to it, so if server dies and comes back, it can rejoin the server
	 */
	public void isServerAlive() throws RemoteException, NotBoundException
		{
			Registry registry = LocateRegistry.getRegistry("localhost");
			INodeHandler nodeHandler = (INodeHandler)registry.lookup("NodeHandler");
	
			try {
				if(nodeHandler.isAlive() && connected==false) // if already connected, dont let it connect again
				{
					establishConnection();
					FindBackup();
				}
			} catch (Exception e) {
				connected=false;
				//Server has crashed
				//Thread.sleep(1000); //wait a bit and retry to join
				//Nodes should retrieve data from secondary storage too..
				establishConnection();
				
			}
		}

		/*
		 * Periodic calls via Thread in constructor
		 */
		
		public void run()
			{
			while (true)
				{
				try{
					Thread.sleep(500);
					//System.out.println("This node is "+ this.getKey() + " the successor's are "+ successor.getKey() + " and predecessor is "+ predecessor.getKey());
					}
					catch (InterruptedException e){
					System.out.println("Interrupted");
					}
				
				try{
					stabilise();
					}
					catch (Exception e)
					{
						//System.out.println("error in stabilise");
						//e.printStackTrace();
					}
				
				try{
					fixFingers();
					}
					catch (Exception e)
	
					{
						//System.out.println("error in fix fingers");
						//e.printStackTrace();
						
					}
				
				try{
					checkPredecessor();
					}
					catch (Exception e){
						//System.out.println("error in check predecessor");
						//e.printStackTrace();
						
					}
				
				try{
					checkDataMoveDown();
					}
					catch (Exception e)
					{
						//System.out.println("error in check data move down");
						
					}


				try {
					isServerAlive();
				} catch (Exception e) {
					//System.out.println("error in server alive function");
					//e.printStackTrace();
					// TODO: handle exception
				}

				try {
					savingFile();
				} catch (Exception e) {
					//e.printStackTrace();
					// TODO: handle exception
				}
				


				}

	
	
			}
		
		public static void main(String args[]) throws RemoteException, NotBoundException
		{
	
	
			if(args.length==1){
	
				//If main server dies, can possible just add a loop here to check for that
	
				String nodeName = args[0]; //can add a check for any " - " since im using this to seperate node names with numbers
				ChordNode thisNode = new ChordNode(nodeName);
				String rmiNodeName = "N-"+thisNode.getKey()+"-"+nodeName;
				
				
				Registry registry = LocateRegistry.getRegistry("localhost"); //export this one as well then connect to the nodeHandler
				IChordNode chordNode = (IChordNode) UnicastRemoteObject.exportObject(thisNode,0);
				registry.rebind(rmiNodeName,chordNode);
	
				//INodeHandler nodeHandler = (INodeHandler)registry.lookup("NodeHandler");



				thisNode.nodeName=nodeName;
				thisNode.rmiNodeName=rmiNodeName;
				thisNode.connected=false;
				//connect to rmi and send to nodeHandler our currently created node

				
				thisNode.isServerAlive();
				
			
		}
		else{
			System.out.println("Include a NAME without spaces for a node");
		}
		

		


		}
	
	}