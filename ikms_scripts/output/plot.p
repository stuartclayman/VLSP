set term postscript eps color
set autoscale
unset log
unset label
set xtic auto
set xtics font 'Arial,18'
set ytics font 'Arial,18'
set xlabel font 'Arial,18'
set ylabel font 'Arial,18'
set title font 'Arial,18'
set key font 'Arial,16'
set ytic auto
set title 'Average Outgoing Throughput'
set xlabel 'Time'
set ylabel 'Average Outgoing Throughput'
set yrange [0:30]
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
plot 'ScenarioTest-30-c1/averageoutgoingthroughput.txt' using 1:2 title 'Placement EnergyLinear' with lines linestyle 1		, 'ScenarioTest-30-c1/averageoutgoingthroughput.txt' using 1:3 title 'Placement EnergyPaper' with lines linestyle 2
