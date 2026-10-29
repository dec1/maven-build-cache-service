package de.example.mavenbuildcacheproxy.testsupport;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * JUnit watcher registered with @ExtendWith to print test outcomes and display names to stdout.
 * Failure and abort lines include a cause; skipped tests include a reason when supplied.
 */
public class TestResultReporter implements TestWatcher {

    @Override
    public void testSuccessful(ExtensionContext context) {
        System.out.printf("[TEST] PASS  %s%n", context.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        System.out.printf("[TEST] FAIL  %s - %s%n",
                context.getDisplayName(),
                Optional.ofNullable(cause.getMessage()).orElse(cause.getClass().getSimpleName()));
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        System.out.printf("[TEST] ABORT %s - %s%n",
                context.getDisplayName(),
                Optional.ofNullable(cause.getMessage()).orElse(cause.getClass().getSimpleName()));
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        System.out.printf("[TEST] SKIP  %s%s%n",
                context.getDisplayName(),
                reason.map(value -> " - " + value).orElse(""));
    }
}
