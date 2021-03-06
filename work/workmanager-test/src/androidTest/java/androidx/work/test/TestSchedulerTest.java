/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.work.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkStatus;
import androidx.work.test.workers.CountingTestWorker;
import androidx.work.test.workers.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TestSchedulerTest {

    private TestDriver mTestDriver;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        WorkManagerTestInitHelper.initializeTestWorkManager(context);
        mTestDriver = WorkManagerTestInitHelper.getTestDriver();
        CountingTestWorker.COUNT.set(0);
    }

    @Test
    public void testWorker_shouldSucceedSynchronously() {
        WorkRequest request = createWorkRequest();
        WorkManager workManager = WorkManager.getInstance();
        workManager.synchronous().enqueueSync(request);
        WorkStatus status = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(status.getState().isFinished(), is(true));
    }

    @Test
    public void testWorker_withDependentWork_shouldSucceedSynchronously() {
        OneTimeWorkRequest request = createWorkRequest();
        OneTimeWorkRequest dependentRequest = createWorkRequest();
        WorkManager workManager = WorkManager.getInstance();
        WorkContinuation continuation = workManager.beginWith(request)
                .then(dependentRequest);
        continuation.synchronous().enqueueSync();
        WorkStatus requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        WorkStatus dependentStatus = workManager
                .synchronous()
                .getStatusByIdSync(dependentRequest.getId());

        assertThat(requestStatus.getState().isFinished(), is(true));
        assertThat(dependentStatus.getState().isFinished(), is(true));
    }

    @Test
    public void testWorker_withConstraints_shouldNoOp() {
        OneTimeWorkRequest request = createWorkRequestWithNetworkConstraints();
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueue(request);
        WorkStatus requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(requestStatus.getState().isFinished(), is(false));
    }

    @Test
    public void testWorker_withConstraints_shouldSucceedAfterSetConstraints() {
        OneTimeWorkRequest request = createWorkRequestWithNetworkConstraints();
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueue(request);
        WorkStatus requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(requestStatus.getState().isFinished(), is(false));
        mTestDriver.setAllConstraintsMet(request.getId());
        requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(requestStatus.getState().isFinished(), is(true));
    }

    @Test
    public void testWorker_withInitialDelay_shouldNoOp() {
        OneTimeWorkRequest request = createWorkRequestWithInitialDelay();
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueue(request);
        WorkStatus requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(requestStatus.getState().isFinished(), is(false));
    }

    @Test
    public void testWorker_withInitialDelay_shouldSucceedAfterSetInitialDelay() {
        OneTimeWorkRequest request = createWorkRequestWithInitialDelay();
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueue(request);
        mTestDriver.setInitialDelayMet(request.getId());
        WorkStatus requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
        assertThat(requestStatus.getState().isFinished(), is(true));
    }

    @Test
    public void testWorker_withPeriodDelay_shouldRun() {
        PeriodicWorkRequest request = createWorkRequestWithPeriodDelay();
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueue(request);
        assertThat(CountingTestWorker.COUNT.get(), is(1));
    }

    @Test
    public void testWorker_withPeriodDelay_shouldRunAfterEachSetPeriodDelay() {
        PeriodicWorkRequest request = createWorkRequestWithPeriodDelay();
        WorkManager workManager = WorkManager.getInstance();
        workManager.enqueue(request);
        assertThat(CountingTestWorker.COUNT.get(), is(1));
        for (int i = 0; i < 5; ++i) {
            mTestDriver.setPeriodDelayMet(request.getId());
            assertThat(CountingTestWorker.COUNT.get(), is(i + 2));
            WorkStatus requestStatus = workManager.synchronous().getStatusByIdSync(request.getId());
            assertThat(requestStatus.getState().isFinished(), is(false));
        }
    }

    private static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(TestWorker.class).build();
    }

    private static OneTimeWorkRequest createWorkRequestWithNetworkConstraints() {
        return new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build();
    }

    private static OneTimeWorkRequest createWorkRequestWithInitialDelay() {
        return new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialDelay(10L, TimeUnit.DAYS)
                .build();
    }

    private static PeriodicWorkRequest createWorkRequestWithPeriodDelay() {
        return new PeriodicWorkRequest.Builder(CountingTestWorker.class, 10L, TimeUnit.DAYS)
                .build();
    }
}
