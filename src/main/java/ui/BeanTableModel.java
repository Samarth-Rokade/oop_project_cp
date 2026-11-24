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
 *     new Column&lt;&gt;("Sales", FarmerSales::salesValue, BigDecimal.class));
 * </pre>
 *
 * Give a column a type (BigDecimal, Integer, LocalDate...) when it should
 * sort numerically/chronologically; a renderer then formats it.
 *
 * @param <T> the type of object shown in each row
 */
public class BeanTableModel<T> extends AbstractTableModel {

    /** One column: header text, how to get the value from a row, and its type. */
    public record Column<T>(String name, Function<T, ?> value, Class<?> type) {

        public Column(String name, Function<T, ?> value) {
            this(name, value, Object.class);
        }
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

    /** Column headers, for CSV export. */
    public List<String> headers() {
        return columns.stream().map(Column::name).toList();
    }

    /** Raw cell values of every row, for CSV export. */
    public List<List<Object>> rawRows() {
        List<List<Object>> out = new ArrayList<>();
        for (T row : rows) {
            List<Object> values = new ArrayList<>();
            for (Column<T> c : columns) {
                values.add(c.value().apply(row));
            }
            out.add(values);
        }
        return out;
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
    public Class<?> getColumnClass(int column) {
        return columns.get(column).type();
    }

    @Override
    public Object getValueAt(int row, int column) {
        Column<T> c = columns.get(column);
        Object value = c.value().apply(rows.get(row));
        // typed columns keep null so sorting never compares a String with a number
        return value == null && c.type() == Object.class ? "" : value;
    }
}
