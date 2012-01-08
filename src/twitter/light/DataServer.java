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
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author najjaray
 */
public class DataServer implements Runnable{
    public Socket request;
    public String ThreadName = "N/A";
    public BufferedReader in;
    public PrintWriter out;
    public String[] http_Request;
    public final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public final Lock r = rwl.readLock();
    public final Lock w = rwl.writeLock();
    public boolean tb;
    //init
    public DataServer (Socket clientSocket)
    {
        this.request=clientSocket;
    }
    // update my this server with the information came from other replica
    public void UpdateDB(String data)
    {
        String[] updates;
        if (data.contains("$"))
        {
            updates = data.split("$");
        }
         else
        {
            updates = new String[1];
            updates[0] = data;
        }
        String no, add, vc, Hash,Body;
        for ( int i=0; i<updates.length;i++)
        {
            no = updates[i].substring(0,updates[i].indexOf("~"));
            add = updates[i].substring(updates[i].indexOf("~")+1, updates[i].indexOf("-"));
            vc = updates[i].substring(updates[i].indexOf("-")+1, updates[i].indexOf("#"));
            Hash = updates[i].substring(updates[i].indexOf("#")+1, updates[i].indexOf("%"));
            Body = updates[i].substring(updates[i].indexOf("%")+1);

            r.lock();
            boolean inDB;
            try { inDB = TwitterLight.DataStore.containsKey(Hash.toUpperCase());}
            finally {r.unlock();}
            if (! inDB)
            {
                w.lock();
                try { TwitterLight.DataStore.put(Hash.toUpperCase(),new ArrayList()); }
                finally {w.unlock();}
            }
            ArrayList tempArray;

            r.lock();
            try { tempArray = ArrayList.class.cast(TwitterLight.DataStore.get(Hash.toUpperCase())); }
            finally {r.unlock();}

            tempArray.add(GetDBVersion() +"&"+Body);
            w.lock();
            try
            {
            TwitterLight.DataStore.put(Hash.toUpperCase(), tempArray);
            TwitterLight.DataServers.put(add, no);
            }
            finally {w.unlock();}
        }
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "DATASTORE was updated successfully");

    }
    // init
    public DataServer ()
    {

    }
    // add server that pingged me to my replica list
    public void AddPinger(String key)
    {
        boolean tb;
        if (TwitterLight.ApplicationName.equals("[WebServer]"))
        {   r.lock();try{tb = !TwitterLight.WebServers.containsKey(key);} finally{r.unlock();}
            if (tb)
            {w.lock();try{
                TwitterLight.WebServers.put(key, new Integer(9));} finally {w.unlock();}
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Addig " + key +" to servers table");
            }
        }
         else
            {   r.lock();try{tb = !TwitterLight.DataServers.containsKey(key);}finally{r.unlock();}
                if (tb)
                {
                w.lock();try{
                TwitterLight.DataServers.put(key, new Integer(9));} finally{w.unlock();}
                }
            }
    }
    // Get the current vector clock
    public String GetDBVersion()
    {
    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Getting VC");
    String  VC = "[";
    Set set;
    r.lock();
    try{set = TwitterLight.DataServers.entrySet();}
    finally{r.unlock();}
    Iterator i = set.iterator();
    while(i.hasNext()) {
    Map.Entry me = (Map.Entry)i.next();
    if (VC.length()> 1)
    {
        VC += ",";
    }
    VC += me.getKey() + ";";
    VC += me.getValue() + "";
    }

    VC += "]";
    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + VC);
      return VC;
    }
    // get all the DB version withoit the [] <== dulicated of the above function.
    public String GetDBData()
    {
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Getting Data");
        String data;
        Iterator it;
        r.lock();try{
        Set set = TwitterLight.DataStore.entrySet();
        it = set.iterator();
        }finally{r.unlock();}
        data = "";
        while (it.hasNext())
            {
            r.lock();try{
            Map.Entry me = (Map.Entry)it.next();
            data += me.getKey() + ";" + me.getValue() ;} finally{r.unlock();}
            }
       TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Data=" + data);
        return data;
    }
    // update we devers list
    public void UpdateWebServers(String WS)
    {
    if (!WS.contains("*"))
      {
          if (WS.contains(","))
          {
              String[] WSL = WS.split(",");
              for (int i=0;i<WSL.length;i++)
              {
                  r.lock();try{tb =! TwitterLight.WebServers.containsKey(WSL[i]);}finally{r.unlock();}
                  if (tb)
                  {
                      w.lock();try{TwitterLight.WebServers.put(WSL[i], new Integer(9));}finally{w.unlock();}
                      TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + "WS[" + WSL[i] + "] was added");
                  }
              }
          }
          else
          {
              r.lock();try{tb = !TwitterLight.WebServers.containsKey(WS);}finally{r.unlock();}
              if (tb)
              {
                  w.lock();try{TwitterLight.WebServers.put(WS, new Integer(9));}finally{w.unlock();}
                  TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + "WS[" + WS + "] was added");
              }
          }
      }
    }
    // Get a list of active (response to pings) data server
    public String GetActiveDataServers()
    {
        boolean tb;
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Getting DataServers");
        String data;
        Set set;
        r.lock();try{ set = TwitterLight.DataServers.entrySet();}finally{r.unlock();}
        Iterator i = set.iterator();
        data = "";
        while (i.hasNext())
            {
            if (data.length() > 0)
            {
                data += ",";
            }
            Map.Entry me = (Map.Entry)i.next();
            r.lock();try{tb = Pinger.inActiveNodes.containsKey(me.getKey());
                }finally{r.unlock();}
            if (!tb)
            {
            data += me.getKey();
            }
            }
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "dataservers=" + data);
        if (data.length() == 0)
        {
            data = "*";
        }
        return data;
    }
    // Get a list of all data servers --- including in active
    public String GetDataServers()
    {
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Getting DataServers");
        String data;
        Set set;
        r.lock();try{ set = TwitterLight.DataServers.entrySet();}finally{r.unlock();}
        Iterator i = set.iterator();
        data = "";
        while (i.hasNext())
            {
            if (data.length() > 0)
            {
                data += ",";
            }
            Map.Entry me = (Map.Entry)i.next();
            data += me.getKey();
            }
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "dataservers=" + data);
        if (data.length() == 0)
        {
            data = "*";
        }
        return data;
    }
    // Get a list of active web servers
    public String GetActiveWebServers()
    {
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Getting DataServers");
        String data;
        Set set;
        r.lock();try{ set = TwitterLight.WebServers.entrySet();}finally{r.unlock();}
        Iterator i = set.iterator();
        data = "";
        while (i.hasNext())
            {
            if (data.length() > 0)
            {
                data += ",";
            }
            Map.Entry me = (Map.Entry)i.next();
            r.lock();try{tb = Pinger.inActiveNodes.containsKey(me.getKey());
            }finally{r.unlock();}
            if (!tb)
            {
            data += me.getKey();
            }
            }
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "webservers=" + data);
        if (data.length() == 0)
        {
            data = "*";
        }
        return data;
    }
    //Get a list of all web servers
    public String GetWebServers()
    {
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Getting webservers");

        String data;
        Set set;
        r.lock();try{set= TwitterLight.WebServers.entrySet();}finally{r.unlock();}
        Iterator i = set.iterator();
        data = "";
        while (i.hasNext())
            {
            if (data.length() > 0)
            {
                data += ",";
            }
            r.lock(); try{
            Map.Entry me = (Map.Entry)i.next();
            data += me.getKey();
            }finally{r.unlock();}
            }
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "webservers=" + data);
        if (data.length() == 0)
        {
            data = "*";
        }
        return data;
    }
    // check if vector clocks are equal
    public boolean VectorsAreEqual(String V1, String V2)
    {
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "are"+ V1 + "==" + V2);

        V1 = V1.substring(1, V1.length()-1);
        V2 = V2.substring(1, V2.length()-1);
        String[] A1 = V1.split(",");
        String[] A2 = V2.split(",");

        String[] tmp;
        HashMap H1 = new HashMap();
        HashMap H2 = new HashMap();
        for (int i =0; i< A1.length; i++)
        {
            tmp = A1[i].split(";",2);
            H1.put(tmp[0], tmp[1]);
        }
        for (int i =0; i< A2.length; i++)
        {
            tmp = A2[i].split(";",2);
            H2.put(tmp[0], tmp[1]);
        }


        Set set = H1.entrySet();
        Iterator i = set.iterator();
        while(i.hasNext())
        {
            Map.Entry me = (Map.Entry)i.next();
            if (H2.containsKey(me.getKey()))
            {
                if (! H2.get(me.getKey()).equals(me.getValue()))
                {
                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName +V1 + "==" + V2 +"=false");
                    return false;
                }
            }
            else
            {
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + V1 + "==" + V2 +"=false");
                return false;
            }
        }
        set = H2.entrySet();
        i = set.iterator();
        while(i.hasNext())
        {
            Map.Entry me = (Map.Entry)i.next();
            if (H1.containsKey(me.getKey()))
            {
                if (! H1.get(me.getKey()).equals(me.getValue()))
                {
                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + V1 + "==" + V2 +"=false");
                    return false;
                }
            }
            else
            {
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + V1 + "==" + V2 +"=false");
                return false;
            }
        }
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + V1 + "==" + V2 +"=true");
        return true;
    }
    //check if vector 2 in newwer than 1
    public boolean IsV2NewerthanV1(String V1, String V2)
    {
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "are"+ V1 + "<" + V2 +"");
        V1 = V1.substring(1, V1.length()-1);
        V2 = V2.substring(1, V2.length()-1);
        String[] A1 = V1.split(",");
        String[] A2 = V2.split(",");

        String[] tmp;
        HashMap H1 = new HashMap();
        HashMap H2 = new HashMap();
        for (int i =0; i< A1.length; i++)
        {
            tmp = A1[i].split(";",2);
            H1.put(tmp[0], tmp[1]);
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "H1.put("+tmp[0] + ","+tmp[1]+")");

        }
        for (int i =0; i< A2.length; i++)
        {
            tmp = A2[i].split(";",2);
            H2.put(tmp[0], tmp[1]);
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "H2.put("+tmp[0] + ","+tmp[1]+")");
        }
        Set set = H1.entrySet();
        Iterator i = set.iterator();
        while(i.hasNext())
        {
            Map.Entry me = (Map.Entry)i.next();
            String Currentkey = String.valueOf(me.getKey());
             TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "H2.contain("+Currentkey+")");
            if (H2.containsKey(Currentkey))
            {
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "H2("+Currentkey+")="+H2.get(Currentkey).toString()+"=="+me.getValue().toString());
                if (Integer.valueOf(H2.get(Currentkey).toString()) < Integer.valueOf(me.getValue().toString()))
                {
                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "are"+ V1 + "<" + V2 +"=false");
                    return false;
                }
            }
        }
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "are"+ V1 + "<" + V2 +"=true");
        return true;

    }
    // get data from DB to response to seach request
    public String GetDatafromDB(String request)
    {
        String VC = "";
        String Key = "";
        if (request.contains("&"))
        {
            Key = request.substring(0,request.indexOf("&"));
            VC = request.substring(request.indexOf("&")+1);
            if (! VC.substring(0, 3).equals("VC="))
            {
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Error: No VC with data request");
                return httpHeaderBuilder(400);
            }
            VC = VC.substring(3);
        
        if (! VectorsAreEqual(VC,GetDBVersion()))
        {
            int counter = 0;
            while (TwitterLight.running && IsV2NewerthanV1(GetDBVersion(),VC))
            {
                try{
                    Thread.sleep(500);
                    counter++;
                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "FE.VC=" + VC + " BE.VC="+GetDBVersion());
                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "FE have newer VC ... waiting for the updates");
                    if (counter >=20)
                    {
                       return httpHeaderBuilder(500); 
                    }

                    }
                catch(InterruptedException x)
                {
                }
            }
            }
        }
        
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + " Getting data from DB ("+ Key +")");
        String KeyData = "";
        r.lock();
        try {
         if (TwitterLight.DataStore.containsKey(Key.toUpperCase()))
         {
            ArrayList tempArray = ArrayList.class.cast(TwitterLight.DataStore.get(Key.toUpperCase()));
            for( int i=0;i<tempArray.size();i++)
            {KeyData += "<tweet>" + tempArray.get(i) + "</tweet>";}
         }
        else
         {
            KeyData = "<tweet></tweet>";
         }
     }
     finally {r.unlock();}
     TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "DatafromDB = " + GetDBVersion() +"*"+ KeyData);
     return httpHeaderBuilder(200)+GetDBVersion() +"^"+GetActiveWebServers()+"+"+ KeyData;
    }
    // add new tweets to data base
    public boolean AddDatatoDB(String tweetBody)
    {
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Adding Data to DB");
        String [] MsgBodyArr = tweetBody.split(" ");
        for(int i=0;i<MsgBodyArr.length;i++)
          {
              if (MsgBodyArr[i].charAt(0) == '#')
              {AddTweet(MsgBodyArr[i].substring(1).toUpperCase(), tweetBody);}
          }
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Data was addes to DB");
        return true;
    }
    // add tweet to DB ---> will be called by AddDatatoDB
    private void AddTweet(String Hash, String Body)
    {
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Adding tweet #"+Hash +"="+Body);
        r.lock();
        boolean inDB;
        try { inDB = TwitterLight.DataStore.containsKey(Hash.toUpperCase());}
        finally {r.unlock();}
        if (! inDB)
        {
            w.lock();
            try { TwitterLight.DataStore.put(Hash.toUpperCase(),new ArrayList()); }
            finally {w.unlock();}
        }
        ArrayList tempArray;

        r.lock();
        try { tempArray = ArrayList.class.cast(TwitterLight.DataStore.get(Hash.toUpperCase())); }
        finally {r.unlock();}
        TwitterLight.DataVersion++;
        tempArray.add(GetDBVersion() +"&"+Body);
        w.lock();
        try
        {
            TwitterLight.DataStore.put(Hash.toUpperCase(), tempArray);          
            TwitterLight.DataServers.put(TwitterLight.myAddress+":"+TwitterLight.MyPortNo, TwitterLight.DataVersion);
            TwitterLight.logQueue.addLast("-bkb,?" + TwitterLight.DataVersion +"~"+TwitterLight.myAddress + ":" + TwitterLight.MyPortNo+"-"+GetDBVersion() +"#"+ Hash.toUpperCase()+ "%" + Body);

        }
        finally {w.unlock();}
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Tweet was added VC was updated");
    }
    // build http header
    public String httpHeaderBuilder (int Code)
    {
        String httpHeader ="HTTP/1.0 ";
        switch (Code)
        {
          case 200: httpHeader = httpHeader + "200 OK"; break;
          case 204: httpHeader = httpHeader + "204 No Content"; break;
          case 400: httpHeader = httpHeader + "400 Bad Request"; break;
          case 404: httpHeader = httpHeader + "404 Not Found"; break;
          case 500: httpHeader = httpHeader + "500 Internal Server Error"; break;
          case 501: httpHeader = httpHeader + "501 Not Implemented"; break;
          case 505: httpHeader = httpHeader + "505 HTTP version not supported"; break;
        }
        if (Code == 200)
        {httpHeader = httpHeader + "\nContent-Type: text/plain\n\n";}

        return httpHeader;
    }
    // read http requests data
    public String[] httpParser(BufferedReader in)
    {
        String[] IncomeRequestArray;
        String[] http_Request;
        http_Request = new String[6];
        try
        {
            String IncomeRequest = in.readLine();
            TwitterLight.logQueue.addLast("-messages, " + TwitterLight.ApplicationName + ThreadName + "Handling " + IncomeRequest);
            IncomeRequestArray = IncomeRequest.split(" ", 3);
            // Getting http version
            http_Request[0] = IncomeRequestArray[2];
            // Getting http request type
            http_Request[1] = IncomeRequestArray[0];
            // Getting URL
            if (IncomeRequestArray[1].contains("/") && IncomeRequestArray[1].contains("?"))
                 {http_Request[2] = IncomeRequestArray[1].substring(IncomeRequestArray[1].indexOf("/"), IncomeRequestArray[1].indexOf("?"));}
            else {http_Request[2] = "N/A";}
            // Getting key name
            if (IncomeRequestArray[1].contains("?") && IncomeRequestArray[1].contains("="))
                 {http_Request[3] = IncomeRequestArray[1].substring(IncomeRequestArray[1].indexOf("?") + 1, IncomeRequestArray[1].indexOf("="));}
            else {http_Request[3] = "N/A";}
            // Getting key value
            if (IncomeRequestArray[1].contains("=") && IncomeRequestArray[1].length()>=(IncomeRequestArray[1].indexOf("=")+1))
                 {http_Request[4] = URLDecoder.decode(IncomeRequestArray[1].substring(IncomeRequestArray[1].indexOf("=") + 1));}
            else { http_Request[4]= "N/A";}
            http_Request[5] = IncomeRequest;
        }
        catch(IOException ie)
        {
        }
       return http_Request;
    }
    public void run()
    {
        ThreadName =  "T# " +String.valueOf(Thread.currentThread().getId()) + ": ";
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Handing new connection");
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Client Address:" + request.getInetAddress() + " Client Name:"+(request.getInetAddress()).getHostName());
        try
        {
        out = new PrintWriter(request.getOutputStream(),true);
        in = new BufferedReader(new InputStreamReader(request.getInputStream()));
        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Parsing http");
        http_Request = httpParser(in);
        // handling not supported HTTP version
        if (http_Request[0].equals("HTTP/1.1") == false)
        {
            out.write(httpHeaderBuilder(505));
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "HTTP version is not supported");
        }
        // handling supported HTTP version
        else
        {// not implemented request types
            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Http version is supported");
           if((http_Request[1].equals("GET") || http_Request[1].equals("POST")) == false)
               {
               out.write(httpHeaderBuilder(501));
               TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "Request type is not supported");
                }
           // handling supported requests
            else
               {// handling GET requets
                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "request type is supported");
                if (http_Request[1].equals("GET"))
                {// handling incorret URI
                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "request type = GET");
                    if (http_Request[2].equals("/search") == false && http_Request[2].equals("/admin") ==false)
                    {out.write(httpHeaderBuilder(404));
                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "incorred URI");}
                    // handling correst URI
                    else
                    {
                        if (http_Request[2].equals("/search"))
                       {
                        // handling missing args
                        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "corrent URI");
                        if (((http_Request[3].substring(0, 1).equals("v") || http_Request[3].equals("q")) == false) || http_Request[4].equals("N/A"))
                        {out.write(httpHeaderBuilder(400));
                        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "missing args");}
                         else
                         {// handling correctly formatedrequaest
                            // handling Data version requeats
                            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "complete request");
                            if (http_Request[3].equals("v"))
                            {
                                out.write(httpHeaderBuilder(200) + GetDBVersion());
                                TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "respond with version information");
                            }
                            else
                            {// handling data request
                               if (http_Request[3].equals("q"))
                                {
                                   if (http_Request[4].contains("*"))
                                   {
                                   UpdateWebServers(http_Request[4].substring(http_Request[4].indexOf("*")+1));
                                   out.write(GetDatafromDB(http_Request[4].substring(0,http_Request[4].indexOf("*"))));
                                    }
                                    else
                                    {
                                       out.write(GetDatafromDB(http_Request[4]));
                                    }
                                   TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "respond with data");}
                            }
                         }
                       }
                        else if (http_Request[2].equals("/admin"))
                        {
                            // get info from server ... useful for debugging to see reult in browser
                            if (http_Request[3].equals("info"))
                            {
                                if (http_Request[4].equals("dataservers"))
                                {
                                    r.lock();try{tb = TwitterLight.DataServers.isEmpty();}finally{r.unlock();}
                                    if (tb)
                                    {
                                        out.write(httpHeaderBuilder(200) + "not data servers");
                                    }
                                     else
                                    {
                                        out.write(httpHeaderBuilder(200));
                                        out.write(GetDataServers());
                                    }
                                }
                                else if(http_Request[4].equals("webservers"))
                                {
                                        out.write(httpHeaderBuilder(200));
                                        out.write(GetWebServers());
                                }
                                else if(http_Request[4].equals("vectorclock"))
                                {
                                    out.write(GetDBVersion());
                                }
                                else if(http_Request[4].equals("data"))
                                {
                                    out.write(this.GetDBVersion());
                                    out.write(GetDBData());
                                }

                            }
                            // pings from other replicas
                            else if (http_Request[3].equals("ping"))
                            {
                                if (http_Request[4].length()>0)
                                {
                                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "was pingged by " +request.getInetAddress().getHostAddress() + ":" + http_Request[4].substring(0,http_Request[4].indexOf("*")));
                                    // add pinger to my replica list
                                    AddPinger(String.valueOf(request.getInetAddress().getHostAddress() + ":" + http_Request[4].substring(0,http_Request[4].indexOf("*"))));
                                    //Add replica and the latest version number he know about me ... will be used to push data
                                    if (! TwitterLight.ApplicationName.equals("[WebServer]"))
                                    {
                                    w.lock();try{TwitterLight.DataToPush.put(request.getInetAddress().getHostAddress() + ":" + http_Request[4].substring(0,http_Request[4].indexOf("*")),http_Request[4].substring(http_Request[4].indexOf("*")+1));}finally{w.unlock();}
                                    }
                                    out.write(httpHeaderBuilder(200) + "OK&WS="+GetActiveWebServers()+"&DS="+GetActiveDataServers());
                                }
                                else
                                {
                                    out.write(httpHeaderBuilder(400));
                                }
                            }
                            // response to push from other replicas
                            else if(http_Request[3].equals("push") && http_Request[4].length() > 0)
                            {
                                UpdateDB(http_Request[4]);
                            }
                            // response to other repicas asking about inacrive server status
                            else if (http_Request[3].equals("inAc") && http_Request[4].length()>0)
                            {
                                boolean inList;
                                if (TwitterLight.ApplicationName.equals("[WebServer]"))
                                {
                                    r.lock();try{inList = TwitterLight.WebServers.containsKey(http_Request[4]);}finally{r.unlock();}
                                }
                                else
                                {
                                    r.lock();try{inList = TwitterLight.DataServers.containsKey(http_Request[4]);}finally{r.unlock();}
                                }
                                // check if the server in inactive servers list
                                r.lock();try{tb = Pinger.inActiveNodes.containsKey(http_Request[4]);}finally{r.unlock();}
                                if (tb)
                                {
                                    //response with the current version if exists inactive
                                    r.lock();try{out.write(httpHeaderBuilder(200) + Pinger.inActiveNodes.get(http_Request[4]));}finally{r.unlock();}
                                }
                                // check if the server still in list
                                else if (inList)
                                {
                                    out.write(httpHeaderBuilder(200) + "Active");
                                }
                                else
                                {
                                    out.write(httpHeaderBuilder(200) + "notExist");
                                }
                            }
                            // response to delete requests from other replicas
                            else if (http_Request[3].equals("del") && http_Request[4].length()>0)
                            {
                                r.lock();try{tb = http_Request[4].substring(0,1).equals("D") && Pinger.inActiveNodes.containsKey(http_Request[4].substring(1));}finally{r.unlock();}
                                if (tb)
                                {
                                    w.lock();try{Pinger.inActiveNodes.remove(http_Request[4].substring(1));}finally{w.unlock();}
                                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "["+http_Request[4].substring(1)+"] was deleted from inActiveNodes");
                                }
                                r.lock();try{tb = http_Request[4].substring(0,1).equals("D") && TwitterLight.DataServers.containsKey(http_Request[4].substring(1));}finally{r.unlock();}
                                if (tb)
                                {
                                    w.lock();try{TwitterLight.DataServers.remove(http_Request[4].substring(1));}finally{w.unlock();}
                                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "["+http_Request[4].substring(1)+"] was deleted from DataSer");
                                }
                                else
                                {
                                    r.lock();try{tb = TwitterLight.WebServers.containsKey(http_Request[4].substring(1));}finally{r.unlock();}
                                    if (tb)
                                    {
                                    w.lock();try{TwitterLight.WebServers.remove(http_Request[4].substring(1));}finally{w.unlock();}
                                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "["+http_Request[4].substring(1)+"] was deleted from WebSer");
                                    }
                                }
                                out.write(httpHeaderBuilder(200) + "deleted");
                            }
                            else
                            {
                                out.write(httpHeaderBuilder(400));
                            }
                        }
                    }
                }
                // handling POST requests
                else
                {
                    // handling incorrect URI
                    TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "request type = POST");
                    if (http_Request[2].equals("/status/update") == false)
                        {out.write(httpHeaderBuilder(404));
                        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "incorrect URI");}
                    else
                        {
                        // handling missing args
                        TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "correct URI");
                        if (http_Request[3].equals("status") == false || http_Request[4].equals("N/A"))
                            {out.write(httpHeaderBuilder(400));
                            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "missing args");}
                        else
                            {
                            if (AddDatatoDB(http_Request[4]) == true)
                            {
                                out.write(httpHeaderBuilder(204));
                            }
                            else
                            {
                                out.write(httpHeaderBuilder(500));
                            }

                            TwitterLight.logQueue.addLast("-log, " + TwitterLight.ApplicationName + ThreadName + "tweet was added");}
                        }
                }
               }
        }
        out.close();
        }
        catch (IOException e)
            {
            out.write(httpHeaderBuilder(500));
            System.exit(1);
            }
        out.close();
    }

}
