import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.milyn.io.FileUtils;

public class XMLtoCSVWorker implements Runnable
{

	private File xlstStyleSheet, tempFile;
	private TransformerFactory transFact;
	private  DirectoryManager output, csvOutput;
	private Transformer transformer;
	private CountDownLatch filesProcessed;

	public XMLtoCSVWorker(File aFile, XMLHandler aXMLHandler)
	{
		this.output = aXMLHandler.getOutput();
		this.csvOutput = aXMLHandler.getCsvOutput();
		this.xlstStyleSheet = aXMLHandler.getXlstStyleSheet();
		filesProcessed = aXMLHandler.getFilesProcessed();
		this.tempFile = aFile;	
		transFact = TransformerFactory.newInstance();
		
	}

	public void run()
	{
		try
		{
			transformXML();
			FileUtils.copyFile(tempFile.getAbsolutePath(),
					output.getDirectory().getAbsolutePath() + "\\" + tempFile.getName());
			tempFile.delete();		
			filesProcessed.countDown();
		}
		catch (TransformerException | IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @throws TransformerException
	 */
	private synchronized void transformXML() throws TransformerException
	{
		// Set file sources for the transformer
		Source xlst = new StreamSource(xlstStyleSheet);
		Source xml = new StreamSource(tempFile);

		transformer = transFact.newTransformer(xlst);
		

		System.out.println("Transforming: " + tempFile.toString());
		transformer.transform(xml, new StreamResult(new File(csvOutput.getDirectory() + "\\" + createFileName())));
	}

	/**
	 * 
	 */
	private synchronized String createFileName()
	{
		int pos = tempFile.getName().lastIndexOf(".");
		String csvFileName = pos > 0 ? tempFile.getName().substring(0, pos) : tempFile.getName();
		String csvFile = csvFileName + ".csv";
		return csvFile;
	}

}
