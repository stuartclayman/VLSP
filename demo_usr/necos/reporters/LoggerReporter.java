package demo_usr.necos.reporters.vlsp;

import eu.reservoir.monitoring.core.Reporter;
//import eu.reservoir.monitoring.core.AbstractReporter;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.table.Table;
import eu.reservoir.monitoring.core.table.TableHeader;
import eu.reservoir.monitoring.core.table.TableRow;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A PrintReporter just prints a Measurement.
 */
public final class LoggerReporter implements Reporter { // AWAITING LATEST LATTICE extends AbstractReporter {
    /**
     * In this LoggerReporter, report() logs and formats the Measurement (from VLSP) to the log file.
     */
    
    private static Logger LOGGER = LoggerFactory.getLogger(LoggerReporter.class);
    
    
    public LoggerReporter(String reporterName) {
        //AWAITING LATEST LATTICE  super(reporterName); 
    }
    
    
    @Override
    public void report(Measurement m) {
	//LOGGER.info(m.toString());
        
        List<ProbeValue> values = m.getValues();
        
        // we get the first Probe value containing the whole table
        ProbeValue tableValue = values.get(0);
        
        // we get the whole table and the table header
        Table table = (Table)tableValue.getValue();
        TableHeader columnDefinitions = table.getColumnDefinitions();

        int rowsNumber = table.getRowCount();
        int columnsNumber = table.getColumnCount();
        TableRow row;
        StringBuilder lcs = new StringBuilder();
        
        for (int i=0; i < rowsNumber; i++) {
            row = table.getRow(0);
            
            int j=0;
            lcs.append("Host ID: ");
            lcs.append(row.get(j).getValue());
            
            for (j=1; j < columnsNumber-1; j++) {
                lcs.append(" ");
                lcs.append(columnDefinitions.get(j).getName());
                lcs.append(": ");
                lcs.append(row.get(j).getValue());
            }
            
            // prnnt routers table now
            //
            
            Table routersTable = (Table)row.get(j).getValue();
            columnDefinitions = table.getColumnDefinitions();
            
            rowsNumber = routersTable.getRowCount();
            columnsNumber = routersTable.getColumnCount();
            
            LOGGER.info("---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            LOGGER.info(lcs.toString());
            
            StringBuilder router = new StringBuilder();
            
            for (i=0; i < rowsNumber; i++) {
                row = routersTable.getRow(i);
                
                for (j=0; j < columnsNumber; j++) {
                    router.append("\t");
                    router.append(columnDefinitions.get(j).getName());
                    router.append(": ");
                    router.append(row.get(j).getValue());
                }
                
            LOGGER.info(router.toString());    
            router.setLength(0);
            }
        
        LOGGER.info("---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
        }        
    }
}
