set term postscript eps color
set autoscale
unset log
unset label
set xtic auto
set ytic auto
set xtics font 'Arial,18'
set ytics font 'Arial,18'
set xlabel font 'Arial,18'
set ylabel font 'Arial,18'
set title font 'Arial,18'
set key font 'Arial,18'
set title 'Average Information Freshness (Selected Flows)'
set xlabel 'Time'
set ylabel 'Information Freshness'
set key top left
set style line 1 lt 1 lw 5 lc rgb 'black'
set style line 2 lt 1 lw 5 lc rgb 'gray'
set style line 3 lt 1 lw 5 lc rgb 'red'
set style line 4 lt 1 lw 5 lc rgb 'blue'
set style line 5 lt 1 lw 5 lc rgb 'yellow'
set style line 6 lt 1 lw 5 lc rgb 'green'
set style line 7 lt 3 lw 3 lc rgb 'black'
set style line 8 lt 6 lw 3 lc rgb 'black'
set style line 9 lt 1 lw 3 lc rgb 'black'
set style line 10 lt 2 lw 5 lc rgb 'black'
set style line 11 lt 3 lw 5 lc rgb 'black'
set style line 12 lt 6 lw 5 lc rgb 'black'
set style line 13 lt 2 lw 3 lc rgb 'black'
plot 'ScenarioEnergyPlacementDirectBusyVMsFutureLoad-30-c1/freshnessmonitoredflows.txt' using 1:2 title 'Placement EnergyLinear' with lines linestyle 1                , 'ScenarioEnergyPlacementDirectBusyVMsFutureLoad-30-c1/freshnessmonitoredflows.txt' using 1:3 title 'Placement EnergyPaper' with lines linestyle 2                , 'ScenarioEnergyPlacementDirectBusyVMsFutureLoad-30-c1/freshnessmonitoredflows.txt' using 1:4 title 'Placement EnergyQuadratic' with lines linestyle 3                , 'ScenarioEnergyPlacementDirectBusyVMsFutureLoad-30-c1/freshnessmonitoredflows.txt' using 1:5 title 'Placement EnergyTranscritical' with lines linestyle 4                , 'ScenarioEnergyPlacementDirectBusyVMsFutureLoad-30-c1/freshnessmonitoredflows.txt' using 1:6 title 'Placement EnergyPichfork' with lines linestyle 5                , 'ScenarioEnergyPlacementDirectBusyVMsFutureLoad-30-c1/freshnessmonitoredflows.txt' using 1:7 title 'Placement LeastBusy' with lines linestyle 6                , 'ScenarioEnergyPlacementDirectBusyVMsFutureLoad-30-c1/freshnessmonitoredflows.txt' using 1:8 title 'Placement LeastUsed' with lines linestyle 7
