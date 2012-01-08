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
import java.net.*;
import java.util.concurrent.*;
import java.util.HashMap;
import java.util.*;
/**
 *
 * @author najjaray
 */
public class TwitterLight {
// initalize global vars 
public static HashMap DataStore;
public static int DataVersion = 10;
public static String MyPortNo = "8111";
public static String myAddress = "127.0.0.1";
public static String ServAddress = "127.0.0.1";
public static String ServPort = "8000";
public static boolean running = true;
public static LinkedList logQueue;
public static String ApplicationName = "N/A ";
public static ServerSocket serverSocket = null;
// ******
public static HashMap DataServers;
public static HashMap WebServers;
public static HashMap DataToPush;



    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        DataServers = new HashMap();
        WebServers = new HashMap();
        DataToPush = new HashMap();

         
        // Get command line parameters and make sure all required params are there
        if (args.length < 1)
        {
            System.out.println("      Error: invlid syntax");
            System.out.println("      java -jar project1-5.jar dataserver <DataServer Addree> <DataServer Port> <Entrty Point address(O)> <Entry point port(o)>");
            System.out.println("      java -jar project1-5.jar webserver <WebServer Address> <WebServer Port> <DataServer Address> <DataServer Port> <Entrty Point address(o)> <Entry point port(o)>");
        }
        else
        {
            if (args[0].toUpperCase().equals("WEBSERVER") || args[0].toUpperCase().equals("WEB"))
            {
                System.out.println("\n      Server type: web server");
                if (args.length < 5)
                    {
                        System.out.println("      - Error: missing parameters");
                        System.exit(1);
                    }
                else
                    {
                       System.out.println("       - Server Address:" + args[1]);
                       myAddress = args[1];
                       System.out.println("       - Server port: " + args[2]);
                       MyPortNo = args[2];
                       System.out.println("       - Data server address: " + args[3]);
                       ServAddress = args[3];
                       System.out.println("       - Data server port: " + args[4]);
                       ServPort = args[4];
                       ApplicationName = "[WebServer]";
                       DataServers.put(ServAddress+":"+ ServPort, new Integer(9));
                       WebServers.put(myAddress+":"+MyPortNo, new Integer(9));

                       if (args.length == 7)
                       {
                           WebServers.put(args[5]+ ":"+args[6],new Integer(9));

                       }
                    }
            }
            else
            {
                if (args[0].toUpperCase().equals("DATASERVER") || args[0].toUpperCase().equals("DATA"))
                {
                    System.out.println("\n      Server type: data server");
                    if (args.length<3)
                    {
                        System.out.println("      - Error: missing parameters");
                        System.exit(1);
                    }
                else
                    {
                       System.out.println("       - Server Address: " + args[1]);
                       myAddress = args[1];
                        System.out.println("       - Server port: " + args[2]);
                       MyPortNo = args[2];
                       ApplicationName = "[DataServer]";
                       DataServers.put(myAddress+":"+ MyPortNo, new Integer(9));
                       if (args.length == 5)
                       {
                           DataServers.put(args[3]+":"+ args[4], new Integer(9));
                       }
                    }
                }
                else
                {
                    System.out.println("      Invalid Server type (supported types: dataserver and webserver)");
                    System.exit(1);
                }
            }


        logQueue = new LinkedList();
        DataStore = new HashMap();
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        // run the logger thread
        logger AppLogger;
        AppLogger = new logger();
        Thread loggerThread;
        loggerThread = new Thread(AppLogger);
        loggerThread.start();

        // run the pinger thread
        Pinger SerPinger;
        SerPinger = new Pinger();
        Thread PingerThread;
        PingerThread = new Thread(SerPinger);
        PingerThread.start();

        // run the controller thread
        controller ctr;
        ctr = new controller();
        Thread ctrThread;
        ctrThread = new Thread(ctr);
        ctrThread.start();

        String ThreadName = "Main: ";
        int ThreadsCount = 20;
        logQueue.addLast("-log, " +ApplicationName + ThreadName + "Data Server started");
        
        // run the server 
        try
        {
            logQueue.addLast("-log, " +ApplicationName + ThreadName + "Opening Port " + MyPortNo);
            serverSocket = new ServerSocket(Integer.parseInt(MyPortNo));
        }
        catch (IOException e)
        {
            logQueue.addLast("-log, " +ApplicationName + ThreadName + "Could not listen on port:" + MyPortNo);
            System.exit(1);
        }
        logQueue.addLast("-log, " +ApplicationName + ThreadName + "Listening on port:" + MyPortNo);
        ExecutorService pool;
        pool = Executors.newFixedThreadPool(ThreadsCount);
        logQueue.addLast("-log, " +ApplicationName + ThreadName + "Threads Pool Created ("+ThreadsCount+") Threads");
        while (running)
        {
            try
            {
            logQueue.addLast("-log, " +ApplicationName + ThreadName + "Waiting for new requests");
            // get the request and handle it bashed on the server type (web or data) 
            if (TwitterLight.ApplicationName.equals("[WebServer]"))
            {
                pool.execute(new WebServer(serverSocket.accept(),ServAddress,ServPort));
            }
             else
            {
                pool.execute(new DataServer(serverSocket.accept()));
            }


            logQueue.addLast("-log, " +ApplicationName + ThreadName + "Request accepted and sent to Threads pool");
            }
            catch (IOException e)
            {
            logQueue.addLast("-log, " +ApplicationName + ThreadName + "connection closed.");
            System.exit(1);
            }
        }
        // cleanup the thread pool
        pool.shutdown();
    try
    {
      serverSocket.close();
      logQueue.addLast("-log, " +ApplicationName + ThreadName + "Server socket closed.");
    }
    catch (IOException e)
    {
    logQueue.addLast("-log, " +ApplicationName + ThreadName + "Can not close server socket.");
    System.exit(1);
    }
    pool.shutdown();
        logQueue.addLast("-log, " +ApplicationName + ThreadName + "Thread pool closed.");
    }
    }
}


