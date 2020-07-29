set logscale z

set grid
set hidden3d

splot "output_sample_eval" using 1:2:3 title "Hidden function" with points, \
      "output_sample_eval" using 1:2:4 title "Learned function" with points
