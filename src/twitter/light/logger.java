/*
Project Name:		TwitterLight
Project Purpose:	Education, Distributed Software Development Course 
Project Supervisor: 	Sami Rollings, USF Professor 
Participants:		Ali Alnajjar USF MS Web Science Student
Contact:		najjaray@gmail.com
Requirement URL:	https://sites.google.com/site/usfcs682f10/assignments/proje
 */

package twitter.light;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;
import java.util.*;
/**
 *
 * @author najjaray
 */
public class logger implements Runnable{
     private String getDateTime(String format) {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        return dateFormat.format(date);
    }
     
    // this thread will be ussed to log what ever in the log queue into log file 
    public void run ()
    {
        Writer output = null;
        String[] tmp;
        String[] has;
        String Fname = TwitterLight.ApplicationName.substring(TwitterLight.ApplicationName.indexOf("[")+1, TwitterLight.ApplicationName.indexOf("]")) + getDateTime("yyyyMMddHHmmss") + "log.txt";
        while (TwitterLight.running)
        {
            while (TwitterLight.logQueue.size()>1)
            {
                try
                {
                    tmp=(TwitterLight.logQueue.pop()).toString().split(",", 2);
                    Fname=TwitterLight.myAddress + TwitterLight.MyPortNo + tmp[0] + ".txt";
                    FileWriter fstream = new FileWriter(Fname,true);
                    BufferedWriter out = new BufferedWriter(fstream);
                    if ( tmp[1].startsWith("*") == false)
                    {
                    out.write(getDateTime("yyyy/MM/dd HH:mm:ss") + "     " + tmp[1] + "\n");
                    }
                    else
                    {
                        out.write(tmp[1].substring(1) + "\n");
                    }
                    out.close();
                }
                catch (IOException x)
                {
                    TwitterLight.logQueue.addLast(TwitterLight.ApplicationName +"Logger: Can not add log to file");
                }
            }
            try
            {Thread.sleep(100);}
            catch (Exception e)
            {}
        }
    }

}
