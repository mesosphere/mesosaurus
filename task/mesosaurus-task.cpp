#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <pthread.h>
#include <unistd.h>
#include <math.h>
#include <sys/time.h>
#include <random>
#include <vector>
#include <algorithm>

using namespace std;

const int work_loop = 100000;

void usage(char **argv) {
  fprintf(stderr,
      "Usage: %s <duration (ms)> <number of cores> <average load (0.0 - 1.0)>"
          " <average memory (megabytes)>\n", argv[0]);
  exit (EXIT_FAILURE);
}

struct work {
  work(int id, pthread_t* thread, float load, long mem, int duration, double fail_rate) :
      id(id), thread(thread), load(load), mem(mem), duration(duration), fail_rate(fail_rate) {
  }

  int id;
  pthread_t* thread;
  float load;
  long mem;
  int duration;
  double fail_rate;
};

long us_timestamp() {
  struct timeval val;
  gettimeofday(&val, NULL);
  return ((val.tv_sec * 1000000) + val.tv_usec);
}

void* workerEntry(void* payload) {
  assert(payload != 0);
  work* current_workload = (work*) payload;

  // Prevent the compiler from optimizing our artificial workload.
  volatile double val = 1.5;

  // To simulate a given percentage cpu load means to utilize
  // a sampled time interval with X percent computation.
  // In other words, for every piece of work done, 1 - X percent
  // should be yield / sleep time.

  // Compute end time.
  long endTime = us_timestamp() + (current_workload->duration * 1000);
  int bytesToAllocate = current_workload->mem;
  int allocated = 0;
  int chunkSize = 1024;
  int estimatedIterations = 1;

  random_device rd;
  normal_distribution<> rng (0.5,0.2);
  mt19937 rnd_gen( rd ());
  bool fail = rng(rnd_gen) > (current_workload->fail_rate);
  long temp = (endTime+us_timestamp())/2;
  normal_distribution<> norm(temp, (endTime-us_timestamp())/2);

  long failure_time = norm(rnd_gen);

  printf("Worker %d: allocate %d bytes over work iterations\n",
      current_workload->id, bytesToAllocate);

  for (int iteration = 0; us_timestamp() < endTime; iteration++) {
    long start = us_timestamp();
    if(fail && start > failure_time)
        {
            printf("worker died for the greater good\n");
            exit(EXIT_FAILURE);
        }
    // Work loop.
    for (int work_iteration = 0; work_iteration < work_loop; work_iteration++) {
      val = sqrt((4.2 + val) / val);
      val = val + work_iteration;
      val = val * val;
    }

    if ((bytesToAllocate > 0) && (chunkSize > 0)) {
      malloc(chunkSize);
      bytesToAllocate -= chunkSize;
      allocated += chunkSize;
    }

    long elapsed = us_timestamp() - start;
    long stale = ((elapsed / current_workload->load)
        * (1 - current_workload->load));
    usleep(stale);

    // Adjust estimated iterations left and chunk size.
    long timeLeft = endTime - us_timestamp();
    int iterations = max(timeLeft / elapsed, 1L);
    chunkSize = max(bytesToAllocate / iterations, 8);
  }

  printf("Worker %d: allocated %d bytes\n", current_workload->id, allocated);
  printf("Worker %d: exitting\n", current_workload->id);
  pthread_exit((void*) current_workload->thread);
  return NULL;
}

int main(int argc, char** argv) {
  if (argc < 5) {
    usage(argv);
  }

  int duration = atoi(argv[1]);
  if (duration <= 0) {
    fprintf(stderr, "Error: Duration must be a positive integer.\n");
    usage(argv);
  }

  int cores = atoi(argv[2]);
  if (cores <= 0) {
    fprintf(stderr, "Error: Cores must be a positive integer.\n");
    usage(argv);
  }

  float load;
  if (sscanf(argv[3], "%f", &load) <= 0) {
    fprintf(stderr, "Error: Load must be a floating point number.\n");
    usage(argv);
  }

  if ((load < 0) || (load > 1)) {
    fprintf(stderr, "Error: Load must be a floating point number"
        " between 0 and 1.\n");
    usage(argv);
  }

  long mem = atol(argv[4]);
  if (mem <= 0) {
    fprintf(stderr, "Error: Memory must be a positive integer.\n");
    usage(argv);
  }

  double fail_rate = atof(argv[5]);


  // Convert from megabytes to bytes.
  mem = mem * 1024 * 1024;

  vector<pthread_t*> threads;
  vector<work*> workloads;
  for (int workerId = 0; workerId < cores; workerId++) {
    pthread_t* thread = new pthread_t();
    threads.push_back(thread);
    work* current_workload = new work(workerId, thread, load, mem / cores,
        duration, fail_rate);

    int status = pthread_create(thread, NULL, workerEntry,
        (void*) current_workload);
    if (status) {
      fprintf(stderr, "Could not create worker thread %d: return code %d\n",
          workerId, status);
      exit (EXIT_FAILURE);
    }

  }

  // Join threads.
    for (int i= 0; i < threads.size(); i++) {
    pthread_t* thread = threads[i];
    int thread_status;
    int status;

    status = pthread_join(*thread, (void**) &status);
    if (status) {
      fprintf(stderr, "Could not join thread: return code %d\n", status);
      exit (EXIT_FAILURE);
    }

    delete thread;
  }

  pthread_exit (NULL);

  return EXIT_SUCCESS;
}
