/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo_usr.necos.probes.vlsp;

//import eu.fivegex.monitoring.appl.probes.docker.*;
import eu.reservoir.monitoring.appl.datarate.EveryNSeconds;
import eu.reservoir.monitoring.core.*;
import eu.reservoir.monitoring.core.table.DefaultTable;
import eu.reservoir.monitoring.core.table.DefaultTableHeader;
import eu.reservoir.monitoring.core.table.DefaultTableRow;
import eu.reservoir.monitoring.core.table.DefaultTableValue;
import eu.reservoir.monitoring.core.table.Table;
import eu.reservoir.monitoring.core.table.TableException;
import eu.reservoir.monitoring.core.table.TableHeader;
import eu.reservoir.monitoring.core.table.TableProbeAttribute;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

/**
 *
 * @author uceeftu
 */
public class VLSPProbe extends AbstractProbe implements Probe {
    VLSPDataCollector vlspDc;
    
    TableHeader localControllers;
    TableHeader routers;
    
    private Logger LOGGER = LoggerFactory.getLogger(VLSPProbe.class);
    
    
    public VLSPProbe(String VLSPHost, String VLSPPort, String probeName)  {
        setName(probeName);
        setDataRate(new EveryNSeconds(10));
        
        try {
            vlspDc = new VLSPDataCollector(VLSPHost, Integer.valueOf(VLSPPort));
        } catch (UnknownHostException e) {
            LOGGER.error("Error while resolving VLSP GC host: " + e.getMessage());
        }
        
        localControllers = new DefaultTableHeader()
                                .add("id", ProbeAttributeType.STRING)
                                .add("cpuLoad", ProbeAttributeType.FLOAT)
                                .add("cpuIdle", ProbeAttributeType.FLOAT)
                                .add("usedMemory", ProbeAttributeType.FLOAT)
                                .add("freeMemory", ProbeAttributeType.FLOAT)
                                .add("energyTot", ProbeAttributeType.FLOAT)
                                .add("energyDelta", ProbeAttributeType.FLOAT)
                                .add("energyNow", ProbeAttributeType.FLOAT)
                                .add("routers", ProbeAttributeType.TABLE);
        
        routers = new DefaultTableHeader()
                                .add("name", ProbeAttributeType.STRING)
                                .add("elapsed", ProbeAttributeType.FLOAT)
                                .add("cpu", ProbeAttributeType.FLOAT)
                                .add("user", ProbeAttributeType.FLOAT)
                                .add("sys", ProbeAttributeType.FLOAT)
                                .add("mem", ProbeAttributeType.FLOAT)
                                .add("energyConsumption", ProbeAttributeType.FLOAT)
                                .add("energyDelta", ProbeAttributeType.FLOAT);
        
        addProbeAttribute(new TableProbeAttribute(0, "hosts", localControllers));
    }
    

    @Override
    public void beginThreadBody() {

    }
    
    @Override
    public ProbeMeasurement collect() {
        try {
            JSONObject values = vlspDc.collectValues();
            
            Table localControllersTable = new DefaultTable();
            localControllersTable.defineTable(localControllers);
            
            Table routersTable = new DefaultTable();
            routersTable.defineTable(routers);
            
            JSONArray localControllersJArray = values.getJSONArray("localcontrollers");
            
            for (int i=0; i < localControllersJArray.length(); i++) {
                JSONObject localController = localControllersJArray.getJSONObject(i);
                
                JSONArray routersJArray = localController.getJSONArray("routers");
                for (int j=0; j < routersJArray.length(); j++) {
                    JSONObject router = routersJArray.getJSONObject(j);
                    
                    routersTable.addRow(new DefaultTableRow()
                                    .add(new DefaultTableValue(router.getString("name")))
                                    .add(new DefaultTableValue((float)router.getDouble("elapsed")))
                                    .add(new DefaultTableValue((float)router.getDouble("cpu")))
                                    .add(new DefaultTableValue((float)router.getDouble("user")))
                                    .add(new DefaultTableValue((float)router.getDouble("sys")))
                                    .add(new DefaultTableValue((float)router.getDouble("mem")))
                                    .add(new DefaultTableValue((float)router.getDouble("energyConsumption")))
                                    .add(new DefaultTableValue((float)router.getDouble("energyDelta")))
                                    );
                }                        
                        
                localControllersTable.addRow(new DefaultTableRow()
                                             .add(new DefaultTableValue(localController.getString("id")))
                                             .add(new DefaultTableValue((float)localController.getDouble("cpuLoad")))
                                             .add(new DefaultTableValue((float)localController.getDouble("cpuIdle")))
                                             .add(new DefaultTableValue((float)localController.getDouble("usedMemory")))
                                             .add(new DefaultTableValue((float)localController.getDouble("freeMemory")))
                                             .add(new DefaultTableValue((float)localController.getDouble("energyTot")))
                                             .add(new DefaultTableValue((float)localController.getDouble("energyDelta")))
                                             .add(new DefaultTableValue((float)localController.getDouble("energyNow")))
                                             .add((new DefaultTableValue(routersTable)))
                                        );        
                        
                }
                    
            
            ArrayList<ProbeValue> list = new ArrayList<ProbeValue>(1);
            
            list.add(new DefaultProbeValue(0, localControllersTable));

            ProbeMeasurement m = new ProducerMeasurement(this, list, "Slice");
            
            return m;
        }
        catch (IOException ioe) {
            LOGGER.error("Error while contacting VLSP GC: " + ioe.getMessage());
        } catch (JSONException je) {
            LOGGER.error("Error while parsing VLSP GC response: " + je.getMessage());
        } catch (TypeException te) {
            LOGGER.error("Error while adding probe attribute: " + te.getMessage());
        } catch (TableException tbe) {
            LOGGER.error("Error while adding probe attribute: " + tbe.getMessage());
        }
        
    return null;
    }  
    
    
    public static void main(String[] args) {
        VLSPProbe p = new VLSPProbe("localhost", "8888", "testProbe");
        p.activateProbe();
        p.collect();
    }
    
    
}
