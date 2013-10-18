package no.kantega.pdf.job;

import com.google.testing.threadtester.*;
import no.kantega.pdf.throwables.ConverterException;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

// This test must not be run by TestNG directly. Instead, it must be called via the thread weaver.
@Test(enabled = false)
public class StubbedFutureRaceConditionTest {

    private static final long TIMEOUT = 1000L;

    private static final String METHOD_NAME_RUN = "run";
    private static final String METHOD_NAME_GET = "get";
    private static final String METHOD_NAME_FETCH_SOURCE = "fetchSource";
    private static final String METHOD_NAME_ON_SOURCE_CONSUMED = "onSourceConsumed";
    private static final String METHOD_NAME_ON_CONVERSION_FINISHED = "onConversionFinished";
    private static final String METHOD_NAME_ON_CONVERSION_FAILED = "onConversionFailed";

    private StubbedFutureWrappingPriorityFuture future;
    private StubPrimaryRunnable mainRunnable;
    private StubSecondaryRunnable secondaryRunnable;

    private Method runMethod;

    private boolean prepared;

    private void assertGetExceptions() throws Exception {
        try {
            future.get();
            fail();
        } catch (ExecutionException e) {
            assertEquals(e.getCause().getClass(), ConverterException.class);
        }
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            fail();
        } catch (ExecutionException e) {
            assertEquals(e.getCause().getClass(), ConverterException.class);
        }
    }

    @ThreadedBefore
    public void setUp() throws Exception {
        prepared = false;
    }

    @ThreadedAfter
    public void tearDown() throws Exception {
        assertTrue(prepared, "The test case was not properly set up");
    }

    public void prepareTest(StubbedFutures behavior) throws Exception {
        future = new StubbedFutureWrappingPriorityFuture(behavior);
        mainRunnable = new StubPrimaryRunnable(future);
        secondaryRunnable = new StubSecondaryRunnable();
        runMethod = AbstractFutureWrappingPriorityFuture.class.getDeclaredMethod(METHOD_NAME_RUN);
        prepared = true;
    }

    @ThreadedTest
    public void testCancelBeforeSourceFetched() throws Exception {
        prepareTest(StubbedFutures.SUCCEED);

        Method breakPointMethod = AbstractFutureWrappingPriorityFuture.class
                .getDeclaredMethod(METHOD_NAME_FETCH_SOURCE);

        ClassInstrumentation instrumentation = Instrumentation
                .getClassInstrumentation(AbstractFutureWrappingPriorityFuture.class);
        CodePosition position = instrumentation.beforeCall(runMethod, breakPointMethod);

        InterleavedRunner.interleave(mainRunnable, secondaryRunnable, Arrays.asList(position)).throwExceptionsIfAny();

        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        assertFalse(future.get());
        assertFalse(future.get(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));

        assertEquals(future.countFetchSource(), 1);
        assertEquals(future.countOnSourceConsumed(), 1);
        assertEquals(future.countStartConversion(), 0);
        assertEquals(future.countOnConversionFinished(), 0);
        assertEquals(future.countOnConversionCancelled(), 1);
        assertEquals(future.countOnConversionFailed(), 0);
        assertEquals(future.countConversionContextAsFuture(), 0);
        assertEquals(future.countCountDownLatch(), 0);
    }

    @ThreadedTest
    public void testCancelAfterSourceFetched() throws Exception {
        prepareTest(StubbedFutures.SUCCEED);

        Method breakPointMethod = AbstractFutureWrappingPriorityFuture.class
                .getDeclaredMethod(METHOD_NAME_FETCH_SOURCE);

        ClassInstrumentation instrumentation = Instrumentation
                .getClassInstrumentation(AbstractFutureWrappingPriorityFuture.class);
        CodePosition position = instrumentation.afterCall(runMethod, breakPointMethod);

        InterleavedRunner.interleave(mainRunnable, secondaryRunnable, Arrays.asList(position)).throwExceptionsIfAny();

        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        assertFalse(future.get());
        assertFalse(future.get(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));

        assertEquals(future.countFetchSource(), 1);
        assertEquals(future.countOnSourceConsumed(), 1);
        assertEquals(future.countStartConversion(), 0);
        assertEquals(future.countOnConversionFinished(), 0);
        assertEquals(future.countOnConversionCancelled(), 1);
        assertEquals(future.countOnConversionFailed(), 0);
        assertEquals(future.countConversionContextAsFuture(), 0);
        assertEquals(future.countCountDownLatch(), 0);
    }

    @ThreadedTest
    public void testCancelBeforeSourceConsumed() throws Exception {
        prepareTest(StubbedFutures.SUCCEED);

        Method breakPointMethod = AbstractFutureWrappingPriorityFuture.class
                .getDeclaredMethod(METHOD_NAME_ON_SOURCE_CONSUMED, Object.class);

        ClassInstrumentation instrumentation = Instrumentation
                .getClassInstrumentation(AbstractFutureWrappingPriorityFuture.class);
        CodePosition position = instrumentation.beforeCall(runMethod, breakPointMethod);

        InterleavedRunner.interleave(mainRunnable, secondaryRunnable, Arrays.asList(position)).throwExceptionsIfAny();

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
        assertTrue(future.get());
        assertTrue(future.get(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));

        assertEquals(future.countFetchSource(), 1);
        assertEquals(future.countOnSourceConsumed(), 1);
        assertEquals(future.countStartConversion(), 1);
        assertEquals(future.countOnConversionFinished(), 1);
        assertEquals(future.countOnConversionCancelled(), 0);
        assertEquals(future.countOnConversionFailed(), 0);
        assertEquals(future.countConversionContextAsFuture(), 1);
        assertEquals(future.countCountDownLatch(), 0);
    }

    @ThreadedTest
    public void testCancelAfterSourceConsumed() throws Exception {
        prepareTest(StubbedFutures.SUCCEED);

        Method breakPointMethod = AbstractFutureWrappingPriorityFuture.class
                .getDeclaredMethod(METHOD_NAME_ON_SOURCE_CONSUMED, Object.class);

        ClassInstrumentation instrumentation = Instrumentation
                .getClassInstrumentation(AbstractFutureWrappingPriorityFuture.class);
        CodePosition position = instrumentation.afterCall(runMethod, breakPointMethod);

        InterleavedRunner.interleave(mainRunnable, secondaryRunnable, Arrays.asList(position)).throwExceptionsIfAny();

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
        assertTrue(future.get());
        assertTrue(future.get(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));

        assertEquals(future.countFetchSource(), 1);
        assertEquals(future.countOnSourceConsumed(), 1);
        assertEquals(future.countStartConversion(), 1);
        assertEquals(future.countOnConversionFinished(), 1);
        assertEquals(future.countOnConversionCancelled(), 0);
        assertEquals(future.countOnConversionFailed(), 0);
        assertEquals(future.countConversionContextAsFuture(), 1);
        assertEquals(future.countCountDownLatch(), 0);
    }

    @ThreadedTest
    public void testCancelBeforeOnConversionFinished() throws Exception {
        prepareTest(StubbedFutures.SUCCEED);

        Method breakPointMethod = AbstractFutureWrappingPriorityFuture.class
                .getDeclaredMethod(METHOD_NAME_ON_CONVERSION_FINISHED, IConversionContext.class);

        ClassInstrumentation instrumentation = Instrumentation
                .getClassInstrumentation(AbstractFutureWrappingPriorityFuture.class);
        CodePosition position = instrumentation.beforeCall(runMethod, breakPointMethod);

        InterleavedRunner.interleave(mainRunnable, secondaryRunnable, Arrays.asList(position)).throwExceptionsIfAny();

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
        assertTrue(future.get());
        assertTrue(future.get(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));

        assertEquals(future.countFetchSource(), 1);
        assertEquals(future.countOnSourceConsumed(), 1);
        assertEquals(future.countStartConversion(), 1);
        assertEquals(future.countOnConversionFinished(), 1);
        assertEquals(future.countOnConversionCancelled(), 0);
        assertEquals(future.countOnConversionFailed(), 0);
        assertEquals(future.countConversionContextAsFuture(), 1);
        assertEquals(future.countCountDownLatch(), 0);
    }

    @ThreadedTest
    public void testCancelAfterOnConversionFinished() throws Exception {
        prepareTest(StubbedFutures.SUCCEED);

        Method breakPointMethod = AbstractFutureWrappingPriorityFuture.class
                .getDeclaredMethod(METHOD_NAME_ON_CONVERSION_FINISHED, IConversionContext.class);

        ClassInstrumentation instrumentation = Instrumentation
                .getClassInstrumentation(AbstractFutureWrappingPriorityFuture.class);
        CodePosition position = instrumentation.afterCall(runMethod, breakPointMethod);

        InterleavedRunner.interleave(mainRunnable, secondaryRunnable, Arrays.asList(position)).throwExceptionsIfAny();

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
        assertTrue(future.get());
        assertTrue(future.get(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));

        assertEquals(future.countFetchSource(), 1);
        assertEquals(future.countOnSourceConsumed(), 1);
        assertEquals(future.countStartConversion(), 1);
        assertEquals(future.countOnConversionFinished(), 1);
        assertEquals(future.countOnConversionCancelled(), 0);
        assertEquals(future.countOnConversionFailed(), 0);
        assertEquals(future.countConversionContextAsFuture(), 1);
        assertEquals(future.countCountDownLatch(), 0);
    }

    @ThreadedTest
    public void testCancelBeforeOnConversionFailed() throws Exception {
        prepareTest(StubbedFutures.FAIL);

        Method breakPointMethod = AbstractFutureWrappingPriorityFuture.class
                .getDeclaredMethod(METHOD_NAME_ON_CONVERSION_FAILED, RuntimeException.class);

        ClassInstrumentation instrumentation = Instrumentation
                .getClassInstrumentation(AbstractFutureWrappingPriorityFuture.class);
        CodePosition position = instrumentation.beforeCall(runMethod, breakPointMethod);

        InterleavedRunner.interleave(mainRunnable, secondaryRunnable, Arrays.asList(position)).throwExceptionsIfAny();

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
        assertGetExceptions();
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));

        assertEquals(future.countFetchSource(), 1);
        assertEquals(future.countOnSourceConsumed(), 1);
        assertEquals(future.countStartConversion(), 1);
        assertEquals(future.countOnConversionFinished(), 0);
        assertEquals(future.countOnConversionCancelled(), 0);
        assertEquals(future.countOnConversionFailed(), 1);
        assertEquals(future.countConversionContextAsFuture(), 1);
        assertEquals(future.countCountDownLatch(), 0);
    }

    @ThreadedTest
    public void testCancelAfterOnConversionFailed() throws Exception {
        prepareTest(StubbedFutures.FAIL);

        Method breakPointMethod = AbstractFutureWrappingPriorityFuture.class
                .getDeclaredMethod(METHOD_NAME_ON_CONVERSION_FAILED, RuntimeException.class);

        ClassInstrumentation instrumentation = Instrumentation
                .getClassInstrumentation(AbstractFutureWrappingPriorityFuture.class);
        CodePosition position = instrumentation.afterCall(runMethod, breakPointMethod);

        InterleavedRunner.interleave(mainRunnable, secondaryRunnable, Arrays.asList(position)).throwExceptionsIfAny();

        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
        assertGetExceptions();
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));

        assertEquals(future.countFetchSource(), 1);
        assertEquals(future.countOnSourceConsumed(), 1);
        assertEquals(future.countStartConversion(), 1);
        assertEquals(future.countOnConversionFinished(), 0);
        assertEquals(future.countOnConversionCancelled(), 0);
        assertEquals(future.countOnConversionFailed(), 1);
        assertEquals(future.countConversionContextAsFuture(), 1);
        assertEquals(future.countCountDownLatch(), 0);
    }

    @ThreadedTest
    public void testCancelWhileBlockingForConversion() throws Exception {
        prepareTest(StubbedFutures.BLOCK_AND_FAIL_ON_CANCEL);

        Method breakPointMethod = Future.class.getDeclaredMethod(METHOD_NAME_GET);

        ClassInstrumentation instrumentation = Instrumentation
                .getClassInstrumentation(AbstractFutureWrappingPriorityFuture.class);
        CodePosition position = instrumentation.beforeCall(runMethod, breakPointMethod);

        InterleavedRunner.interleave(mainRunnable, secondaryRunnable, Arrays.asList(position)).throwExceptionsIfAny();

        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
        assertFalse(future.get());
        assertFalse(future.get(TIMEOUT, TimeUnit.MILLISECONDS));
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));

        assertEquals(future.countFetchSource(), 1);
        assertEquals(future.countOnSourceConsumed(), 1);
        assertEquals(future.countStartConversion(), 1);
        assertEquals(future.countOnConversionFinished(), 0);
        assertEquals(future.countOnConversionCancelled(), 1);
        assertEquals(future.countOnConversionFailed(), 0);
        assertEquals(future.countConversionContextAsFuture(), 1);
        assertEquals(future.countCountDownLatch(), 0);
    }
}
