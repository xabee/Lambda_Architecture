package bdma.labos.lambda.exercises;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import scala.Tuple2;
import twitter4j.JSONObject;
import bdma.labos.lambda.utils.Utils;

public class Exercise1 {

	@SuppressWarnings("serial")
	public static void run(String twitterFile) throws Exception {
		SparkConf conf = new SparkConf().setAppName("LambdaArchitecture").setMaster("spark://master:7077");
		JavaSparkContext context = new JavaSparkContext(conf);
		JavaStreamingContext streamContext = new JavaStreamingContext(context, new Duration(1000));
		
		JavaPairReceiverInputDStream<String, String> kafkaStream = Utils.getKafkaStream(streamContext, twitterFile);
		
	JavaDStream<String> json = kafkaStream.map(
			    new Function<Tuple2<String, String>, String>() {
			        public String call(Tuple2<String, String> m) {
		            System.out.println(m._2());
		            return m._2;
			        }
			    });
		
        //kafkaStream.print();
        json.print();
				
		streamContext.start();
		streamContext.awaitTermination();
	}
	
}
