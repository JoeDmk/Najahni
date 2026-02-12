package com.najahni.utils;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

/**
 * Utility class for creating wrapped text TableCell factories.
 * Prevents text truncation in TableView columns.
 */
public class WrappedTextCellFactory<S> implements Callback<TableColumn<S, String>, TableCell<S, String>> {

    private final TextAlignment alignment;
    private final boolean centerVertically;

    /**
     * Creates a WrappedTextCellFactory with default left alignment.
     */
    public WrappedTextCellFactory() {
        this(TextAlignment.LEFT, true);
    }

    /**
     * Creates a WrappedTextCellFactory with specified alignment.
     * @param alignment The text alignment
     * @param centerVertically Whether to center text vertically
     */
    public WrappedTextCellFactory(TextAlignment alignment, boolean centerVertically) {
        this.alignment = alignment;
        this.centerVertically = centerVertically;
    }

    @Override
    public TableCell<S, String> call(TableColumn<S, String> param) {
        return new TableCell<>() {
            private final Text text = new Text();

            {
                text.wrappingWidthProperty().bind(param.widthProperty().subtract(15));
                text.setTextAlignment(alignment);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(text);
                
                if (centerVertically) {
                    setAlignment(Pos.CENTER_LEFT);
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    text.setText(null);
                    setGraphic(null);
                } else {
                    text.setText(item);
                    setGraphic(text);
                    
                    // Apply style from parent row
                    if (getTableRow() != null && getTableRow().isSelected()) {
                        text.setStyle("-fx-fill: #2c3e50;");
                    } else {
                        text.setStyle("-fx-fill: #2c3e50;");
                    }
                }
            }
        };
    }

    /**
     * Creates a centered text cell factory.
     * @param <S> The type of the TableView items
     * @return A new WrappedTextCellFactory with center alignment
     */
    public static <S> WrappedTextCellFactory<S> centered() {
        return new WrappedTextCellFactory<>(TextAlignment.CENTER, true);
    }

    /**
     * Creates a left-aligned text cell factory.
     * @param <S> The type of the TableView items
     * @return A new WrappedTextCellFactory with left alignment
     */
    public static <S> WrappedTextCellFactory<S> leftAligned() {
        return new WrappedTextCellFactory<>(TextAlignment.LEFT, true);
    }

    /**
     * Creates a right-aligned text cell factory.
     * @param <S> The type of the TableView items
     * @return A new WrappedTextCellFactory with right alignment
     */
    public static <S> WrappedTextCellFactory<S> rightAligned() {
        return new WrappedTextCellFactory<>(TextAlignment.RIGHT, true);
    }
}
