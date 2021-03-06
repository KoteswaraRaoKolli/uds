package procstats;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;



// Class that has nothing but a main.
// Does a Put, Get and a Scan against an hbase table.
public class GenAuctionHist {

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
    s.addColumn(Bytes.toBytes("m"), Bytes.toBytes("tshw"));
    s.addColumn(Bytes.toBytes("m"), Bytes.toBytes("tauctHist"));
    ResultScanner scanner = table.getScanner(s);
    try 
    {
        int rownum=0;
        HashMap<String,StatsCounter> overallAuctHist=new HashMap<String,StatsCounter>();
        ObjectMapper objectMapper = new ObjectMapper();

        //Target segments
        List<Integer>  tshwhr=new ArrayList<Integer>();
        tshwhr.add(1);
        tshwhr.add(3);
        tshwhr.add(5);
        tshwhr.add(24);
        tshwhr.add(48);
        tshwhr.add(30*24);

        for (Result r : scanner) 
        {
            //System.out.println("Row-->"+Bytes.toString(r.getRow()));
            //Iterate over all rows which have dco entries and generate shopping window ts 
            //generated
            if(r.containsColumn(Bytes.toBytes("r"),Bytes.toBytes("rtbjson")) && 
                    r.containsColumn(Bytes.toBytes("r"),Bytes.toBytes("minprocjson")) &&
                    r.containsColumn(Bytes.toBytes("m"),Bytes.toBytes("tshw")))
            {
                if(r.containsColumn(Bytes.toBytes("m"),Bytes.toBytes("tauctHist")))
                {
                    System.out.println("Skipping "+ Bytes.toString(r.getRow()));
                    rownum++;
                    continue;
                }
                byte [] rtbjson = r.getValue(Bytes.toBytes("r"),Bytes.toBytes("rtbjson"));
                byte [] minprocjson = r.getValue(Bytes.toBytes("r"),Bytes.toBytes("minprocjson"));
                byte [] shwjson = r.getValue(Bytes.toBytes("m"),Bytes.toBytes("tshw"));
                List<ShoppingWindow> shwl=ShoppingWindow.parseFromJson(shwjson);

                if(shwl!=null && shwl.size()>0 && rtbjson!=null && minprocjson!=null)
                {
                    System.out.println("Processing "+ Bytes.toString(r.getRow()));
                    //overallAuctHist=MinprocParser.getAuctionHistObject(minprocjson,rtbjson,tshwhr,shwl,overallAuctHist);
                    HashMap<String,StatsCounter> auctMap=MinprocParser.getAuctionHistObject(minprocjson,rtbjson,tshwhr,shwl);
                    String tauctHist=MinprocParser.getAuctHistAsJsonString(auctMap);

                    if(tauctHist!=null && tauctHist.length()>5) //Some  rudimentary checks on string length
                    {
                        //overallAuctHist=MinprocParser.mergeStatsCounter(auctMap,overallAuctHist);
                        Put p = new Put(r.getRow());
                        p.add(Bytes.toBytes("m"), Bytes.toBytes("tauctHist"),Bytes.toBytes(tauctHist));
                        table.put(p);
                        rownum++;
                        System.out.println(Bytes.toString(r.getRow())+"\t"+tauctHist);
                        if(rownum%1000==0)
                        {
                            //System.out.println("overalluid\t"+rownum+"\t"+MinprocParser.getAuctHistAsJsonString(overallAuctHist));
                            System.out.println("overalluid\t"+rownum);
                            //give hints to do GC for every 1000 reads
                            System.gc();
                        }
                    }else
                    {
                        System.out.println(Bytes.toString(r.getRow())+" tauctHist is empty");
                    }
                }else
                {
                    System.out.println(Bytes.toString(r.getRow())+" rtbjson or minprocjson or shwl is null or empty ");
                }
            }
        }
        System.out.println("Generated "+rownum+" AuctHist");
        /*
        if(overallAuctHist!=null)
        {
            String overallRow=new String("alluid");
            Put p = new Put(Bytes.toBytes(overallRow));
            String tauctHist=MinprocParser.getAuctHistAsJsonString(overallAuctHist);
            p.add(Bytes.toBytes("m"), Bytes.toBytes("tauctHist"),Bytes.toBytes(tauctHist));
            table.put(p);
            System.out.println(overallRow+"\t"+tauctHist);
        }
        */
    }finally 
    {
      // Make sure you close your scanners when you are done!
      // Thats why we have it inside a try/finally clause
      scanner.close();
    }
  }
}
