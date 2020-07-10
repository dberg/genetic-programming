set logscale xy
set xlabel "Generations"
set ylabel "Score - lower is better"

plot "output_sample" using 2:1 title "Genetic Programming" with boxes
