set terminal png
set output 'results/timeline.png'
set ytics scale 0,0 format ""
set xlabel "Elapsed time (ms)"
set yrange [0:8000]
set boxwidth 2 absolute
plot 'results/arrival.dat' using 2:1 title 'Arrived', 'results/launched.dat' using 2:1 title 'Launched', 'results/finished.dat' using 2:1 title 'Finished', 'results/waittime.dat' using 1:2 title 'Wait time' with boxes, 'results/makespan.dat' using 1:2 title 'Makespan' with boxes
