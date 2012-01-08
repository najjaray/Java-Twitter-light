/*
Project Name:		TwitterLight
Project Purpose:	Education, Distributed Software Development Course 
Project Supervisor: 	Sami Rollings, USF Professor 
Participants:		Ali Alnajjar USF MS Web Science Student
Contact:		najjaray@gmail.com
Requirement URL:	https://sites.google.com/site/usfcs682f10/assignments/proje
 */

package twitter.light;
import java.net.*;
import java.io.*;
import java.io.IOException;
/**
 *
 * @author najjaray
 */
public class WebServer extends DataServer{
    public String DataServer;
    public String DataServerPort;
    public URL DataServerUrl;
    public HttpURLConnection SrvRequest;
    // init
    public WebServer (Socket clientSocket, String DBServAdd, String DBServPor)//(Socket clientSocket)
    {
        this.request = clientSocket;
        this.DataServer = DBServAdd;
        this.DataServerPort = DBServPor;
    }
    public WebServer (String DBServAdd, String DBServPor)//(Socket clientSocket)
    {
        this.DataServer = DBServAdd;
        this.DataServerPort = DBServPor;
    }
    // update my vector clock with the new one came from DataServer
    public void UpdateVC(String V)
    {
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Updateing VC = " + V);
        V = V.substring(1, V.length()-1);
        String[] A;
        if (V.contains(",") == true)
        {
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "VC contains many DServ");
            A = V.split(",");
        }
        else
        {
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "VC contains one DServ");
            A = new String[1];
            A[0] = V;
        }
        String[] tmp = new String[2];
        TwitterLight.DataServers.clear();
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "old VC Cleared");
        for (int i =0; i< A.length; i++)
        {
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + A[i]);
            tmp = A[i].split(";",2);
            TwitterLight.DataServers.put(tmp[0], new Integer(tmp[1]));
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "VC["+tmp[0]+"] = " + tmp[1]);
        }
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "VC updated");
    }
    // add new tweet to data base
    public boolean AddDatatoDB(String tweetBody)
    {
        try
        {

            DataServerUrl = new URL("http://" + DataServer +":"+DataServerPort +"/status/update?status="+URLEncoder.encode(tweetBody));
            SrvRequest = (HttpURLConnection) DataServerUrl.openConnection();
            SrvRequest.setRequestMethod("POST");
            SrvRequest.connect();
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Sending to data server \"" +tweetBody + "\"");
            if (SrvRequest.getResponseCode() == 204)
            {
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Tweet was added");
                return true;
            }
            else
            {
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Tweet wasn't added");
                return false;
            }
        }
        catch(IOException io)
        {
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Error: Exception in AddDatatoDB()");
            return false;
        }
    }
    // Get a tweet boday from cashed data
    public String GetTweets_fromCash(String SearchKey)
    {
        String Tweet = "";
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + " Getting data from cash");
        Tweet += "<tweets query=\"" +SearchKey.toUpperCase() + "\" cached=\"yes\">";
        r.lock();
        try{Tweet += String.valueOf(TwitterLight.DataStore.get(SearchKey.toUpperCase()));}finally{ r.unlock();}
        Tweet += "</tweets>";
        return httpHeaderBuilder(200)+Tweet;//+GetDBVersion() +"^"+GetActiveWebServers()+"+"+ Tweet;
    }
    // Get tweets from back end
    public String GetTweets_fromServer(String SearchKey)
    {
        String Tweet = "";
        boolean tb;
        try
        {
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Getting data from server");
            // get the data from data server
            r.lock();
                try { DataServerUrl = new URL("http://" + DataServer +":"+DataServerPort +"/search?q="+URLEncoder.encode(SearchKey.toUpperCase()) + "&VC=" + URLEncoder.encode(GetDBVersion()) + "*" + URLEncoder.encode(GetActiveWebServers())); }
            finally { r.unlock(); }
            SrvRequest = (HttpURLConnection) DataServerUrl.openConnection();
            SrvRequest.setRequestMethod("GET");
            //SrvRequest.setRequestProperty("HTTP-Version", "HTTP/1.1");
            SrvRequest.connect();
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Requesting data from server \"" +SearchKey.toUpperCase() + "\"");
            SrvRequest.getResponseCode();
            SrvRequest.getResponseMessage();
            String xmlbody = "";
            String xmlline = "";
            in = new BufferedReader(new InputStreamReader(SrvRequest.getInputStream()));
            String DV;
            if (SrvRequest.getResponseCode() == 200)
            {
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "got data from server successfully");
                xmlbody = "";
                while ((xmlline = in.readLine()) != null)
                {
                    xmlbody += xmlline;
                }
                DV = this.GetDBVersion();
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "comparing VCs ");
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Is the server newerVC = " + DV + "SerVC=" + xmlbody.substring(0,xmlbody.indexOf("^")));
                if (this.IsV2NewerthanV1(DV, xmlbody.substring(0,xmlbody.indexOf("^"))))
                {
                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Server have a newer version");
                    w.lock();
                    try { TwitterLight.DataStore.clear(); }
                    finally {w.unlock(); }
                }
                r.lock();
                try { this.UpdateVC(xmlbody.substring(0,xmlbody.indexOf("^"))); }
                finally { r.unlock(); }
                String WS = xmlbody.substring(xmlbody.indexOf("^")+1,xmlbody.indexOf("+"));
                if (! WS.equals("*"))
                {
                    if (WS.contains(","))
                    {
                        String[] WSL = WS.split(",");
                        for (int i=0; i<WSL.length;i++)
                        {
                            r.lock();try{tb = !TwitterLight.WebServers.containsKey(WSL[i]);}finally{r.unlock();}
                            if (tb)
                            {
                                w.lock();try{TwitterLight.WebServers.put(WSL[i], new Integer(9));}finally{w.unlock();}
                                TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + "WS[" + WSL[i] + "] was added");
                            }
                        }
                    }
                    else
                    {
                        r.lock();try{tb = ! TwitterLight.WebServers.containsKey(WS);}finally{r.unlock();}
                        if (tb)
                        {
                            w.lock();try{ TwitterLight.WebServers.put(WS, new Integer(9));}finally{w.unlock();}
                            TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + "WS[" + WS + "] was added");
                        }
                    }
                }
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "client clock updated");
                w.lock();try { TwitterLight.DataStore.put(SearchKey.toUpperCase(),xmlbody.substring(xmlbody.indexOf("+")+1)); }
                finally { w.unlock(); }
                Tweet += "<tweets query=\"" +SearchKey + "\" cached=\"no\">";
                Tweet += xmlbody.substring(xmlbody.indexOf("+")+1);
                Tweet += "</tweets>";
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "tweets="+Tweet);
                return httpHeaderBuilder(200) + Tweet;//+GetDBVersion() +"^"+GetActiveWebServers()+"+"+ Tweet;
            }
             else
            {
              TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Can not get tweets from sever");
              return httpHeaderBuilder(500) ;
             }
        }
        catch(IOException ie)
        {
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Error: Exception in Get Tweet from Server()");
            return httpHeaderBuilder(500);
        }
    }
    // get data from DB
     public String GetDatafromDB(String Key)
    {
        try{
        boolean inDB = false;
        r.lock();
        try { inDB = TwitterLight.DataStore.containsKey(Key.toUpperCase()); }
        finally { r.unlock(); }
        if(inDB)
        {
            String DV;
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName +Key + " was found in the cashed data");
            r.lock();
            try { DataServerUrl = new URL("http://" + DataServer +":"+DataServerPort+"/search?v="); }
            finally { r.unlock(); }
            SrvRequest = (HttpURLConnection) DataServerUrl.openConnection();
            SrvRequest.setRequestMethod("GET");
            //SrvRequest.setRequestProperty("HTTP-Version", "HTTP/1.1");
            SrvRequest.connect();
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Requesting version number from the server");
            //SrvRequest.getResponseCode();
            //SrvRequest.getResponseMessage();
            String Vnum = "";
            String Vpart = "";
            in = new BufferedReader(new InputStreamReader(SrvRequest.getInputStream()));
            if (SrvRequest.getResponseCode() == 200)
            {
                Vnum = "";
                while ((Vpart = in.readLine()) != null)
                {
                    Vnum += Vpart;
                }
            }
            DV = this.GetDBVersion();
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + " is ["+Vnum+"] == ["+DV+"]");
            if (this.VectorsAreEqual(Vnum, DV))
            {
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + " is ["+Vnum+"] == ["+DV+"] = True");
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + " getting data from cash");
                return GetTweets_fromCash(Key);
            }
             else
            {
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + " is ["+Vnum+"] == ["+DV+"] = False");
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + " getting data from data server");
                return GetTweets_fromServer(Key);
            }
        }
         else
        {
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName +Key + " was not found in the cashed data");
            return GetTweets_fromServer(Key);
        }
        }
        catch(IOException ie)
        {
            //System.out.println(ApplicationName + ThreadName + "Error:" + ie.getMessage());
            return httpHeaderBuilder(500);
        }
    }

}
