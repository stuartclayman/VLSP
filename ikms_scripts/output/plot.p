set term postscript eps enhanced
set autoscale
unset log
unset label
set xtic auto
set ytic auto
set title 'Average Information Freshness (Selected Flows)'
set xlabel 'Time'
set ylabel 'Information Freshness'
set key top right
set style line 1 lt 1 lw 1 lc rgb 'black'
set style line 2 lt 2 lw 1 lc rgb 'black'
set style line 3 lt 3 lw 1 lc rgb 'black'
set style line 4 lt 6 lw 1 lc rgb 'black'
set style line 5 lt 1 lw 5 lc rgb 'black'
set style line 6 lt 1 lw 5 lc rgb 'gray'
set style line 7 lt 3 lw 3 lc rgb 'black'
set style line 8 lt 6 lw 3 lc rgb 'black'
set style line 9 lt 1 lw 3 lc rgb 'black'
set style line 10 lt 2 lw 5 lc rgb 'black'
set style line 11 lt 3 lw 5 lc rgb 'black'
set style line 12 lt 6 lw 5 lc rgb 'black'
set style line 13 lt 2 lw 3 lc rgb 'black'
plot 'ScenarioFlowsPullFromEntity-100R/freshnessmonitoredflows.txt' using 1:2 title 'Flows 10' with lines linestyle 1                , 'ScenarioFlowsPullFromEntity-100R/freshnessmonitoredflows.txt' using 1:3 title 'Flows 20' with lines linestyle 2                , 'ScenarioFlowsPullFromEntity-100R/freshnessmonitoredflows.txt' using 1:4 title 'Flows 30' with lines linestyle 3        	, 'ScenarioFlowsPullFromEntity-100R/freshnessmonitoredflows.txt' using 1:5 title 'Flows 40' with lines linestyle 4
