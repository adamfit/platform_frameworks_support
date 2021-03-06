/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.work.impl.background.firebase;

import static androidx.work.NetworkType.CONNECTED;
import static androidx.work.NetworkType.METERED;
import static androidx.work.NetworkType.NOT_ROAMING;
import static androidx.work.NetworkType.UNMETERED;
import static androidx.work.impl.background.firebase.FirebaseJobConverter
        .FIREBASE_MIN_BACKOFF_DURATION;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;

import android.content.Context;
import android.net.Uri;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.PackageManagerHelper;
import androidx.work.worker.FirebaseTestWorker;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobTrigger;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.ObservedUri;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(maxSdkVersion = WorkManagerImpl.MAX_PRE_JOB_SCHEDULER_API_LEVEL)
public class FirebaseJobConverterTest {
    private FirebaseJobDispatcher mDispatcher;
    private FirebaseJobConverter mConverter;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext().getApplicationContext();
        PackageManagerHelper.setComponentEnabled(context, FirebaseJobService.class, true);
        mDispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
        mConverter = new FirebaseJobConverter(mDispatcher);
    }

    @Test
    @SmallTest
    public void testConvert_basicWorkSpec() {
        final String expectedWorkSpecId = "026e3422-9cd1-11e7-abc4-cec278b6b50a";
        WorkSpec workSpec = new WorkSpec(expectedWorkSpecId, FirebaseTestWorker.class.getName());
        Job job = mConverter.convert(workSpec);
        assertThat(job.getTag(), is(expectedWorkSpecId));
        assertThat(job.getLifetime(), is(Lifetime.FOREVER));
        assertThat(job.getService(), is(FirebaseJobService.class.getName()));

        JobTrigger.ImmediateTrigger trigger = (JobTrigger.ImmediateTrigger) job.getTrigger();
        assertThat(trigger, is(Trigger.NOW));
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testConvert_backoffPolicy() {
        long givenBackoffDelayDuration = 50000L;
        WorkSpec workSpec = new WorkSpec("id", FirebaseTestWorker.class.getName());
        workSpec.setBackoffDelayDuration(givenBackoffDelayDuration);
        workSpec.backoffPolicy = BackoffPolicy.LINEAR;
        Job job = mConverter.convert(workSpec);

        int expectedBackoffDelayDuration =
                (int) TimeUnit.MILLISECONDS.toSeconds(givenBackoffDelayDuration);
        assertThat(job.getRetryStrategy().getInitialBackoff(), is(expectedBackoffDelayDuration));
        assertThat(job.getRetryStrategy().getPolicy(), is(RetryStrategy.RETRY_POLICY_LINEAR));
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testConvert_backoffPolicyBelowMinimumSize() {
        long givenBackoffDelayDuration = 1L;
        WorkSpec workSpec = new WorkSpec("id", FirebaseTestWorker.class.getName());
        workSpec.setBackoffDelayDuration(givenBackoffDelayDuration);
        workSpec.backoffPolicy = BackoffPolicy.LINEAR;
        Job job = mConverter.convert(workSpec);

        int expectedBackoffDelayDuration =
                (int) TimeUnit.MILLISECONDS.toSeconds(FIREBASE_MIN_BACKOFF_DURATION);
        assertThat(job.getRetryStrategy().getInitialBackoff(), is(expectedBackoffDelayDuration));
        assertThat(job.getRetryStrategy().getPolicy(), is(RetryStrategy.RETRY_POLICY_LINEAR));
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
        assertThat(mDispatcher.getValidator().validate(job.getRetryStrategy()), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testConvert_initialDelay() {
        long givenInitialDelayDuration = 50000L;
        WorkSpec workSpec = new WorkSpec("id", FirebaseTestWorker.class.getName());
        workSpec.initialDelay = givenInitialDelayDuration;
        Job job = mConverter.convert(workSpec);

        // Initial delay is handled via an AlarmManager broadcast
        assertThat(job.getTrigger(), is(instanceOf(JobTrigger.ImmediateTrigger.class)));
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testConvert_requireContentUriTrigger() {
        final Uri expectedUri = Uri.parse("TEST_URI");
        final ObservedUri expectedObservedUri =
                new ObservedUri(expectedUri, ObservedUri.Flags.FLAG_NOTIFY_FOR_DESCENDANTS);
        WorkSpec workSpec = new OneTimeWorkRequest.Builder(FirebaseTestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .addContentUriTrigger(expectedUri, true)
                        .build())
                .build()
                .getWorkSpec();
        Job job = mConverter.convert(workSpec);

        JobTrigger.ContentUriTrigger trigger = (JobTrigger.ContentUriTrigger) job.getTrigger();
        List<ObservedUri> observedUriList = trigger.getUris();
        MatcherAssert.assertThat(observedUriList, contains(expectedObservedUri));
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testConvert_requiresCharging() {
        WorkSpec workSpec = new OneTimeWorkRequest.Builder(FirebaseTestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresCharging(true)
                        .build())
                .build()
                .getWorkSpec();
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.DEVICE_CHARGING);
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testConvert_periodic() {
        long testInterval = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;
        long testFlex = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS;

        int expectedWindowEndSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(testInterval);
        int flexSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(testFlex);
        int expectedWindowStartSeconds = expectedWindowEndSeconds - flexSeconds;

        WorkSpec workSpec = new WorkSpec("id", FirebaseTestWorker.class.getName());
        workSpec.setPeriodic(testInterval);
        Job job = mConverter.convert(workSpec);

        JobTrigger.ExecutionWindowTrigger trigger =
                (JobTrigger.ExecutionWindowTrigger) job.getTrigger();
        assertThat(trigger.getWindowEnd(), is(expectedWindowEndSeconds));
        assertThat(trigger.getWindowStart(), is(expectedWindowStartSeconds));
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testConvert_requiresDeviceIdle() {
        WorkSpec workSpec = new OneTimeWorkRequest.Builder(FirebaseTestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresDeviceIdle(true)
                        .build())
                .build()
                .getWorkSpec();
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.DEVICE_IDLE);
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testConvert_requiresNetworkAny() {
        WorkSpec workSpec = new OneTimeWorkRequest.Builder(FirebaseTestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(CONNECTED)
                        .build())
                .build()
                .getWorkSpec();
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.ON_ANY_NETWORK);
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testConvert_requiresNetworkMetered_unsupported() {
        WorkSpec workSpec = new OneTimeWorkRequest.Builder(FirebaseTestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(METERED)
                        .build())
                .build()
                .getWorkSpec();
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.ON_ANY_NETWORK);
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testConvert_requiresNetworkNotRoaming_unsupported() {
        WorkSpec workSpec = new OneTimeWorkRequest.Builder(FirebaseTestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NOT_ROAMING)
                        .build())
                .build()
                .getWorkSpec();
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.ON_ANY_NETWORK);
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testConvert_requiresNetworkUnmetered() {
        WorkSpec workSpec = new OneTimeWorkRequest.Builder(FirebaseTestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(UNMETERED)
                        .build())
                .build()
                .getWorkSpec();
        Job job = mConverter.convert(workSpec);
        assertHasIntInArray(job.getConstraints(), Constraint.ON_UNMETERED_NETWORK);
        assertThat(mDispatcher.getValidator().validate(job), is(nullValue()));
    }

    private void assertHasIntInArray(int[] array, int expectedItem) {
        boolean found = false;
        for (int item : array) {
            if (item == expectedItem) {
                found = true;
                break;
            }
        }
        assertThat(found, is(true));
    }
}
