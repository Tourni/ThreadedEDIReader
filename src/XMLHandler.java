import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author liamg
 */
public class XMLHandler implements Runnable
{
	private File xlstStyleSheet;
	private DirectoryManager input, output, csvOutput;
	private CountDownLatch ediDone, filesProcessed;

	/**
	 * @param input
	 * @param output
	 */
	public XMLHandler(DirectoryManager input, DirectoryManager xmlOutput, DirectoryManager csvOutput,
			CountDownLatch ediDone)
	{
		xlstStyleSheet = new File("Config/edi2csv.XLST");
		this.input = input;
		this.output = xmlOutput;
		this.csvOutput = csvOutput;
		this.ediDone = ediDone;
	}

	/**
	 * 
	 */
	@Override
	public void run()
	{
		try
		{
			// wait for EDI to finish.
			ediDone.await();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		input.listContentsFiltered(".xml");
		List<String> xmlFiles = input.getFileList();
		if (!(xmlFiles.size() == 0))
		{
			filesProcessed = new CountDownLatch(xmlFiles.size() - 1);

			for (Iterator<String> it = xmlFiles.iterator(); it.hasNext();)
			{
				File tempFile = new File(input.getDirectory() + "\\" + it.next());
				XMLtoCSVWorker xsw = new XMLtoCSVWorker(tempFile, this);
				Thread aThread = new Thread(xsw, "XML Thread");
				aThread.run();
			}

			// wait for all threads to finish then release the latch.
			try
			{
				filesProcessed.await();
			}
			catch (InterruptedException e1)
			{
				e1.printStackTrace();
			}
		}
		System.out.println("Transform complete");
	}

	public DirectoryManager getCsvOutput()
	{
		return csvOutput;
	}

	public DirectoryManager getInput()
	{
		return input;
	}

	public DirectoryManager getOutput()
	{
		return output;
	}

	public CountDownLatch getFilesProcessed()
	{
		return filesProcessed;
	}

	public File getXlstStyleSheet()
	{
		return xlstStyleSheet;
	}
}
