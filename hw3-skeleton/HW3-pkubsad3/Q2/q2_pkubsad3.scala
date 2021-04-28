// Databricks notebook source
// STARTER CODE - DO NOT EDIT THIS CELL
import org.apache.spark.sql.functions.desc
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import spark.implicits._
import org.apache.spark.sql.expressions.Window

// COMMAND ----------

// STARTER CODE - DO NOT EDIT THIS CELL
val customSchema = StructType(Array(StructField("lpep_pickup_datetime", StringType, true), StructField("lpep_dropoff_datetime", StringType, true), StructField("PULocationID", IntegerType, true), StructField("DOLocationID", IntegerType, true), StructField("passenger_count", IntegerType, true), StructField("trip_distance", FloatType, true), StructField("fare_amount", FloatType, true), StructField("payment_type", IntegerType, true)))

// COMMAND ----------

// STARTER CODE - YOU CAN LOAD ANY FILE WITH A SIMILAR SYNTAX.
val df = spark.read
   .format("com.databricks.spark.csv")
   .option("header", "true") // Use first line of all files as header
   .option("nullValue", "null")
   .schema(customSchema)
   .load("/FileStore/tables/nyc_tripdata-1.csv") // the csv file which you want to work with
   .withColumn("pickup_datetime", from_unixtime(unix_timestamp(col("lpep_pickup_datetime"), "MM/dd/yyyy HH:mm")))
   .withColumn("dropoff_datetime", from_unixtime(unix_timestamp(col("lpep_dropoff_datetime"), "MM/dd/yyyy HH:mm")))
   .drop($"lpep_pickup_datetime")
   .drop($"lpep_dropoff_datetime")

// COMMAND ----------

// LOAD THE "taxi_zone_lookup.csv" FILE SIMILARLY AS ABOVE. CAST ANY COLUMN TO APPROPRIATE DATA TYPE IF NECESSARY.
// STARTER CODE - YOU CAN LOAD ANY FILE WITH A SIMILAR SYNTAX.
val customSchema2 = StructType(Array(StructField("LocationID", IntegerType, true), StructField("Borough", StringType, true), StructField("Zone", StringType, true), StructField("service_zone", StringType, true)))

val df_2 = spark.read
   .format("com.databricks.spark.csv")
   .option("header", "true") // Use first line of all files as header
   .option("nullValue", "null")
   .schema(customSchema2)
   .load("/FileStore/tables/taxi_zone_lookup-1.csv") // the csv file which you want to work with
// ENTER THE CODE BELOW

// COMMAND ----------

// STARTER CODE - DO NOT EDIT THIS CELL
// Some commands that you can use to see your dataframes and results of the operations. You can comment the df.show(5) and uncomment display(df) to see the data differently. You will find these two functions useful in reporting your results.
// display(df)
df.show(5) // view the first 5 rows of the dataframe

// COMMAND ----------

// STARTER CODE - DO NOT EDIT THIS CELL
// Filter the data to only keep the rows where "PULocationID" and the "DOLocationID" are different and the "trip_distance" is strictly greater than 2.0 (>2.0).

// VERY VERY IMPORTANT: ALL THE SUBSEQUENT OPERATIONS MUST BE PERFORMED ON THIS FILTERED DATA

val df_filter = df.filter($"PULocationID" =!= $"DOLocationID" && $"trip_distance" > 2.0)
df_filter.show(5)

// COMMAND ----------

// PART 1a: The top-5 most popular drop locations - "DOLocationID", sorted in descending order - if there is a tie, then one with lower "DOLocationID" gets listed first
// Output Schema: DOLocationID int, number_of_dropoffs int 

// Hint: Checkout the groupBy(), orderBy() and count() functions.
// ENTER THE CODE BELOW
var df_drop_off=df_filter.groupBy("DOLocationID").agg(count("*").as("number_of_dropoffs")).orderBy(col("number_of_dropoffs").desc,col("DOLocationID"))
df_drop_off.limit(5).show()


// COMMAND ----------

// PART 1b: The top-5 most popular pickup locations - "PULocationID", sorted in descending order - if there is a tie, then one with lower "PULocationID" gets listed first 
// Output Schema: PULocationID int, number_of_pickups int

// Hint: Code is very similar to part 1a above.

// ENTER THE CODE BELOW
var df_pick_up=df_filter.groupBy("PULocationID").agg(count("*").as("number_of_dropoffs")).orderBy(col("number_of_dropoffs").desc,col("PULocationID"))
df_pick_up.limit(5).show()

// COMMAND ----------

// PART 2: List the top-3 locations with the maximum overall activity, i.e. sum of all pickups and all dropoffs at that LocationID. In case of a tie, the lower LocationID gets listed first.
// Output Schema: LocationID int, number_activities int

// Hint: In order to get the result, you may need to perform a join operation between the two dataframes that you created in earlier parts (to come up with the sum of the number of pickups and dropoffs on each location). 

