package procstats;

import java.io.IOException;
import java.util.HashMap;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import java.io.*;


// Class that has nothing but a main.
// Does a Put, Get and a Scan against an hbase table.
public class GenBidLS {

  public static void main(String[] args) throws IOException 
  {
    // You need a configuration object to tell the client where to connect.
    // When you create a HBaseConfiguration, it reads in whatever you've set
    // into your hbase-site.xml and in hbase-default.xml, as long as these can
    // be found on the CLASSPATH
    org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
    HTable table = new HTable(config, "user");
    //Get overall stats object if it exists
    /*
    Get g = new Get(Bytes.toBytes(overallRow));
    Result r = table.get(g);
    */

    Scan s = new Scan();
    s.addColumn(Bytes.toBytes("r"), Bytes.toBytes("rtbjson"));
    s.addColumn(Bytes.toBytes("r"), Bytes.toBytes("minprocjson"));
    s.addColumn(Bytes.toBytes("m"), Bytes.toBytes("bidls"));
    ResultScanner scanner = table.getScanner(s);
    try 
    {
        HashMap<String,StatsCounter> overallBidLs=null;
        for (Result r : scanner) 
        {
            //System.out.println("Row-->"+Bytes.toString(r.getRow()));
            //Iterate over all rows which have these two log entries for which bidls has not been 
            //generated
            if(r.containsColumn(Bytes.toBytes("r"),Bytes.toBytes("minprocjson")) &&
                r.containsColumn(Bytes.toBytes("r"),Bytes.toBytes("rtbjson")) &&
                !r.containsColumn(Bytes.toBytes("m"),Bytes.toBytes("bidls")))
            {
                byte [] minprocJson = r.getValue(Bytes.toBytes("r"),Bytes.toBytes("minprocjson"));
                byte [] rtbJson = r.getValue(Bytes.toBytes("r"),Bytes.toBytes("rtbjson"));
                if(rtbJson!=null && minprocJson!=null)
                {
                    //overallBidLs=MinprocParser.getBidLandScapeObject(minprocJson,rtbJson,overallBidLs);
                    String bidLs=MinprocParser.getBidLandScapeAsJsonString(minprocJson,rtbJson);
                    if(bidLs!=null && !bidLs.isEmpty())
                    {
                        Put p = new Put(r.getRow());
                        p.add(Bytes.toBytes("m"), Bytes.toBytes("bidls"),Bytes.toBytes(bidLs));
                        table.put(p);
                    }else
                    {
                        System.out.println(Bytes.toString(r.getRow())+" bidls is empty");
                    }
                    System.out.println(Bytes.toString(r.getRow()));
                }else
                {
                    System.out.println(Bytes.toString(r.getRow())+" rtbJson or minprocJson is null");
                }
            }
        }
        if(overallBidLs!=null)
        {
            String overallRow=new String("alluid");
            Put p = new Put(Bytes.toBytes(overallRow));
            p.add(Bytes.toBytes("m"), Bytes.toBytes("bidls"),
                Bytes.toBytes(MinprocParser.getBidLandScapeAsJsonString(overallBidLs)));
            table.put(p);
        }
    }finally 
    {
      // Make sure you close your scanners when you are done!
      // Thats why we have it inside a try/finally clause
      scanner.close();
    }
  }
}
