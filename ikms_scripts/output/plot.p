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
plot 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:2 title 'Flows 1' with lines linestyle 1                , 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:3 title 'Flows 2' with lines linestyle 2                , 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:4 title 'Flows 3' with lines linestyle 3                , 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:5 title 'Flows 4' with lines linestyle 4                , 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:6 title 'Flows 5' with lines linestyle 5                , 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:7 title 'Flows 6' with lines linestyle 6                , 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:8 title 'Flows 7' with lines linestyle 7                , 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:9 title 'Flows 8' with lines linestyle 8                , 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:10 title 'Flows 9' with lines linestyle 9                , 'Scenario1PullFromEntity/freshnessmonitoredflows.txt' using 1:11 title 'Flows 10' with lines linestyle 10
