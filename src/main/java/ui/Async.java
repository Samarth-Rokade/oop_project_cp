package ui;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs database work on a background thread and hands the result back
 * on the Event Dispatch Thread, so the window never freezes.
 *
 * <pre>
 * Async.run(this, () -> service.getProductsOf(farmer), products -> table.setProducts(products));
 * </pre>
 */
public final class Async {

    private Async() {
    }

    /** Errors are shown with {@link Dialogs#showError}. */
    public static <T> void run(Component parent, Callable<T> task, Consumer<T> onSuccess) {
        run(parent, task, onSuccess, error -> Dialogs.showError(parent, error));
    }

    public static <T> void run(Component parent, Callable<T> task,
                               Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Cursor previous = parent == null ? null : parent.getCursor();
        if (parent != null) {
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();                       // background thread
            }

            @Override
            protected void done() {                       // Event Dispatch Thread
                if (parent != null) {
                    parent.setCursor(previous);
                }
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    onError.accept(e.getCause());
                }
            }
        }.execute();
    }
}
