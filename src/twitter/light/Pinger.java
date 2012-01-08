/*
Project Name:		TwitterLight
Project Purpose:	Education, Distributed Software Development Course 
Project Supervisor: 	Sami Rollings, USF Professor 
Participants:		Ali Alnajjar USF MS Web Science Student
Contact:		najjaray@gmail.com
Requirement URL:	https://sites.google.com/site/usfcs682f10/assignments/proje
 */

package twitter.light;
import java.util.*;
import java.lang.Thread;
import java.lang.InterruptedException;
import java.io.*;
import java.net.*;
import java.util.concurrent.locks.*;


/**
 *
 * @author najjaray
 */
public class Pinger implements Runnable{
    public static HashMap inActiveNodes = new HashMap();
    public int inAcCode;
    public final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public final Lock r = rwl.readLock();
    public final Lock w = rwl.writeLock();
    // get the knowen version of a server to use it with pings
    public String GetSerVer(String server)
    {
        String ver = "";
        boolean tb;
        r.lock();try{tb = TwitterLight.DataServers.containsKey(server);}finally{r.unlock();}
        if (tb)
        {
        r.lock();try{ver = String.valueOf(TwitterLight.DataServers.get(server));}finally{r.unlock();}
        }
        else{ver ="0";}
        return ver;
    }
    // Get a list of keys for a giving hashmap
    public Set GetKeys(HashMap map)
    {
    Set results = new HashSet();
    Set KeyList = new HashSet();
    r.lock();try{KeyList = map.keySet();}finally{r.unlock();}
        for (Object s: KeyList)
        {
            results.add(s);
        }
    return results;
    }
    // send delete server request to other server
    public boolean DelServer(String type,String InAc_Server, String Ac_Server)
    {
        String Response ="";
        try
        {
        URL DataServerUrl = new URL("http://" + Ac_Server+"/admin?Del=" +type+ InAc_Server);
        HttpURLConnection SrvRequest = (HttpURLConnection) DataServerUrl.openConnection();
        SrvRequest.setRequestMethod("GET");
        SrvRequest.connect();

        BufferedReader in = new BufferedReader(new InputStreamReader(SrvRequest.getInputStream()));
        if (SrvRequest.getResponseCode() == 200)
            {
            while ((Response = in.readLine()) != null)
                {
                TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: " +Ac_Server +"del request response="+ Response);
                }
            }
        }
        catch(IOException ie)
        {
            //System.out.println(ApplicationName + ThreadName + "Error:" + ie.getMessage());

        }
    return true;
    }
    // Get inactive server status in active server
    public String GetServerV(String InAc_Server, String Ac_Server)
    {
        String Response = "";
        try
            {
            URL DataServerUrl = new URL("http://" + Ac_Server+"/admin?inAc=" + InAc_Server);
            HttpURLConnection SrvRequest = (HttpURLConnection) DataServerUrl.openConnection();
            SrvRequest.setRequestMethod("GET");
            SrvRequest.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(SrvRequest.getInputStream()));
            if (SrvRequest.getResponseCode() == 200)
                {
                while ((Response = in.readLine()) != null)
                    {
                    return Response;
                    }

                }
            }
            catch(IOException ie)
            {
                //System.out.println(ApplicationName + TwitterLight.ThreadName + "Error:" + ie.getMessage());
                
            }
        return Response;
    }
    // remove inactive server from the system
    public boolean RemoveNodeFromNet(String InActiceSr, String type)
    {
    TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: removing node[" + InActiceSr +"] ");
    boolean tb;
    if (type.equals("Data"))
    {
        Set set;
        r.lock();try{set = TwitterLight.DataServers.entrySet();}finally{r.unlock();}
        Iterator i1 = set.iterator();
        String ActiceSr;

        while (i1.hasNext())
        {
            Map.Entry me = (Map.Entry)i1.next();
            ActiceSr = String.valueOf(me.getKey());
            r.lock();try{tb = ! inActiveNodes.containsKey(ActiceSr) && !ActiceSr.equals(TwitterLight.myAddress+":"+TwitterLight.MyPortNo);}finally{r.unlock();}
            if (tb)
            {
            if (GetServerV(InActiceSr,ActiceSr).equals("Active"))
            {
                TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: ["+ActiceSr+ "] says [" +InActiceSr+ "] is Active");
                w.lock();try{inActiveNodes.put(InActiceSr, new Integer(5));}finally{w.unlock();}
                return false;
            }
            }
        }
        r.lock();try{set = TwitterLight.DataServers.entrySet();}finally{r.unlock();}
        Iterator i2 = set.iterator();
        while (i2.hasNext())
        {
            Map.Entry me = (Map.Entry)i2.next();
            ActiceSr = String.valueOf(me.getKey());
            DelServer("D",InActiceSr, ActiceSr);
            TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: deleting ["+InActiceSr+"] from [" +ActiceSr+"]");

        }
        r.lock();try{set = TwitterLight.WebServers.entrySet();}finally{r.unlock();}
        Iterator i3 = set.iterator();
        while (i3.hasNext())
        {
            Map.Entry me = (Map.Entry)i3.next();
            ActiceSr = String.valueOf(me.getKey());
            DelServer("D",InActiceSr, ActiceSr);
            TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: deleting ["+InActiceSr+"] from [" +ActiceSr+"]");
        }
        w.lock();try{
        TwitterLight.DataServers.remove(InActiceSr);
        inActiveNodes.remove(InActiceSr);
        }finally{w.unlock();}
        
        return true;
    }
    else
    {
        Set set;
        r.lock();try{ set = TwitterLight.WebServers.entrySet();} finally {r.unlock();}
        Iterator i4 = set.iterator();
        String ActiceSr;

        while (i4.hasNext())
        {
            Map.Entry me = (Map.Entry)i4.next();
            ActiceSr = String.valueOf(me.getKey());
            r.lock();try{tb = ! inActiveNodes.containsKey(ActiceSr) && !ActiceSr.equals(TwitterLight.myAddress+":"+TwitterLight.MyPortNo);} finally {r.unlock();}
            if (tb)
            {
            if (GetServerV(InActiceSr,ActiceSr).equals("Active"))
            {
                w.lock();try{inActiveNodes.put(InActiceSr, new Integer(5));}finally{w.unlock();}
                return false;
            }
            }
        }
        r.lock();try{set = TwitterLight.DataServers.entrySet();}finally{r.unlock();}
        Iterator i5 = set.iterator();
        while (i5.hasNext())
        {
            Map.Entry me = (Map.Entry)i5.next();
            //System.out.println(me.getKey().toString());
            ActiceSr = String.valueOf(me.getKey());
            DelServer("W",InActiceSr, ActiceSr);
            TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: deleting ["+InActiceSr+"] from [" +ActiceSr+"]");

        }
        set = TwitterLight.WebServers.entrySet();
        Iterator i6 = set.iterator();
        while (i6.hasNext())
        {
            Map.Entry me = (Map.Entry)i6.next();
            ActiceSr = String.valueOf(me.getKey());
            DelServer("W",InActiceSr, ActiceSr);
            TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: deleting ["+InActiceSr+"] from [" +ActiceSr+"]");

        }
        w.lock();try{
        TwitterLight.WebServers.remove(InActiceSr);
        inActiveNodes.remove(InActiceSr);
        }finally {w.unlock();}
        return true;
    }
    }
    // Get new data added to this erver after the giving version
   public String GetNewData(int ver)
    {
            String data ="";
            String line = "";

            String Fname = TwitterLight.myAddress + TwitterLight.MyPortNo +"-bkb.txt";
            try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(Fname));
             while ((line = bufferedReader.readLine()) != null)
              {
                if (data.length()>0)
                {
                data += "$";
                }
                if (Integer.parseInt(line.substring(line.indexOf("?")+1, line.indexOf("~"))) > ver)
                {
                    data += line.substring(line.indexOf("?")+1);
                }
              }
            bufferedReader.close();
            } catch (Exception x)
            {

            }
        if (data.length() == 0)
        {
            data ="*";
        }
        return data;
    }
   // push new data to the server
   public boolean PushData(String Address,int ver)
    {
       String dx;
       dx = GetNewData(ver);
       if (!dx.equals("*"))
       {
            try { URL DataServerUrl = new URL("http://" + Address+"/admin?push=" + URLEncoder.encode(dx));
            HttpURLConnection SrvRequest = (HttpURLConnection) DataServerUrl.openConnection();
            SrvRequest.setRequestMethod("GET");
            //SrvRequest.setRequestProperty("HTTP-Version", "HTTP/1.1");
            SrvRequest.connect();
            //SrvRequest.getResponseCode();
            //SrvRequest.getResponseMessage();
            String Response = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(SrvRequest.getInputStream()));
            if (SrvRequest.getResponseCode() == 200)
            {
                while ((Response = in.readLine()) != null)
                {
                if (Response.substring(0,2).equals("OK"))
                {
                    return true;
                }
                }
                }
        }
        catch(IOException ie)
        {
            return false;
        }
        }
       return false;
     }
    // ping other replicas
    public boolean ping(String Address, String Port)
    {
        boolean tb;
        //TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + " pingging " + Address+":"+Port);
        try { URL DataServerUrl = new URL("http://" + Address +":"+Port+"/admin?ping=" + TwitterLight.MyPortNo + "*" + GetSerVer(Address +":"+Port));
            HttpURLConnection SrvRequest = (HttpURLConnection) DataServerUrl.openConnection();
            SrvRequest.setRequestMethod("GET");
            SrvRequest.connect();
            String Response = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(SrvRequest.getInputStream()));
            if (SrvRequest.getResponseCode() == 200)
            {
                while ((Response = in.readLine()) != null)
                {
                if (Response.substring(0,2).equals("OK"))
                {
                    String[] tmp = Response.substring(3).split("&",2);
                    tmp[0] = tmp[0].substring(3);
                    if (! tmp[0].equals("*"))
                    {
                        if (tmp[0].contains(","))
                        {
                            String[] WServs = tmp[0].split(",");
                            for (int i=0;i<WServs.length;i++)
                            {
                                r.lock();try{tb = ! TwitterLight.WebServers.containsKey(WServs[i]);} finally{r.unlock();}
                                if (tb)
                                {
                                    if (WServs[i].length()>3)
                                    {
                                    w.lock();try{TwitterLight.WebServers.put(WServs[i], new Integer(9));}finally{w.unlock();}
                                    TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + "WS[" + WServs[i] + "] was added");
                                    }
                                }
                            }
                        }
                        else
                        {
                            r.lock();try{tb = !TwitterLight.WebServers.containsKey(tmp[0]);}finally{r.unlock();}
                            if (tb)
                            {
                               if (tmp[0].length()>3)
                               {
                                w.lock();try{TwitterLight.WebServers.put(tmp[0], new Integer(9));}finally{w.unlock();}
                                TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + "WS[" + tmp[0] + "] was added");
                               }
                            }
                        }
                    }
                    tmp[1] = tmp[1].substring(3);
                    if (! tmp[1].equals("*"))
                    {
                        if (tmp[1].contains(","))
                        {
                            String[] DServs = tmp[1].split(",");
                            for (int i=0;i<DServs.length;i++)
                            {
                                r.lock();try{tb = ! TwitterLight.DataServers.containsKey(DServs[i]);}finally{r.unlock();}
                                if (tb)
                                {
                                    if(DServs[i].length()>3)
                                    {
                                    w.lock();try{TwitterLight.DataServers.put(DServs[i], new Integer(9));}finally{w.unlock();}
                                    TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + "DS[" + DServs[i] + "] was added");
                                    }
                                }
                            }
                        }
                        else
                        {
                            r.lock();try{tb =!TwitterLight.DataServers.containsKey(tmp[1]);}finally{r.unlock();}
                            if (tb)
                            {
                                if (tmp[1].length()>3)
                                {
                                w.lock();try{TwitterLight.DataServers.put(tmp[1], new Integer(9));}finally{w.unlock();}
                                TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + "WS[" + tmp[1] + "] was added");
                                }
                            }
                        }
                    }
                    return true;
                }
                else
                {
                    return false;
                }
                }
            }
            else
            {
                return false;
            }
            }
        catch(IOException ie)
        {
            //System.out.println(ApplicationName + ThreadName + "Error:" + ie.getMessage());
            return false;
        }
        //System.out.println("Pingging " + Address +":" + Port);
        return false;
    }


    public void run()
    {
        int counter =0;
        boolean tb;
        boolean Re ;
        Re = true;
        boolean pigResult;
        try
        {Thread.sleep(5000);}
        catch(InterruptedException x){}
        while (TwitterLight.running)
        {
            try{Thread.sleep(1000);}catch(InterruptedException x){}
            counter ++;
            if (TwitterLight.ApplicationName.equals("[WebServer]") == true)
            {
                String[] tmp;
                Set KeyList;
                // loop to cover all web server
                KeyList = GetKeys(TwitterLight.WebServers);
                for (Object s: KeyList)
                {
                    if (!(TwitterLight.myAddress +":"+ TwitterLight.MyPortNo).equals(String.valueOf(s)))
                    {
                        tmp = String.valueOf(s).split(":",2);
                        pigResult = ping(tmp[0],tmp[1]);
                        TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + " pingging " + tmp[0]+":"+tmp[1] +" result = " +pigResult);
                        // if server didn't response
                        if (pigResult == false)
                            {
                                TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: handling un responsive nodeŸç");
                                r.lock();try{tb = inActiveNodes.containsKey(tmp[0]+":"+tmp[1]);}finally{r.unlock();}
                                // if server alredy exist in inactive nodes
                                if (tb)
                                {
                                    r.lock();try{inAcCode = Integer.parseInt(String.valueOf(inActiveNodes.get(tmp[0]+":"+tmp[1])));}finally{r.unlock();}
                                    TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: node [" + tmp[0]+":"+tmp[1] +"] in inactive nodes code =Ÿç" + inAcCode);
                                    // if zero completed 6 cycles without rsponse
                                    if (inAcCode <= 0)
                                    {
                                        TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: node [" + tmp[0]+":"+tmp[1] +"] to be deleted");
                                        // start the process of removing the server
                                        RemoveNodeFromNet(tmp[0]+":"+tmp[1], "Web");
                                    }
                                    else
                                    {
                                        // less than 6 cycles inactive ---> update cycle no
                                        TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: node [" + tmp[0]+":"+tmp[1] +"] ininactive nodes new code =Ÿç" + (inAcCode-1));
                                        w.lock();try{inActiveNodes.put(tmp[0]+":"+tmp[1], new Integer(inAcCode-1));}finally{w.unlock();}
                                    }
                                }
                                else
                                {
                                    // add to inactive list
                                    w.lock();try{inActiveNodes.put(tmp[0]+":"+tmp[1], new Integer(6));}finally{w.unlock();}
                                    TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: node [" + tmp[0]+":"+tmp[1] +"] added to inactive nodes");
                                }
                            }
                            else
                            {
                                // if response to ping remove from inactive list
                                r.lock();try{tb = inActiveNodes.containsKey(tmp[0]+":"+tmp[1]);}finally{r.unlock();}
                                if (tb)
                                    {
                                        w.lock();try{inActiveNodes.remove(tmp[0]+":"+tmp[1]);}finally{w.unlock();}
                                    }
                            }
                    }
                }
            }
             else
            {
            // handling data servers with the same algorithm
            String[] tmp;
            Set KeyList;
            //while (i8.hasNext())
            KeyList = GetKeys(TwitterLight.DataServers);
            for (Object s: KeyList)
            {
                if (!(TwitterLight.myAddress +":"+ TwitterLight.MyPortNo).equals(String.valueOf(s)))
                {
                    tmp = String.valueOf(s).split(":",2);
                   pigResult = ping(tmp[0],tmp[1]);
                    TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping:" + " pingging " + tmp[0]+":"+tmp[1] +" result = " +pigResult);
                    if (pigResult == false)
                        {
                            TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: handling unresponsive nodesŸç");
                            r.lock();try{tb = inActiveNodes.containsKey(tmp[0]+":"+tmp[1]);}finally{r.unlock();}
                            if (tb)
                            {
                                r.lock();try{inAcCode = Integer.parseInt(String.valueOf(inActiveNodes.get(tmp[0]+":"+tmp[1])));}finally{r.unlock();}
                                TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: node [" + tmp[0]+":"+tmp[1] +"] in inactive nodes code =Ÿç" + inAcCode);
                                if (inAcCode == 0)
                                {
                                    RemoveNodeFromNet(tmp[0]+":"+tmp[1], "Data");
                                    TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: node [" + tmp[0]+":"+tmp[1] +"] to be deleted");
                                }
                                else
                                {
                                    w.lock();try{inActiveNodes.put(tmp[0]+":"+tmp[1], new Integer(inAcCode -1));}finally{w.unlock();}
                                    TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: node [" + tmp[0]+":"+tmp[1] +"] ininactive nodes new code =Ÿç" + (inAcCode-1));
                                }
                            }
                            else
                            {
                                w.lock();try{inActiveNodes.put(tmp[0]+":"+tmp[1], new Integer(6));}finally{w.unlock();}
                                TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: node [" + tmp[0]+":"+tmp[1] +"] added to inactive nodes");
                            }
                        }
                        else
                        {
                            r.lock();try{tb = inActiveNodes.containsKey(tmp[0]+":"+tmp[1]) == false;}finally{r.unlock();}
                            if (tb)
                            {
                                w.lock();try{inActiveNodes.remove(tmp[0]+":"+tmp[1]);}finally{w.unlock();}
                            }
                        }
                    }
                }
            }
            // if complete 15 cycle
            if (counter == 15)
            {
                    counter = 0;
                // change the data server for web server
                if (TwitterLight.ApplicationName.equals("[WebServer]") == true)
                {
                    Random generator = new Random();
                    Object[] entries = TwitterLight.DataServers.entrySet().toArray();
                    String randomValue = String.valueOf(entries[generator.nextInt(entries.length)]);
                    randomValue = randomValue.substring(0,randomValue.indexOf("="));
                    String SerAdd = randomValue.substring(0, randomValue.indexOf(":"));
                    String SerPor = randomValue.substring(randomValue.indexOf(":")+1);
                    w.lock();
                    try{
                        TwitterLight.ServAddress = SerAdd;
                        TwitterLight.ServPort = SerPor;
                        }finally{w.unlock();}

                    TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: change data server to "+SerAdd +":"+ SerPor);
                }
                else
                {
                  // push data for other data servers
                String[] tmp;
                Set KeyList;
                boolean resu = false;
                KeyList = GetKeys(TwitterLight.DataToPush);
                for (Object s: KeyList)
                {
                    String Value;
                    r.lock();try{ Value = String.valueOf(TwitterLight.DataToPush.get(s));}finally{r.unlock();}
                    if (!Value.equals("*"))
                        {
                            TwitterLight.logQueue.addLast("-log, " +TwitterLight.ApplicationName + "Ping: "+String.valueOf(s)+"["+Value+"] needs some updates");
                            resu = PushData(String.valueOf(s),new Integer(Value));
                            if (resu)
                            {
                                w.lock();try{TwitterLight.DataToPush.put(s, "*");}finally{w.unlock();}
                            }
                        }
                }
            }

        }
    }

    }
}
