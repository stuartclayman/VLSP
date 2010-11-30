set term postscript eps color enhanced "NimbusSanL-Regu,20" fontfile "/usr/share/texmf-texlive/fonts/type1/urw/helvetic/uhvr8a.pfb"
FILE = "out"
LW = 2
PS = 2
set logscale
set key bottom left 
set xlabel "Survival time s (seconds)"
set ylabel "P(s>X)"
plot FILE using ($1):($2) w l lw LW  title "Estimated dist" , \
  FILE  using ($1):($3) w l lw LW title "Tail fit dist" , \
  FILE  using ($1):($4) w l lw LW title "Actual dist" 

