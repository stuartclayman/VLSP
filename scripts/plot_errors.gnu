set term postscript color eps 24
set ylabel "Estimated traffic/node"
set xlabel "Number of nodes"
set key bottom right
set logscale x
plot  "pressure_PA.post" using ($6):($12/$6):(($12-$13)/$6):(($12+$13)/$6) w yerrorbars ls 1 title "Pressure", \
"pressure_PA.post" using ($6):($12/$6) w l ls 1 notitle, \
   "hotspot_PA.post" using ($6):($12/$6):(($12-$13)/$6):(($12+$13)/$6) w yerrorbars ls 2 title "HotSpot", \
   "hotspot_PA.post" using ($6):($12/$6) w l ls 2 notitle, \
   "random_PA.post" using ($6):($12/$6):(($12-$13)/$6):(($12+$13)/$6) w yerrorbars ls 3 title "Random", \
   "random_PA.post" using ($6):($12/$6) w l ls 3 notitle
