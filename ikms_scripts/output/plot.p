set term postscript eps color
set autoscale
unset log
unset label
set xtic auto
set ytic auto
set xtics font 'Arial,16'
set ytics font 'Arial,16'
set xlabel font 'Arial,16'
set ylabel font 'Arial,16'
set title font 'Arial,16'
set key font 'Arial,16'
set title 'Average Response Time'
set xlabel 'Time'
set ylabel 'Response Time'
set yrange [0:200]
set key top left
set style line 1 dt 1 lt 1 lw 5 lc rgb 'black'
set style line 2 dt 1 lt 1 lw 5 lc rgb 'gray'
set style line 3 dt 2 lt 1 lw 5 lc rgb 'black'
set style line 4 dt 3 lt 1 lw 5 lc rgb 'black'
set style line 5 dt 4 lt 1 lw 5 lc rgb 'black'
set style line 6 dt 2 lt 1 lw 5 lc rgb 'gray'
set style line 7 lt 3 lw 3 lc rgb 'black'
set style line 8 lt 6 lw 3 lc rgb 'black'
set style line 9 lt 1 lw 3 lc rgb 'black'
set style line 10 lt 2 lw 5 lc rgb 'black'
set style line 11 lt 3 lw 5 lc rgb 'black'
set style line 12 lt 6 lw 5 lc rgb 'black'
set style line 13 lt 2 lw 3 lc rgb 'black'
plot 'Scenario3/scalability/responsetime-scalability-pullfromstoragegov-300.txt' using 1:2 title 'Flows 1' with lines linestyle 1                , 'Scenario3/scalability/responsetime-scalability-pullfromstoragegov-300.txt' using 1:3 title 'Flows 2' with lines linestyle 2                , 'Scenario3/scalability/responsetime-scalability-pullfromstoragegov-300.txt' using 1:4 title 'Flows 5' with lines linestyle 3        	, 'Scenario3/scalability/responsetime-scalability-pullfromstoragegov-300.txt' using 1:5 title 'Flows 10' with lines linestyle 4
