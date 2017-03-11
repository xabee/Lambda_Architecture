package bdma.labos.lambda.exercises;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.mortbay.util.ajax.JSON;

import scala.Tuple2;
import twitter4j.JSONObject;
import bdma.labos.lambda.utils.Utils;
import bdma.labos.lambda.writers.WriterClient;
import bdma.labos.lambda.writers.WriterServer;

public class Exercise2 {

	@SuppressWarnings({ "serial", "deprecation" })
	public static void run(String twitterFile) throws Exception {
		WriterServer writerServer = new WriterServer();
		writerServer.start();
		
		SparkConf conf = new SparkConf().setAppName("LambdaArchitecture").setMaster("spark://master:7077");
		JavaSparkContext context = new JavaSparkContext(conf);
		JavaStreamingContext streamContext = new JavaStreamingContext(context, new Duration(1000));
		
		JavaPairReceiverInputDStream<String, String> kafkaStream = Utils.getKafkaStream(streamContext, twitterFile);
		
		kafkaStream.mapPartitions(new FlatMapFunction< Iterator<Tuple2<String, String> >, String >() {

			public Iterable<String> call(Iterator< Tuple2<String, String> > messages) throws Exception {
				WriterClient writer = new WriterClient();
				List<String> tweets = new ArrayList<String>();
				/*******************/
				// CODE HERE
				
				Configuration config = HBaseConfiguration.create();
			    Connection connection = ConnectionFactory.createConnection(config);

			    // save tweets
			    while (messages.hasNext())
			    {
			      Tuple2<String, String> object = (Tuple2)messages.next();
			      writer.write(((String)object._2()).getBytes());
			      tweets.add((String)object._2());
			    }

			    connection.close();
				
				/*******************/
			    
				writer.close();
				return tweets;
			}
			
		}).foreachRDD(new Function< JavaRDD<String>, Void >() {

			public Void call(JavaRDD<String> rdd) throws Exception {
				rdd.foreachPartition(new VoidFunction< Iterator<String> >() {

					public void call(Iterator<String> tweets) throws Exception {
						Configuration config = HBaseConfiguration.create();
						Connection connection = ConnectionFactory.createConnection(config);
						Table table = connection.getTable(TableName.valueOf("lambda"));
						/*************************/
						// CODE HERE
						
						int j;
					    int i = 0;
					    while (tweets.hasNext())
					    	// iterate over tweets
					    {
					      // get next tweet
					      String tweet = (String)tweets.next();
					      JSONObject json = new JSONObject(tweet);
					      
					      // split tweet text into words
					      String[] words = json.get("text").toString().split(" ");
					      
					      // get length of words string
					      String[] a = null;
					      a = words;
					      
					      // get first word
					      String word = a[0];
					      
					      if (word.startsWith("#"))
					    	  // if Hashtag
					      {
  
					        Put put = new Put(word.getBytes());
					        put.addColumn(
					          "tweets".getBytes(), 
					          json.get("id").toString().getBytes(), 
					          json.get("created").toString().getBytes());
					        
					        table.put(put);
					        System.out.println("----->> " + json.get("id").toString() + " " + word);
					  
					      }
					      i++;
					    }
						
						/************************/
						table.close();
						connection.close();
					}
					
				});
				return null;
			}
			
		});
		
		streamContext.start();
		streamContext.awaitTermination();
		
		writerServer.finish();
	}
	
}
