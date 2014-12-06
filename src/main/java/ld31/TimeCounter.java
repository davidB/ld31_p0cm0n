package ld31;
class TimeCounter {
	public float time = 0;
	enum Status {
		pending,
		running,
		stopped
	}
	Status status = Status.pending;

	void reset() {
		time = 0;
		status = Status.pending;
	}

	void start() {
		if (status == Status.pending) {
			status = Status.running;
		}
	}

	void stop() {
		status = Status.stopped;
	}

	boolean inc(float tpf) {
		long t0 = (long)Math.floor(time);
		boolean b = false;
		if (status == Status.running){
			time += (tpf * 2);
			b = (long)Math.floor(time) != t0;
		}
		return b;
	}
}