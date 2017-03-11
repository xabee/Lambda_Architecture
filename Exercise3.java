package bdma.labos.lambda.exercises;

import java.util.Iterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.VoidFunction;
import twitter4j.JSONObject;

public class Exercise3
{
  private static String HDFS = "hdfs://master:27000/user/bdma08/lambda";
  
  public static void run()
    throws Exception
  {
    SparkConf conf = new SparkConf().setAppName("LambdaArchitecture");
    JavaSparkContext context = new JavaSparkContext(conf);
    
    Configuration config = new Configuration();
    FileSystem fileSystem = FileSystem.get(config);
    
    RemoteIterator<LocatedFileStatus> fileStatusListIterator = fileSystem.listFiles(new Path(HDFS), false);
    while (fileStatusListIterator.hasNext())
    {
      LocatedFileStatus fileStatus = (LocatedFileStatus)fileStatusListIterator.next();
      
      context.textFile(fileStatus.getPath().toString()).foreachPartition(
        new VoidFunction()
        {
          public void call(Iterator<String> tweets)
            throws Exception
          {
            Configuration config = HBaseConfiguration.create();
            Connection connection = ConnectionFactory.createConnection(config);
            Table table = connection.getTable(TableName.valueOf("lambda"));
            while (tweets.hasNext())
            {
              String tweet = (String)tweets.next();
              JSONObject json = new JSONObject(tweet);
              
              String[] words = json.get("text").toString().split(" ");
           // get length of words string
		      String[] a = null;
		      a = words;
              String word = a[0];
              if (word.startsWith("#"))
              {
                Put put = new Put(word.getBytes());
                put.addColumn(
                  "tweets".getBytes(), 
                  json.get("id").toString().getBytes(), 
                  json.get("created").toString().getBytes());
                
                table.put(put);
              }
            }
            table.close();
            connection.close();
          }

		public void call(Object arg0) throws Exception {
			// TODO Auto-generated method stub
			
		}

        });
    }
    context.close();
  }
}
