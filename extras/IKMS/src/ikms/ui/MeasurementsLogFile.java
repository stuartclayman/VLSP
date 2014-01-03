package ikms.ui;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class MeasurementsLogFile {

	PrintWriter out = null; 
	
	/**
	 * @param args
	 */
	public MeasurementsLogFile (String fileName) {
		try {
			out = new PrintWriter(new FileWriter(fileName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void Log (String line) {
		out.println(line);
		out.flush();
	}
	
	public void CloseFile () {
		out.close();
	}

}
