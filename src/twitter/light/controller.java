/*
Project Name:		TwitterLight
Project Purpose:	Education, Distributed Software Development Course 
Project Supervisor: 	Sami Rollings, USF Professor 
Participants:		Ali Alnajjar USF MS Web Science Student
Contact:		najjaray@gmail.com
Requirement URL:	https://sites.google.com/site/usfcs682f10/assignments/proje
 */

package twitter.light;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.*;

/**
 *
 * @author najjaray
 */


// this thread will be use to allow / handle user commands
public class controller implements Runnable{
        public final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public final Lock r = rwl.readLock();
    public final Lock w = rwl.writeLock();

    public void run(){
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        int counter = 0;
        while(TwitterLight.running)
        {
            try
            {

                System.out.println(TwitterLight.ApplicationName + "Enter exit to shut down:");
                userInput = stdIn.readLine();
                
                // if the user enter EXIT in command line ==> shut down the system
                if (userInput.toUpperCase().equals("EXIT"))
                {
                    TwitterLight.running = false;
                    TwitterLight.serverSocket.close();
                }
            }
            catch (IOException io)
            {

        }
    }

}
}
