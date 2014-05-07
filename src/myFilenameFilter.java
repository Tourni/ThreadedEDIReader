import java.io.File;
import java.io.FilenameFilter;


public class myFilenameFilter implements FilenameFilter
{
	private String ext;
	
	public myFilenameFilter(String extension)
	{
		this.ext = extension;
	}

	@Override
	public boolean accept(File aDir, String fileName)
	{	
		boolean result = false;
		
		//ignore case
		if (fileName.endsWith(ext.toLowerCase()) || fileName.endsWith(ext.toUpperCase()) || fileName.endsWith(ext))
		{
			result = true;	
		}
		
		return result;
	}

}
