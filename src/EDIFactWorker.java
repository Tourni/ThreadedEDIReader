import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import javax.xml.transform.stream.StreamSource;

import org.milyn.Smooks;
import org.milyn.SmooksException;
import org.milyn.container.ExecutionContext;
import org.milyn.io.FileUtils;
import org.milyn.io.StreamUtils;
import org.milyn.payload.StringResult;
import org.xml.sax.SAXException;

public class EDIFactWorker implements Runnable
{
	private byte[] messageIn;
	private FileInputStream fis = null;
	private BufferedWriter writer;
	private File ediFile, xmlFile;
	private CountDownLatch filesProcessed;
	private DirectoryManager input;
	private DirectoryManager output; 

	/**
	 * Constructor to create a thread to convert a EDIFACT file to XML
	 * 
	 * @param aFile - the file to be converted
	 * @param ediFactHandler - containing the input and output directories. 
	 */
	public EDIFactWorker(File aFile, EDIFactHandler ediFactHandler)
	{
		this.ediFile = aFile;
		this.filesProcessed = ediFactHandler.getFilesProcessed();
		this.input = ediFactHandler.getInput();
		this.output = ediFactHandler.getOutput();
		
	}

	public void run()
	{
		processFiles();
	}

	private synchronized void processFiles()
	{
		try
		{
			createXMLFile();
			writeXML();
			backupFile();
			filesProcessed.countDown();
		}
		catch (SmooksException | IOException | SAXException e)
		{
			System.out.println("There was a problem converting the file: " + ediFile.toString());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				writer.close();
			}
			catch (Exception ex)
			{
				System.out.println("error trying to close the writer");
				ex.printStackTrace();
			}
		}
	}

	private synchronized void backupFile() throws IOException
	{
		// copy the processed file to the processed dir
		FileUtils.copyFile(ediFile.getAbsolutePath(),
				output.getDirectory().getAbsolutePath() + "\\" + ediFile.getName());
		ediFile.delete();
	}
	/**
	 * Attempts to write the xml data to the file created in createXMLFile
	 * 
	 * @throws SmooksException
	 * @throws IOException
	 * @throws SAXException
	 */
	private synchronized void writeXML() throws SmooksException, IOException, SAXException
	{
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlFile), "utf-8"));
		writer.write(transformEDITOXML(ediFile.getAbsolutePath()));
	}

	/**
	 * Creates an XML file with the same name as the EDIFact file.
	 * 
	 * @throws IOException
	 */
	private synchronized void createXMLFile() throws IOException
	{
		// create a var to store the name of the file without extension, then
		// create the xml file with the same name.
		int pos = ediFile.getName().lastIndexOf(".");
		String ediFileName = pos > 0 ? ediFile.getName().substring(0, pos) : ediFile.getName();
		xmlFile = new File(input.getDirectory() + "\\" + ediFileName + ".XML");
		xmlFile.createNewFile();
	}

	/**
	 * Transforms the file pass in this instance of smooks to a XML file
	 * 
	 * @return - returns a string containing the resulting XML conversion.
	 * @throws IOException - If the stream cannot be read
	 * @throws SAXException - if there is a problem parsing the xml
	 * @throws SmooksException - if there is a problem creating the xml.
	 */
	private synchronized String transformEDITOXML(String filePath) throws IOException, SAXException, SmooksException
	{
		System.out.println("current processing: " + filePath);
		// initiate the input reader
		messageIn = readInputMessage(filePath);

		// set up locale
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(new Locale("en", "IE"));

		// Instantiate Smooks with the config...
		Smooks smooks = new Smooks("smooks-config.xml");

		try
		{
			// Create an exec context - no profiles....
			ExecutionContext executionContext = smooks.createExecutionContext();
			StringResult result = new StringResult();

			// Filter the input message to the outputWriter, using the execution
			// context...
			smooks.filterSource(executionContext, new StreamSource(new ByteArrayInputStream(messageIn)), result);
			Locale.setDefault(defaultLocale);
			return result.getResult();
		}
		finally
		{
			smooks.close();

		}
	}

	/**
	 * Helper method that creates the input stream.
	 * 
	 * @return - byte array containing the input stream text.
	 */
	private synchronized byte[] readInputMessage(String filePath)
	{
		try
		{
			fis = new FileInputStream(filePath);

			return StreamUtils.readStream(fis);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return "<no-message/>".getBytes();
		}
		finally
		{
			try
			{
				fis.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
