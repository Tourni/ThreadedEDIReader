import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Converts the EDI files to XML files in the directory specified
 * 
 * @author LiamG
 */
public class EDIFactHandler
{
	private DirectoryManager input, output;
	private CountDownLatch ediDone, filesProcessed;
	private List<String> ediFiles;

	/**
	 * Creates an instance of edifact handler initialized with a an input
	 * directory, output directory and a count down latch.
	 * 
	 * @param input
	 * @param output
	 * @param ediDone
	 */
	public EDIFactHandler(DirectoryManager anInput, DirectoryManager anOutput, CountDownLatch aLatch)
	{
		this.input = anInput;
		this.output = anOutput;
		this.ediDone = aLatch;
	}

	/**
	 * Cycling through the files and creating a conversion thread.
	 */
	public void processToXML()
	{

		// protective call - get a new list.
		input.listContentsFiltered(".edi");
		ediFiles = input.getFileList();

		if (!(ediFiles.size() == 0))
		{

			filesProcessed = new CountDownLatch(ediFiles.size() - 1);

			// cycle through files and create a thread for each.
			for (Iterator<String> it = ediFiles.iterator(); it.hasNext();)
			{
				// get the file
				File ediFile = new File(input.getDirectory() + "\\" + it.next());
				EDIFactWorker efw = new EDIFactWorker(ediFile, this);
				Thread aThread = new Thread(efw, "Edi Thread");
				aThread.run();
			}
			// wait for all threads to finish then release the latch.
			try
			{
				filesProcessed.await();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		ediDone.countDown();
	}

	public DirectoryManager getInput()
	{
		return input;
	}

	public DirectoryManager getOutput()
	{
		return output;
	}

	public CountDownLatch getEdiDone()
	{
		return ediDone;
	}

	public CountDownLatch getFilesProcessed()
	{
		return filesProcessed;
	}

	public List<String> getEdiFiles()
	{
		return ediFiles;
	}
}
