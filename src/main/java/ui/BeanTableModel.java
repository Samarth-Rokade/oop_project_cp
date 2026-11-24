package ui;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Generic table model: each column is a name plus a function that reads
 * the cell value from a row object.
 *
 * <pre>
 * new BeanTableModel&lt;UserSummary&gt;(
 *     new Column&lt;&gt;("Name",  UserSummary::fullName),
 *     new Column&lt;&gt;("Email", UserSummary::email));
 * </pre>
 *
 * @param <T> the type of object shown in each row
 */
public class BeanTableModel<T> extends AbstractTableModel {

    /** One column: header text and how to get the value from a row. */
    public record Column<T>(String name, Function<T, ?> value) {
    }

    private final List<Column<T>> columns;
    private List<T> rows = new ArrayList<>();

    @SafeVarargs
    public BeanTableModel(Column<T>... columns) {
        this.columns = List.of(columns);
    }

    public void setRows(List<T> rows) {
        this.rows = new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public List<T> getRows() {
        return List.copyOf(rows);
    }

    public T getRow(int modelRow) {
        return rows.get(modelRow);
    }

    public Optional<Integer> indexOf(Predicate<T> match) {
        for (int i = 0; i < rows.size(); i++) {
            if (match.test(rows.get(i))) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).name();
    }

    @Override
    public Object getValueAt(int row, int column) {
        Object value = columns.get(column).value().apply(rows.get(row));
        return value == null ? "" : value;
    }
}