// ENTER THE CODE BELOW
// val df_temp=df_filter.groupBy(col("PULocationID").as("LocationID")).agg(count("*").as("number_activities"))
// df_pick_up_renamed=
val df_appended=df_pick_up.as("pick_up").join(df_drop_off.as("drop_off"),$"pick_up.PULocationID"===$"drop_off.DOLocationID","outer").withColumn("LocationID",coalesce(col("PULocationID"),col("DOLocationID"))).withColumn("number_activities",(coalesce($"pick_up.number_of_dropoffs", lit(0)) + coalesce($"drop_off.number_of_dropoffs", lit(0)))).select("LocationID","number_activities")
display(df_appended.orderBy(col("number_activities").desc,col("LocationId")).limit(3))

// COMMAND ----------

// PART 3: List all the boroughs in the order of having the highest to lowest number of activities (i.e. sum of all pickups and all dropoffs at that LocationID), along with the total number of activity counts for each borough in NYC during that entire period of time.
// Output Schema: Borough string, total_number_activities int

// Hint: You can use the dataframe obtained from the previous part, and will need to do the join with the 'taxi_zone_lookup' dataframe. Also, checkout the "agg" function applied to a grouped dataframe.

// ENTER THE CODE BELOW
val df_joined=df_appended.as("df_number_activities").join(df_2.as("df_2"),$"df_number_activities.LocationId"===$"df_2.LocationID","outer")
 .groupBy("df_2.Borough").agg(sum("df_number_activities.number_activities").as("total_number_activities")).orderBy(col("total_number_activities").desc,col("Borough"))
display(df_joined)


// COMMAND ----------

// PART 4: List the top 2 days of week with the largest number of (daily) average pickups, along with the values of average number of pickups on each of the two days. The day of week should be a string with its full name, for example, "Monday" - not a number 1 or "Mon" instead.
// Output Schema: day_of_week string, avg_count float

// Hint: You may need to group by the "date" (without time stamp - time in the day) first. Checkout "to_date" function.
// var df_temp=df_filter.union(df_filter.select(date_format(to_date(col("pickup_datetime")),"EEEE").as("day")))


// ENTER THE CODE BELOW
var df_temp=df_filter.withColumn("date",to_date(col("pickup_datetime"))).groupBy("date").agg(count("*").as("count"))
var df_temp2=df_temp.withColumn("day_of_week",date_format(to_date(col("date")),"EEEE")).groupBy(col("day_of_week").as("day_of_week")).agg(avg(col("count")).as("avg_count")).orderBy(col("avg_count").desc).limit(2)
display(df_temp2)


// COMMAND ----------

// PART 5: For each particular hour of a day (0 to 23, 0 being midnight) - in their order from 0 to 23, find the zone in Brooklyn borough with the LARGEST number of pickups. 
// Output Schema: hour_of_day int, zone string, max_count int

// Hint: You may need to use "Window" over hour of day, along with "group by" to find the MAXIMUM count of pickups

// ENTER THE CODE BELOW
val winSpec = Window.partitionBy("hour_of_day").orderBy(col("count").desc)
// Src: https://stackoverflow.com/questions/39417209/conditional-join-in-spark-dataframe
val joinCondition = when($"df_filter.PULocationId"===$"df_2.LocationID",$"df_2.Borough"==="Brooklyn")
// .groupBy(col("hour").as("hour_of_day")).agg(count("*"))
val df_joined=df_filter.as("df_filter").join(df_2.as("df_2"),joinCondition).withColumn("hour",hour(col("pickup_datetime"))).groupBy(col("hour").as("hour_of_day"),col("zone")).agg(count(col("*")).as("count")).withColumn("rank",rank().over(winSpec))
display(df_joined.filter(col("rank")===1).withColumnRenamed("count","max_count").drop(col("rank")).orderBy("hour_of_day"))


// COMMAND ----------

// PART 6 - Find which 3 different days of the January, in Manhattan, saw the largest percentage increment in pickups compared to previous day, in the order from largest increment % to smallest increment %. 
// Print the day of month along with the percent CHANGE (can be negative), rounded to 2 decimal places, in number of pickups compared to previous day.
// Output Schema: day int, percent_change float


// Hint: You might need to use lag function, over a window ordered by day of month.

// ENTER THE CODE BELOW
val winSpec = Window.orderBy(col("day"))
// Src: https://stackoverflow.com/questions/39417209/conditional-join-in-spark-dataframe
val joinCondition = when($"df_filter.PULocationId"===$"df_2.LocationID",$"df_2.Borough"==="Manhattan")
val df_joined=df_filter.filter(month(col("pickup_datetime"))===1).as("df_filter").join(df_2.as("df_2"),joinCondition).withColumn("pickup_day",dayofmonth(col("pickup_datetime")))
.groupBy(col("pickup_day").as("day")).agg(count(col("*")).as("count"))
.withColumn("lag",lag(col("count"),1).over(winSpec))
.withColumn("diff",col("count")-col("lag"))
.withColumn("percent_change",round(((col("diff")/col("lag"))*100),2))
.orderBy((col("percent_change")).desc)
.select("day","percent_change")
.limit(3)
display(df_joined)
