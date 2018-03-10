/*
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rpc.turbo.util;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface SystemClock {

	/**
	 * 很快，但不是那么靠谱，要求高精度的地方不能用
	 * 
	 * @return
	 */
	static SystemClock fast() {
		return FastClock.clock;
	}

	/**
	 * 靠谱时间，要求高精度的地方用
	 * 
	 * @return
	 */
	static SystemClock realTime() {
		return RealTimeClock.clock;
	}

	/**
	 * 返回当前秒
	 * 
	 * @return
	 */
	long seconds();

	/**
	 * 返回当前毫秒
	 * 
	 * @return
	 */
	long mills();

	/**
	 * 返回当前微妙
	 * 
	 * @return
	 */
	long micros();
}

class RealTimeClock implements SystemClock {

	static final SystemClock clock = new RealTimeClock();

	@Override
	public long seconds() {
		return System.currentTimeMillis() / 1000;
	}

	@Override
	public long mills() {
		return System.currentTimeMillis();
	}

	@Override
	public long micros() {
		Instant instant = Instant.now();
		long micros = instant.getEpochSecond() * 1_000_000 + instant.getNano() / 1000;
		return micros;
	}

}

/**
 * {@link SystemClock} is a optimized substitute of
 * {@link System#currentTimeMillis()} for avoiding context switch overload.
 *
 * base on
 * <A>https://github.com/zhongl/jtoolkit/blob/master/common/src/main/java/com/github/zhongl/jtoolkit/SystemClock.java</A>
 */
class FastClock implements SystemClock {

	static final SystemClock clock = new FastClock(1);

	//删除掉无效的缓存行填充，应该使用继承的方式来引入缓存行。http://geek.csdn.net/news/detail/114619
	
	final long precision;
	volatile long mills;
	volatile long seconds;

	FastClock(long precision) {
		this.precision = precision;
		mills = System.currentTimeMillis();
		seconds = mills / 1000;
		scheduleClockUpdating();
	}

	private void scheduleClockUpdating() {
		ScheduledExecutorService scheduler = Executors//
				.newSingleThreadScheduledExecutor(runnable -> {
					Thread t = new Thread(runnable, "system.clock");
					t.setDaemon(true);
					return t;
				});

		scheduler.scheduleAtFixedRate(() -> {
			long currentTimeMillis = System.currentTimeMillis();
			mills = currentTimeMillis;

			if (seconds != currentTimeMillis / 1000) {
				seconds = currentTimeMillis / 1000;
			}
		}, precision, precision, TimeUnit.MILLISECONDS);
	}

	public long seconds() {
		return seconds;
	}

	public long mills() {
		return mills;
	}

	public long micros() {
		return mills * 1000;
	}

}
