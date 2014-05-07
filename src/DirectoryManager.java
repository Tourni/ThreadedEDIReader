import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
/**
 * Directory Manager with generic file management functions.
 * 
 * @author LiamG
 */
public class DirectoryManager
{
	private static Map<File, Object> DirectoryManagers;

	private File dir;
	private List<String> fileList;

	/**
	 * Creates a new instance of DirectoryManager for the directory specified.
	 * 
	 * @param aFile - a directory.
	 */
	private DirectoryManager(String aFile)
	{
		this.dir = new File(aFile);
		this.fileList = new ArrayList<String>();
		this.listContents();
		DirectoryManagers.put(this.dir, this);
	}

	/**
	 * Factory creator to ensure only a single instance of the directory manager
	 * is used for a directory
	 * 
	 * @param aFile
	 * @return a DirectoryManager
	 */
	public static DirectoryManager getDirectoryManager(String aFile)
	{
		File tempDir = new File(aFile);

		if (tempDir.isDirectory()) // check that the string is a directory.
		{
			if (DirectoryManagers == null) // if the map is empty, create one.
			{
				DirectoryManagers = new HashMap<File, Object>();
				return new DirectoryManager(aFile);
			}
			else
			{
				if (DirectoryManagers.containsKey(aFile)) // return an already created item, if it exists.
				{
					return (DirectoryManager) DirectoryManagers.get(tempDir);
				}
				else
				{
					return new DirectoryManager(aFile);
				}
			}
		}
		else
		{
			throw new InvalidPathException(aFile, "is not a valid directory");
		}
	}

	/**
	 * updates dir to a list of all files in the directory.
	 */
	public void listContents()
	{
		this.fileList = new ArrayList<String>(Arrays.asList(dir.list()));
	}

	/**
	 * Updates dir filtered list of files in the directory
	 * 
	 * @param extension a file extension not case sensitive ie ".ext"
	 */
	public void listContentsFiltered(String extension)
	{
		myFilenameFilter filter = new myFilenameFilter(extension);
		this.fileList = new ArrayList<String>(Arrays.asList(dir.list(filter)));

		if (fileList.size() == 0)
		{
			System.out.println("No files found with extension: " + extension);
		}
		else
		{
			System.out.println("Number of files found: " + fileList.size());
		}
	}

	/**
	 * An MD5 comparison of the files in this directory to those in the
	 * directory of the argument. The duplicate files are moved to "Duplicates"
	 * folder for review.
	 * 
	 * @param aDirectoryManager - A DirectoryManager for a valid directory.
	 */
	public void moveDuplicateFiles(DirectoryManager aDirectoryManager)
	{
		// Sets for iteration.
		List<String> comparedSet = aDirectoryManager.getFileList();
		List<File> duplicates = new ArrayList<File>();

		// Cycle through the passed directory files and get MD5s...
		for (Iterator<String> iterator = comparedSet.iterator(); iterator.hasNext();)
		{
			File existingFile = new File(aDirectoryManager.getDirectory().getAbsolutePath() + "\\" + iterator.next());
			String oFile = getMD5(existingFile).toString();

			// Cycle through this directory and get md5s...
			for (Iterator<String> it = fileList.iterator(); it.hasNext();)
			{
				File newFile = new File(this.dir.getAbsolutePath() + "\\" + it.next());
				String nFile = getMD5(newFile).toString();
				if (oFile.equals(nFile))
				{
					duplicates.add(newFile);
				}
			}
		}
		// Move the files to a duplicates directory
		for (Iterator<File> it = duplicates.iterator(); it.hasNext();)
		{
			File aFile = it.next();
			File dup = new File("Duplicates" + "\\" + aFile.getName());

			try
			{
				Files.move(aFile.toPath(), dup.toPath(), StandardCopyOption.ATOMIC_MOVE);
				System.out.println("Moved file: " + aFile.getAbsolutePath() + " -> " + dup.getAbsolutePath());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		// update the list
		listContents();
	}

	/**
	 * Removes files for the current directory older that were modified more
	 * than the specified date afterDays.
	 * 
	 * @param afterDays
	 */
	public void deleteFilesOlderThan(int afterDays)
	{

		int daysOld = afterDays;
		Calendar cal = Calendar.getInstance();
		Date date = cal.getTime();
		Date fileDate;

		// cycle through all files and get dates
		for (Iterator<String> it = fileList.iterator(); it.hasNext();)
		{
			File tmpFile = new File(this.dir.getAbsolutePath() + "\\" + it.next());
			fileDate = new Date(tmpFile.lastModified());
			// 1 day = 86400000 milliseconds
			int diff = (int) ((date.getTime() - fileDate.getTime()) / 86400000);

			if (diff > daysOld)
			{
				tmpFile.delete();
			}
		}
		// update the list again
		listContents();

	}

	/**
	 * Returns a copy of the list of files.
	 * 
	 * @return a copy ArrayList
	 */
	public List<String> getFileList()
	{

		return Collections.unmodifiableList(fileList);
	}

	/**
	 * Returns a File type reference to the directory
	 * 
	 * @return - a File which represents the root directory.
	 */
	public File getDirectory()
	{
		return this.dir;
	}

	/**
	 * Returns a string representation of the aFiles MD5
	 * 
	 * @param aFile - a valid file
	 * @return String
	 */
	private StringBuffer getMD5(File aFile)
	{
		byte[] dataBytes = new byte[1024];
		FileInputStream fis = null;
		int nread = 0;
		StringBuffer sb = new StringBuffer("");
		MessageDigest md;

		try
		{
			fis = new FileInputStream(aFile);
			md = MessageDigest.getInstance("MD5");

			while ((nread = fis.read(dataBytes)) != -1)
			{
				md.update(dataBytes, 0, nread);
			}

			byte[] md5Bytes = md.digest();

			for (int i = 0; i < md5Bytes.length; i++)
			{
				sb.append(Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
		}
		catch (NoSuchAlgorithmException | IOException e)
		{
			e.printStackTrace();
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
		return sb;
	}
}
