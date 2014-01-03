package ikms.operations;

// The Information Aggregation (IA) operation applies aggregation functions to the collected data / information. The aggregation 
// process increases the level of information abstraction, thereby transforming the data into a structured form, but at the same 
// time reducing the load on the network. 
// Aggregation works in situations where entities do not need a continuous stream of data from the IKMS, but can get by with an 
// approximation of the data values.  For example, getting an occasional measurement with the average of the volume of traffic on 
// a network link may be enough for some entities. 
// Some common aggregation functions include SUM, AVERAGE, STDDEV, MIN and MAX.  Although it is most common to use aggregation 
// functions such as the above, arbitrary functions can be passed in, which give considerable power and flexibility when determining 
// aggregations. For example: a customized function that is more complicated compared to the basic aggregation functions.

public class InformationAggregationOperation {

}
