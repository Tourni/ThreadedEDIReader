import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;

public class EDIREADER
{
	public static void main(String[] args)
	{
		PrintStream origErr = System.err; // create a ref to the original err 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();  //output stream for new errors
		PrintStream newErr = new PrintStream(baos); //init the new errors
		System.setErr(newErr); //set the system to the new errors.
		
		CountDownLatch doneSignal = new CountDownLatch(1);
		
		// Setup Retention
		int processed = 90;
		int duplicate = 7;

		// Setup the directories
		DirectoryManager fileInput = DirectoryManager
				.getDirectoryManager("FileInput");
		DirectoryManager ediProcessed = DirectoryManager
				.getDirectoryManager("FileInput\\EDI Processed");
		DirectoryManager duplicates = DirectoryManager
				.getDirectoryManager("Duplicates");
		DirectoryManager xmlProcessed = DirectoryManager
				.getDirectoryManager("FileInput\\XML Processed");
		DirectoryManager csv = DirectoryManager
				.getDirectoryManager("CSVOutput");

		//filter Edi files, remove duplicates, process. 
		fileInput.listContentsFiltered(".edi");
		fileInput.moveDuplicateFiles(ediProcessed);		
		EDIFactHandler ediHandler = new EDIFactHandler(fileInput, ediProcessed, doneSignal);
		ediHandler.processToXML();	
					
		// set the filter for xml files
		fileInput.listContentsFiltered(".XML");
		fileInput.moveDuplicateFiles(xmlProcessed);

		// run a thread to convert XML to CSV
		XMLHandler xmlHandler = new  XMLHandler(fileInput, xmlProcessed, csv, doneSignal);	
		Thread xml = new Thread(xmlHandler, "xml");
		xml.run();
		
		// clean directories
		ediProcessed.deleteFilesOlderThan(processed);
		xmlProcessed.deleteFilesOlderThan(processed);
		duplicates.deleteFilesOlderThan(duplicate);
		
		System.setErr(origErr);  //return to original error stream. 
		
		//System.err.print(baos); 
	}
}
